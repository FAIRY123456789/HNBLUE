"""
CatBoost模型训练与碳储量预测

功能概述：
• 集成多种机器学习模型进行地上生物量预测
• 重点优化CatBoost模型并进行交叉验证评估
• 生成全国森林和类红树林样本的碳储量预测
• 提供模型可解释性分析(SHAP)和可视化结果

数据处理流程：
1. 数据加载与特征工程（数值/分类特征分别处理）
2. 多模型训练比较（聚焦CatBoost最优表现）
3. 交叉验证与模型稳健性分析
4. SHAP特征重要性解析
5. 碳储量预测与结果导出

核心输出：
• GlobalForest_Estimated.csv - 全国森林碳储量预测
• MangroveLike_Estimated.csv - 类红树林碳储量预测
• 模型性能对比与可视化图表
• 训练完成的预测管道模型

技术特色：
• 自动化特征预处理管道
• 交叉验证缓存机制加速实验
• 双碳转换系数（森林0.45，红树林0.48）
• 完整的模型可解释性分析
"""
import os
import joblib
import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split, KFold, RepeatedKFold, cross_val_score
from sklearn.linear_model import LinearRegression
from sklearn.metrics import mean_squared_error, mean_absolute_error, r2_score, make_scorer
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import make_pipeline
from sklearn.impute import SimpleImputer
from sklearn.compose import ColumnTransformer
from sklearn.preprocessing import OneHotEncoder
import warnings
warnings.filterwarnings("ignore")

# 新增模型
from xgboost import XGBRegressor
from catboost import CatBoostRegressor
from lightgbm import LGBMRegressor
from sklearn.ensemble import HistGradientBoostingRegressor
from ngboost import NGBRegressor
from ngboost.distns import Normal
from pytorch_tabnet.tab_model import TabNetRegressor
from sklearn.preprocessing import LabelEncoder
import matplotlib.pyplot as plt
import seaborn as sns
import shap
import matplotlib.ticker as ticker

# print(shap.__version__)


# ========== Step 1: 数据加载 ==========
df = pd.read_csv("BAAD_cleaned.csv")
print(f"已加载数据，共 {df.shape[0]} 行，{df.shape[1]} 列")


# ========== Step 2: 目标变量与特征选择 ==========
TARGET = "m.so"  # 地上生物量作为预测目标
features = [
    'latitude', 'longitude', 'mat', 'map', 'age', 'h.t', 'd.bh', 'a.cp', 'c.d',
    'vegetation', 'growingcondition', 'pft'
]
df = df[df[TARGET].notna()].copy()  # 过滤掉目标变量缺失的样本


# ========== Step 3: 数据预处理管道构建 ==========
# 分离数值型和分类型特征
numeric_features = df[features].select_dtypes(include=[np.number]).columns.tolist()
categorical_features = df[features].select_dtypes(include=["object", "category"]).columns.tolist()

# 数值特征处理：中位数填补 + 标准化
numeric_transformer = make_pipeline(
    SimpleImputer(strategy="median"),
    StandardScaler()
)
# 分类特征处理：常量填补 + 独热编码
categorical_transformer = make_pipeline(
    SimpleImputer(strategy="constant", fill_value="missing"),
    OneHotEncoder(handle_unknown="ignore")
)
# 组合预处理步骤
preprocessor = ColumnTransformer(
    transformers=[
        ("num", numeric_transformer, numeric_features),
        ("cat", categorical_transformer, categorical_features),
    ]
)


# ========== Step 4: 数据集划分 ==========
X = df[features]
y = df[TARGET]
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)


