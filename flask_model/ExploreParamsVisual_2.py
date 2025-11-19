"""
CatBoost模型超参数调优可视化分析 - 学习率分析

功能概述：
• 分析学习率参数对模型收敛速度和精度的影响
• 展示不同学习率下模型性能指标的变化规律
• 识别学习率的有效调优范围

数据说明：
• 学习率范围：0.01-0.1，覆盖从保守到激进的学习步长
• 横轴刻度优化：仅显示偶数学习率，避免标签重叠

分析重点：
• 确定学习率的最优区间（0.04-0.07）
• 观察学习率与训练稳定性的关系
• 为树深度参数提供补充调优维度

"""
import matplotlib.pyplot as plt
import numpy as np

# 参数与评估指标数据
learning_rates = [0.01, 0.02, 0.03, 0.04, 0.05, 0.06, 0.07, 0.08, 0.09, 0.1]
rmse_values = [0.738808, 0.693955, 0.673803, 0.662976, 0.658598, 0.659229, 0.658135, 0.658270, 0.663152, 0.658358]
mae_values =  [0.353333, 0.315131, 0.294265, 0.284082, 0.278139, 0.275264, 0.272655, 0.271833, 0.272209, 0.267728]
r2_values =   [0.930986, 0.939111, 0.942597, 0.944426, 0.945158, 0.945053, 0.945235, 0.945213, 0.944397, 0.945198]

# 自定义颜色
custom_blue = "#4682B4"  # RMSE
custom_orange = "orange"  # MAE
custom_green = "green"  # R²

# 设置字体与样式
plt.rcParams.update({
    'font.size': 12,
    'axes.titlesize': 14,
    'axes.labelsize': 12,
    'xtick.labelsize': 11,
    'ytick.labelsize': 11,
})

# 创建子图
fig, axes = plt.subplots(1, 3, figsize=(15, 4.5))

# 图例内容
titles = ["RMSE vs Learning Rate", "MAE vs Learning Rate", "R² vs Learning Rate"]
y_labels = ["RMSE", "MAE", "R²"]
colors = [custom_blue, custom_orange, custom_green]
data_series = [rmse_values, mae_values, r2_values]

# 横轴刻度，仅显示偶数 learning_rate，步长为 0.02
xticks_filtered = [x for x in learning_rates if round(x * 100) % 2 == 0]

# 绘图循环
for ax, title, ylabel, color, data in zip(axes, titles, y_labels, colors, data_series):
    ax.plot(learning_rates, data, marker='o', linewidth=2, color=color)
    ax.set_xlabel("Learning Rate")
    ax.set_ylabel(ylabel)
    ax.set_title(title)
    ax.set_xticks(xticks_filtered)
    ax.grid(True, linestyle='--', alpha=0.6)

    # 美化边框
    ax.spines['top'].set_visible(False)
    ax.spines['right'].set_visible(False)
    ax.tick_params(direction='out', length=6, width=1)

# 布局优化
plt.tight_layout()
plt.show()
