"""
CatBoost模型超参数调优可视化分析 - L2正则化分析

功能概述：
• 分析L2正则化强度对模型泛化能力的影响
• 评估正则化在防止过拟合方面的作用效果
• 确定最优的正则化参数配置

数据说明：
• L2参数范围：1-12，覆盖从弱到强的正则化强度
• 参数间隔非均匀，重点测试关键强度值

分析重点：
• 识别L2正则化的"甜点"区域（1-3）
• 观察强正则化对模型性能的抑制效应
• 综合前两个文件的发现，确定最终参数组合

调优结论：
• 推荐参数：树深度11 + 学习率0.05-0.07 + L2=1-2
• 三个参数协同调优达到最佳泛化效果
"""
import matplotlib.pyplot as plt

# 参数值与评估结果
l2_leaf_values = [1, 2, 3, 5, 8, 12]
rmse_values = [0.65574, 0.649286, 0.654016, 0.664167, 0.670313, 0.677001]
mae_values =  [0.26264, 0.265063, 0.268591, 0.275472, 0.278763, 0.288405]
r2_values =   [0.945633, 0.946698, 0.945919, 0.944227, 0.94319, 0.94205]

# 自定义颜色
custom_blue = "#4682B4"  # RMSE
custom_orange = "orange"  # MAE
custom_green = "green"  # R²

# 设置字体和样式
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
titles = ["RMSE vs L2 Regularization", "MAE vs L2 Regularization", "R² vs L2 Regularization"]
y_labels = ["RMSE", "MAE", "R²"]
colors = [custom_blue, custom_orange, custom_green]
data_series = [rmse_values, mae_values, r2_values]

# 绘图循环
for ax, title, ylabel, color, data in zip(axes, titles, y_labels, colors, data_series):
    ax.plot(l2_leaf_values, data, marker='o', linewidth=2, color=color)
    ax.set_xlabel("l2_leaf_reg")
    ax.set_ylabel(ylabel)
    ax.set_title(title)
    ax.set_xticks(l2_leaf_values)
    ax.grid(True, linestyle='--', alpha=0.6)

    # 去除顶部与右侧边框
    ax.spines['top'].set_visible(False)
    ax.spines['right'].set_visible(False)
    ax.tick_params(direction='out', length=6, width=1)

# 布局优化
plt.tight_layout()
plt.show()