# ========== Step 5: 模型定义与配置 ==========
models = {
    "Linear Regression": LinearRegression(),
    "XGBoost": XGBRegressor(n_estimators=100, learning_rate=0.1, max_depth=5, random_state=42),
    "CatBoost": CatBoostRegressor(   # 经过超参数调优的CatBoost模型配置
        iterations=1500,
        learning_rate=0.08,
        depth=11,
        l2_leaf_reg=2,
        loss_function='RMSE',
        eval_metric='R2',
        verbose=0,
        random_seed=42,
        early_stopping_rounds=50,
    ),
    "LightGBM": LGBMRegressor(n_estimators=100, learning_rate=0.1, random_state=42),
    "HistGradientBoosting": HistGradientBoostingRegressor(random_state=42),
    "NGBoost": NGBRegressor(Dist=Normal, verbose=False, random_state=42)
}
# 原本的（未经过贪心法调参的最初版本）：
# models = {
#     "Linear Regression": LinearRegression(),
#     "XGBoost": XGBRegressor(n_estimators=100, learning_rate=0.1, max_depth=5, random_state=42),
#     "CatBoost": CatBoostRegressor(verbose=0, random_state=42),
#     "LightGBM": LGBMRegressor(n_estimators=100, learning_rate=0.1, random_state=42),
#     "HistGradientBoosting": HistGradientBoostingRegressor(random_state=42),
#     "NGBoost": NGBRegressor(Dist=Normal, verbose=False, random_state=42)
# }


# ========== Step 6: 模型训练与评估 ==========
results = []
for name, model in models.items():
    # 构建完整处理管道
    pipeline = make_pipeline(preprocessor, model)
    pipeline.fit(X_train, y_train)
    y_pred = pipeline.predict(X_test)
    # 计算评估指标
    mse = mean_squared_error(y_test, y_pred)
    rmse = np.sqrt(mse)
    mae = mean_absolute_error(y_test, y_pred)
    r2 = r2_score(y_test, y_pred)

    results.append({"Model": name, "RMSE": rmse, "MAE": mae, "R2": r2})
    print(f"{name} 完成：RMSE={rmse:.3f}, MAE={mae:.3f}, R²={r2:.3f}")

# ========== Step 6.1: 对全国样本进行全量预测并保存 GlobalForest_Estimated.csv ==========
print("\n对全国森林样本进行全量预测并生成碳储量结果...")

# 构建 pipeline 并再次 fit（为了获得训练好的 pipeline）
pipeline_cb = make_pipeline(preprocessor, models["CatBoost"])
pipeline_cb.fit(X, y)

# 预测全国森林的地上生物量
y_global_pred = pipeline_cb.predict(X)

# 构建结果数据集（包含预测值和碳储量转换）
df_global_pred = df.copy()
df_global_pred["m.so_pred"] = y_global_pred
df_global_pred["carbon_stock_pred"] = df_global_pred["m.so_pred"] * 0.45  # 森林碳转换系数使用全国森林 CF=0.45

# 保存预测结果
df_global_pred.to_csv("GlobalForest_Estimated.csv", index=False)
print("已保存预测结果文件：GlobalForest_Estimated.csv")

# # ========== （选择性查看）TabNet 特别训练 ==========
# # 填补 TabNet 分类变量中的 NaN
# for col in categorical_features:
#     df[col] = df[col].fillna("missing")
#     df[col] = df[col].astype(str)
#
# # LabelEncoder 训练
# le_dict = {col: LabelEncoder().fit(df[col]) for col in categorical_features}
# for col, le in le_dict.items():
#     df[col] = le.transform(df[col])
#
# # TabNet 模型训练
# tabnet_X = df[features].values
# tabnet_y = df[TARGET].values.reshape(-1, 1)  # 关键修复：reshape 成 2D
# X_train_tn, X_test_tn, y_train_tn, y_test_tn = train_test_split(tabnet_X, tabnet_y, test_size=0.2, random_state=42)
#
# clf = TabNetRegressor(verbose=0)
# clf.fit(X_train_tn, y_train_tn, eval_set=[(X_test_tn, y_test_tn)], patience=50, max_epochs=200)
# y_pred_tabnet = clf.predict(X_test_tn).squeeze()
#
# mse = mean_squared_error(y_test_tn, y_pred_tabnet)
# rmse = np.sqrt(mse)
# mae = mean_absolute_error(y_test_tn, y_pred_tabnet)
# r2 = r2_score(y_test_tn, y_pred_tabnet)
# results.append({"Model": "TabNet", "RMSE": rmse, "MAE": mae, "R2": r2})
# print(f"TabNet 完成：RMSE={rmse:.3f}, MAE={mae:.3f}, R²={r2:.3f}")
#
# 提前全局定义 cv_pipeline，避免 if/else 中作用域遗漏
cv_pipeline = make_pipeline(preprocessor, models["CatBoost"])


