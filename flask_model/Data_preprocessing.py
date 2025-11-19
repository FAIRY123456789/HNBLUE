"""
BAAD 生态数据分析与预处理脚本

功能概述：
• 读取并清洗BAAD生态数据集（包含植物形态与生物量参数）
• 自动化异常值检测与处理（基于正态性检验选择IQR或3σ方法）
• 分策略缺失值插补（KNN + 中位数）
• 筛选类红树林生态样本（基于气候、植被类型和生长条件）
• 生成数据质量可视化图表（Q-Q图、箱线图）

数据处理流程：
1. 数据读取与列名标准化
2. 异常值检测与剔除（正态性检验驱动）
3. 缺失值插补（低缺失率用KNN，高缺失率用中位数）
4. 生态样本筛选（热带雨林+特定气候条件）
5. 结果保存与统计分析

技术特点：
• 自适应异常值处理方法（normaltest自动选择IQR/3σ）
• 分层次缺失值处理策略
• 生态学驱动的样本筛选逻辑
• 模块化函数设计便于扩展

数据输出：
• BAAD_cleaned.csv - 完整清洗后的数据集
• BAAD_MangroveLike.csv - 类红树林生态样本子集
• response_plots/ - 数据质量可视化图表目录

依赖库：
• pandas, numpy - 数据处理
• scipy, sklearn - 统计分析与机器学习
• matplotlib, seaborn - 数据可视化
"""
import pandas as pd
import numpy as np
from scipy import stats
from sklearn.impute import KNNImputer
import matplotlib.pyplot as plt
import matplotlib
import seaborn as sns
from scipy.stats import probplot
from matplotlib import gridspec
import os

matplotlib.rcParams['font.family'] = 'Arial'  # 避免字体警告


# ========== 1. 数据读取与初步探索 ==========
file_path = 'BAAD_raw.csv'

# 读取BAAD生态数据集，包含植物形态测量和生物量参数
df = pd.read_csv(file_path, encoding='gbk')  # 使用gbk编码读取中文数据
print(f"原始数据读取完成，记录数: {df.shape[0]} 行，字段数: {df.shape[1]} 列")

# 显示所有字段名称，便于数据理解和后续处理
print("\n原始字段名称如下：")
for i, col in enumerate(df.columns):
    print(f"{i+1:02d}. {col}")

# 标准化列名：去除空格并转为小写，便于后续处理
df.columns = df.columns.str.strip().str.lower()


# ========== 2. 异常值检测与处理 ==========
def handle_outliers(dataframe):
    """
    基于正态性检验的智能异常值处理
    - 对每个数值列进行正态性检验(D'Agostino test)
    - p<0.05(非正态): 使用IQR方法(Q1-1.5IQR, Q3+1.5IQR)
    - p≥0.05(正态): 使用3σ方法(均值±3倍标准差)
    - 缺失率>90%的列直接删除
    """
    print("正在进行异常值检测与处理...")
    dropped_cols = []
    for col in dataframe.select_dtypes(include=[np.number]).columns:
        series = dataframe[col]
        # 高缺失率列直接删除
        if series.isnull().mean() > 0.9:
            dataframe.drop(columns=[col], inplace=True)
            dropped_cols.append(col)
            continue
        try:
            # 正态性检验：p值越小越偏离正态分布
            k2, p = stats.normaltest(series.dropna())
            # 非正态分布，使用IQR方法
            if p < 0.05:
                method = "IQR"
                Q1 = series.quantile(0.25)
                Q3 = series.quantile(0.75)
                IQR = Q3 - Q1
                lower = Q1 - 1.5 * IQR
                upper = Q3 + 1.5 * IQR
                outliers = ((series < lower) | (series > upper)).sum()
                # 将异常值替换为NaN，后续进行插补
                dataframe.loc[(series < lower) | (series > upper), col] = np.nan
            # 正态分布，使用3σ方法
            else:
                method = "3σ"
                mean = series.mean()
                std = series.std()
                outliers = ((series < mean - 3 * std) | (series > mean + 3 * std)).sum()
                dataframe.loc[(series < mean - 3 * std) | (series > mean + 3 * std), col] = np.nan
            print(f" {col}: 使用 {method} 方法剔除异常值 {outliers} 个")
        except Exception as e:
            print(f" {col} 异常处理失败: {e}")
    return dataframe

df = handle_outliers(df)

