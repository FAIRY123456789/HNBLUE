"""
CatBoost模型超参数调优可视化分析 - 树深度分析

功能概述：
• 分析树深度参数对模型性能的影响规律
• 可视化展示RMSE、MAE、R²三个指标随树深度的变化趋势
• 自动标注各指标的最优值点，辅助参数选择

数据说明：
• 树深度范围：6-13，覆盖从浅层到深层树的完整变化区间
• 性能指标：RMSE(误差)、MAE(误差)、R²(拟合优度)

分析重点：
• 识别树深度的过拟合临界点（约11层后性能下降）
• 为后续学习率和正则化分析提供基准参考

"""
import matplotlib.pyplot as plt

# 参数与评估结果
depth_values = [6, 7, 8, 9, 10, 11, 12, 13]
rmse_values = [0.696084, 0.669855, 0.661468, 0.665714, 0.65827, 0.649286, 0.666755, 0.662084]
mae_values = [0.317153, 0.298721, 0.285388, 0.281087, 0.271833, 0.265063, 0.266962, 0.262718]
r2_values = [0.938737, 0.943267, 0.944679, 0.943966, 0.945213, 0.946698, 0.943791, 0.944576]

# 自定义颜色
custom_blue = "#4682B4"
custom_orange = "orange"
custom_green = "green"
highlight_color = "#666666"  # 柔和醒目的灰色

# 设置字体
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
titles = ["RMSE vs Tree Depth", "MAE vs Tree Depth", "R² vs Tree Depth"]
y_labels = ["RMSE", "MAE", "R²"]
colors = [custom_blue, custom_orange, custom_green]
data_series = [rmse_values, mae_values, r2_values]
optimal_funcs = [min, min, max]  # RMSE/MAE: 越小越好；R²: 越大越好

# 绘制每个子图
for ax, title, ylabel, color, data, opt_func in zip(axes, titles, y_labels, colors, data_series, optimal_funcs):
    ax.plot(depth_values, data, marker='o', linewidth=2, color=color)
    ax.set_xlabel("Tree Depth")
    ax.set_ylabel(ylabel)
    ax.set_title(title)
    ax.set_xticks(range(min(depth_values), max(depth_values) + 1, 1))
    ax.grid(True, linestyle='--', alpha=0.6)

    # 找最优点并标注
    optimal_value = opt_func(data)
    optimal_index = data.index(optimal_value)
    optimal_depth = depth_values[optimal_index]

    ax.plot(optimal_depth, optimal_value, marker='o', color=highlight_color, markersize=8)
    ax.annotate(f"{optimal_value:.4f}",
                xy=(optimal_depth, optimal_value),
                xytext=(optimal_depth, optimal_value + (0.005 if ylabel != "R²" else 0.0007)),
                ha='center', fontsize=10, color=highlight_color)

    # 美化边框
    ax.spines['top'].set_visible(False)
    ax.spines['right'].set_visible(False)
    ax.tick_params(direction='out', length=6, width=1)

plt.tight_layout()
plt.show()