# ========== Step 7: CatBoost交叉验证评估 ==========
print("\n开始对 CatBoost 模型进行 5 折交叉验证...")
cv_cache = "catboost_cv_scores.pkl"
if os.path.exists(cv_cache):
    scores = joblib.load(cv_cache)
    print("已加载缓存的 KFold 交叉验证结果")
else:
    cv_pipeline = make_pipeline(preprocessor, models["CatBoost"])
    kf = KFold(n_splits=5, shuffle=True, random_state=42)
    scores = cross_val_score(cv_pipeline, X, y, scoring=make_scorer(r2_score), cv=kf)
    joblib.dump(scores, cv_cache)
    print("KFold 验证完成并已缓存")
print(f"CatBoost 交叉验证 R² 平均值: {scores.mean():.4f} ± {scores.std():.4f}")


# ========== Step 8: 重复交叉验证稳健性分析 ==========
print("\n开始重复 KFold 交叉验证（RepeatedKFold）...")
rkf_cache = "catboost_rkf_scores.pkl"
if os.path.exists(rkf_cache):
    scores_repeated = joblib.load(rkf_cache)
    print("已加载缓存的 RepeatedKFold 验证结果")
else:
    rkf = RepeatedKFold(n_splits=5, n_repeats=3, random_state=42)
    scores_repeated = cross_val_score(cv_pipeline, X, y, scoring=make_scorer(r2_score), cv=rkf)
    joblib.dump(scores_repeated, rkf_cache)
    print("RepeatedKFold 验证完成并已缓存")
print(f"RepeatedKFold R² 平均值: {scores_repeated.mean():.4f} ± {scores_repeated.std():.4f}")


# ========== Step 9: 交叉验证结果可视化 ==========
fig, axes = plt.subplots(1, 2, figsize=(14, 8), sharey=True)

# 左图：K-Fold R² 分布
sns.boxplot(y=scores, ax=axes[0], color='skyblue')
axes[0].set_title('K-Fold R² Distribution', fontsize=24)  # 子图标题
axes[0].set_ylabel('R² Score', fontsize=22)  # 纵轴标签
axes[0].tick_params(axis='y', labelsize=20)  # 纵轴刻度
axes[0].tick_params(axis='x', labelsize=20)  # 横轴刻度
axes[0].grid(True, linestyle='--', linewidth=1.2, alpha=0.5)  # 增强网格线

# 右图：Repeated K-Fold R² 分布
sns.boxplot(y=scores_repeated, ax=axes[1], color='lightgreen')
axes[1].set_title('Repeated K-Fold R² Distribution', fontsize=24)  # 子图标题
axes[1].tick_params(axis='y', labelsize=20)  # 纵轴刻度
axes[1].tick_params(axis='x', labelsize=20)  # 横轴刻度
axes[1].grid(True, linestyle='--', linewidth=1.2, alpha=0.5)  # 增强网格线

# 总标题
plt.suptitle('Comparison of Cross-Validation Results for CatBoost Model', fontsize=26)
plt.tight_layout(rect=[0, 0, 1, 0.92])
plt.savefig("plots/catboost_cv_results_enhanced.png", dpi=300)
plt.show()


# ========== Step 10: SHAP模型可解释性分析 ==========
print("\n🔎 进行 SHAP 变量重要性分析...")

# 数据预处理（确保分类特征格式正确）
X_train_cb = X_train.copy()
for col in categorical_features:
    X_train_cb[col] = X_train_cb[col].astype(str).fillna("missing")

# 训练最终CatBoost模型（直接处理分类特征）
final_model = models["CatBoost"]
final_model.fit(X_train_cb, y_train, cat_features=categorical_features)