# # ========== （不强制要求）可视化：Q-Q 图 & 箱线图 ==========
# print("正在绘制 Q-Q 图与箱线图...")
# os.makedirs("plots", exist_ok=True)
# qq_cols = ['h.t', 'd.bh', 'm.so', 'm.rt', 'a.cp', 'm.lf']
# # Q-Q 图
# fig = plt.figure(figsize=(18, 10))
# gs = gridspec.GridSpec(2, 6)
# for i, col in enumerate(qq_cols):
#     if col in df.columns:
#         ax = plt.subplot(gs[i // 3, (i % 3) * 2:(i % 3) * 2 + 2])
#         probplot(df[col].dropna(), dist="norm", plot=ax)
#         ax.set_title(f'Q-Q: {col}')
# plt.tight_layout()
# plt.savefig("response_plots/qqplots.png")
# plt.close()
# # 箱线图
# fig = plt.figure(figsize=(18, 10))
# gs = gridspec.GridSpec(2, 6)
# for i, col in enumerate(qq_cols):
#     if col in df.columns:
#         ax = plt.subplot(gs[i // 3, (i % 3) * 2:(i % 3) * 2 + 2])
#         sns.boxplot(y=df[col], ax=ax, color='skyblue')
#         ax.set_title(f'Boxplot: {col}')
# plt.tight_layout()
# plt.savefig("response_plots/boxplots.png")
# plt.close()
# print("图像保存至 response_plots/ 文件夹")


# ========== 3. 缺失值分层插补策略 ==========
print("开始缺失值插补（分变量策略）...")

# 统计数值列的缺失率
numeric_cols = df.select_dtypes(include=[np.number]).columns
missing_ratio = df[numeric_cols].isnull().mean()

# 根据缺失率分配插补策略：
# - 低缺失率(0-60%): 使用KNN保持变量间关系
# - 高缺失率(60-90%): 使用中位数对偏态分布稳健
knn_vars = [col for col in numeric_cols if 0 < missing_ratio[col] <= 0.6]
median_vars = [col for col in numeric_cols if 0.6 < missing_ratio[col] < 0.9]
print(f"使用 KNN 插补变量数：{len(knn_vars)}")
print(f"使用中位数插补变量数：{len(median_vars)}")

# KNN插补：利用变量间相关性进行智能填充
if knn_vars:
    knn_imputer = KNNImputer(n_neighbors=3)
    df[knn_vars] = knn_imputer.fit_transform(df[knn_vars])
    print("已完成 KNN 插补")

# 中位数插补：对高缺失率变量更稳健
for var in median_vars:
    df[var] = df[var].fillna(df[var].median())
print("已完成中位数插补")


# ========== 4. 类红树林生态样本筛选 ==========
print("开始筛选类红树林样本...")
"""
类红树林样本筛选标准（基于生态相似性）：
- 植被类型：热带雨林(TropRF)和热带季雨林(TropSF)
- 生长条件：野外(FW)、自然恢复(FE)、公共用地(PU)、管理用地(PM)  
- 气候条件：年均降水≥1000mm，年均气温≥18°C
生态学意义：筛选出在气候条件和植被特征上与红树林相似的样本，
为红树林生态研究提供可比较的参照数据集。
"""
mangrove_like = df[
    (df['vegetation'].isin(['TropRF', 'TropSF'])) &
    (df['growingcondition'].isin(['FW', 'FE', 'PU', 'PM'])) &
    (df['map'] >= 1000) &
    (df['mat'] >= 18)
].copy()
print(f"类红树林样本筛选完成，样本数: {mangrove_like.shape[0]} 行")


# ========== 5. 结果统计与保存 ==========
df.to_csv('BAAD_cleaned.csv', index=False)
mangrove_like.to_csv('BAAD_MangroveLike.csv', index=False)
print("\n 数据预处理完成，保存文件：")
print(" - BAAD_cleaned.csv（全国）")
print(" - BAAD_MangroveLike.csv（类红树林）")


# ========== 6. 重新读取 BAAD 数据（CSV 格式） ==========
file_path = 'BAAD_cleaned.csv'
df = pd.read_csv(file_path, encoding='gbk')  # 如有乱码尝试 'gbk'
print(f"Cleaned数据读取完成，记录数: {df.shape[0]} 行，字段数: {df.shape[1]} 列")
print("\n原始字段名称如下：")
for i, col in enumerate(df.columns):
    print(f"{i+1:02d}. {col}")


# ========== 7. 补充统计：计算类红树林样本占比（A/B 格式） ==========
total_samples = df.shape[0]
mangrove_samples = mangrove_like.shape[0]
mangrove_percentage = (mangrove_samples / total_samples) * 100

print(f"类红树林样本占比为：{mangrove_samples} / {total_samples} （{mangrove_percentage:.2f}%）")

