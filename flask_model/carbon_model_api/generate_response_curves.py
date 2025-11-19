"""
单变量响应曲线分析脚本

功能概述：
• 分析关键生态变量对碳储量预测的单独影响
• 生成单变量响应曲线可视化图表
• 保持其他变量恒定，观察目标变量变化的影响

分析变量：
• h.t (树高): 1-25米
• a.cp (冠幅): 1-15米
• c.d (木材密度): 0.3-1.2
• age (林龄): 1-80年
• map (年降水量): 500-3000毫米
• mat (年均温): 10-30°C

方法说明：
• 采用控制变量法，每次只改变一个特征值
• 基于预训练的CatBoost模型进行预测
• 生成碳储量随单个变量变化的响应曲线

输出结果：
• response_plots/response_{feature}.png - 各变量的响应曲线图
• 图表展示碳储量预测值随变量变化的趋势

应用价值：
• 识别对碳储量最敏感的关键变量
• 为森林管理决策提供科学依据
• 辅助理解模型预测的生态学机制
"""
import joblib
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import os

# 加载模型（已包含预处理器）
model = joblib.load("catboost_pipeline.pkl")

# 定义基础样本（保持其他变量不变）
base_sample = {
    'latitude': 19.0,
    'longitude': 109.5,
    'mat': 25.5,
    'map': 1800,
    'age': 25,
    'h.t': 5.0,
    'd.bh': 10.0,
    'a.cp': 3.5,
    'c.d': 0.7,
    'vegetation': 'mangrove',
    'growingcondition': 'natural',
    'pft': 'broadleaf'
}

# 响应变量及其变化范围
feature_ranges = {
    'h.t': np.linspace(1, 25, 40),        # 树高（1~25m）
    'a.cp': np.linspace(1, 15, 40),       # 冠幅（1~15m）
    'c.d': np.linspace(0.3, 1.2, 40),     # 木材密度（0.3~1.2）
    'age': np.linspace(1, 80, 40),        # 林龄（1~80年）
    'map': np.linspace(500, 3000, 40),    # 年降水量（500~3000mm）
    'mat': np.linspace(10, 30, 40),  # 年均温 (10~30°C)
}

# 输出目录
output_dir = "response_plots"
os.makedirs(output_dir, exist_ok=True)

# 每个变量绘图函数
def plot_response(feature, values):
    samples = pd.DataFrame([base_sample] * len(values))
    samples[feature] = values
    preds = model.predict(samples)

    plt.figure(figsize=(8, 6))
    plt.plot(values, preds, color='teal', linewidth=2)
    plt.title(f"Response Curve for {feature}")
    plt.xlabel(feature)
    plt.ylabel("Predicted Carbon Stock (t/ha)")
    plt.grid(True, linestyle='--', alpha=0.4)
    plt.tight_layout()

    filename = f"{output_dir}/response_{feature}.png"
    plt.savefig(filename)
    plt.close()
    print(f"已保存: {filename}")

# 批量绘图
for feat, val_range in feature_ranges.items():
    plot_response(feat, val_range)