# SHAP分析：解释模型预测逻辑
try:
    explainer = shap.TreeExplainer(final_model, feature_perturbation="tree_path_dependent")
    shap_values = explainer(X_train_cb)
    # 打印 SHAP 值信息
    print(f"SHAP 值类型: {type(shap_values)}")
    print(f"SHAP 值形状: {shap_values.shape}")

    # ========== SHAP特征重要性条形图 ==========
    print("绘制 SHAP 条形图...")
    # 自定义颜色
    custom_color = "#86A1CC"  # 你指定的颜色
    # 创建条形图
    fig, ax = plt.subplots(figsize=(8, 6))
    shap.plots.bar(shap_values, max_display=12, show=False)
    # 获取当前轴对象并修改条形图颜色
    for bar in ax.patches:
        bar.set_color(custom_color)
    # 设置字体大小和其他属性
    ax.set_title("Feature Importance (CatBoost + SHAP)", fontsize=20)  # 增大标题
    ax.set_xlabel("Mean SHAP Value", fontsize=16)  # 增大X轴标签
    ax.set_ylabel("Features", fontsize=16)  # 增大Y轴标签
    ax.tick_params(axis='x', labelsize=14)  # 增大X轴刻度字体
    ax.tick_params(axis='y', labelsize=14)  # 增大Y轴刻度字体
    # 移除手动数值注释，保留默认SHAP的数值显示
    plt.tight_layout()
    plt.savefig("plots/shap_bar_custom.png")
    plt.show()

    # ========== SHAP特征影响分布图 ==========
    print("绘制 SHAP 分布图...")
    plt.figure(figsize=(8, 6))
    shap.summary_plot(
        shap_values,
        features=X_train_cb,
        feature_names=X_train_cb.columns.tolist(),
        show=False
    )
    plt.title("SHAP Summary Plot (CatBoost + SHAP)", fontsize=20)  # 增大标题
    plt.xlabel("SHAP Value", fontsize=16)  # 增大X轴标签
    plt.ylabel("Features", fontsize=16)  # 增大Y轴标签
    plt.xticks(fontsize=14)  # 增大X轴刻度
    plt.yticks(fontsize=14)  # 增大Y轴刻度
    plt.tight_layout()
    plt.savefig("plots/shap_summary.png")
    plt.show()
except Exception as e:
    print(f"SHAP 分析失败: {e}")


# ========== Step 11: 模型性能汇总 ==========
result_df = pd.DataFrame(results).sort_values("RMSE")
print("\n各模型表现对比：")
print(result_df.to_string(index=False))


# ========== Step 12: 类红树林样本预测 ==========
print("\n开始在类红树林样本上估算...")

# 读取类红树林样本
df_mangrove = pd.read_csv("BAAD_MangroveLike.csv")
df_mangrove = df_mangrove[df_mangrove[TARGET].notna()].copy()
print(f"已加载类红树林数据，共 {df_mangrove.shape[0]} 行")

# 数据预处理（与主模型一致）
df_mangrove[categorical_features] = df_mangrove[categorical_features].astype(str).fillna("missing")
X_mangrove = df_mangrove[features]
y_mangrove = df_mangrove[TARGET]

# 预测类红树林样本
y_pred_mangrove = final_model.predict(X_mangrove)

# 评估迁移预测效果
rmse_m = np.sqrt(mean_squared_error(y_mangrove, y_pred_mangrove))
mae_m = mean_absolute_error(y_mangrove, y_pred_mangrove)
r2_m = r2_score(y_mangrove, y_pred_mangrove)
print(f"类红树林样本预测完成：RMSE={rmse_m:.3f}, MAE={mae_m:.3f}, R²={r2_m:.4f}")

# 保存预测结果
df_mangrove["m.so_pred"] = y_pred_mangrove
df_mangrove.to_csv("MangroveLike_Estimated.csv", index=False)
print("📁 已保存预测结果文件：MangroveLike_Estimated.csv")


# ========== Step 13: 预测值与实际值散点图 ==========
plt.figure(figsize=(8, 6))
plt.scatter(y_mangrove, y_pred_mangrove, alpha=0.5, color='teal', edgecolor='k')
# 添加理想拟合线
plt.plot([y_mangrove.min(), y_mangrove.max()],
         [y_mangrove.min(), y_mangrove.max()],
         color='red', linestyle='--', linewidth=2)
plt.title("Estimated vs Actual (MangroveLike Samples)", fontsize=20)
plt.xlabel("Actual m.so", fontsize=16)
plt.ylabel("Estimated m.so", fontsize=16)
plt.xticks(fontsize=14)
plt.yticks(fontsize=14)
plt.grid(True, linestyle='--', alpha=0.5)  # 增强网格线清晰度

# 添加评估指标文本框
metrics_text = f"R² = {r2_m:.4f}\nRMSE = {rmse_m:.3f}\nMAE = {mae_m:.3f}"
plt.text(0.05, 0.95, metrics_text, transform=plt.gca().transAxes,
         fontsize=16, verticalalignment='top',
         bbox=dict(facecolor='white', alpha=0.8, boxstyle='round'))

# 调整坐标轴刻度格式
plt.gca().xaxis.set_major_locator(ticker.MaxNLocator(integer=False))
plt.gca().yaxis.set_major_locator(ticker.MaxNLocator(integer=False))

# 保存图像
plt.tight_layout()
plt.savefig("plots/mangrove_estimation_vs_actual.png")
plt.show()
print("已生成预测 vs 实际散点图：plots/mangrove_estimation_vs_actual.png")


# ========== Step 14: 残差分析图 ==========
residuals = y_mangrove - y_pred_mangrove

plt.figure(figsize=(8, 6))
plt.scatter(y_pred_mangrove, residuals, alpha=0.4, color='orange', edgecolor='k')
plt.axhline(y=0, color='red', linestyle='--', linewidth=2)

# 增大标题、标签和刻度字体大小
plt.title("Residuals vs Estimated Values (MangroveLike Samples)", fontsize=20)
plt.xlabel("Estimated m.so", fontsize=16)
plt.ylabel("Residuals (Actual - Estimated)", fontsize=16)
plt.xticks(fontsize=14)
plt.yticks(fontsize=14)
plt.grid(True, linestyle='--', alpha=0.5)  # 增强网格线清晰度

# 保存图像
plt.tight_layout()
plt.savefig("plots/mangrove_residuals_plot.png")
plt.show()
print("已保存残差图：plots/mangrove_residuals_plot.png")

# ========== Step 15: 在两个 Estimated 文件中补充碳储量列 ==========
print("\n📌 开始补充碳储量信息到预测文件中...")

# 1. 类红树林样本（MangroveLike）：CF = 0.48
mangrove_file = "MangroveLike_Estimated.csv"
df_m = pd.read_csv(mangrove_file)
if "m.so_pred" in df_m.columns:
    df_m["carbon_stock_pred"] = df_m["m.so_pred"] * 0.48
    df_m.to_csv(mangrove_file, index=False)
    print(f"已更新类红树林预测文件：{mangrove_file}")
else:
    print(f"未找到 m.so_pred 列于 {mangrove_file}，请确认是否已执行预测步骤")

# 2. 全国森林样本（GlobalForest）：CF = 0.45
global_file = "GlobalForest_Estimated.csv"
df_g = pd.read_csv(global_file)
if "m.so_pred" in df_g.columns:
    df_g["carbon_stock_pred"] = df_g["m.so_pred"] * 0.45
    df_g.to_csv(global_file, index=False)
    print(f"已更新全国森林预测文件：{global_file}")
else:
    print(f"未找到 m.so_pred 列于 {global_file}，请确认是否已执行预测步骤")


# ========== Step 16: 模型保存 ==========
# 构建完整预测管道并保存
pipeline_cb = make_pipeline(preprocessor, final_model)
pipeline_cb.fit(X, y)  # 全数据拟合

# 保存 pipeline
joblib.dump(pipeline_cb, "carbon_model_api/catboost_pipeline.pkl")
print("模型已保存为 catboost_pipeline.pkl")
