"""
碳储量预测服务核心模块

功能概述：
• 提供单样本和批量样本的碳储量预测功能
• 执行参数敏感性分析（±20%扰动测试）
• 生成敏感性分析可视化图表
• 提供敏感性结果的文本解释

核心服务：
• predict_carbon_stock - 单样本碳储量预测
• batch_predict_carbon_stock - 批量样本预测
• sensitivity_analysis - 参数敏感性分析
• save_sensitivity_plot - 敏感性分析图表生成
• explain_sensitivity - 敏感性结果生态学解释

数据处理：
• 前端参数映射到模型特征字段
• 自动处理缺失值和默认值填充
• 预测结果精度控制（保留3位小数）

技术特性：
• 基于预训练的CatBoost模型管道
• 支持无头模式运行（Agg后端）
• 唯一文件名生成避免冲突
• 完整的错误处理机制

输出格式：
• 标准化JSON响应数据
• 可视化图表静态资源URL
• 生态学解释文本报告
"""
import joblib
import pandas as pd
import matplotlib.pyplot as plt
import matplotlib
matplotlib.use('Agg')  # 使用非图形化的后端
import os
import uuid

# 加载训练好的 pipeline（预处理器 + 模型）
model = joblib.load("catboost_pipeline.pkl")

# 单样本预测
def predict_carbon_stock(input_dict):
    try:
        sample = {
            'latitude': input_dict.get('latitude', 19.0),
            'longitude': input_dict.get('longitude', 109.5),
            'mat': input_dict.get('mat', 25.5),
            'map': input_dict.get('map', 1800),
            'age': input_dict.get('age', 25),
            'h.t': float(input_dict['treeHeight']),
            'd.bh': float(input_dict['dbh']),
            'a.cp': float(input_dict['canopy']),
            'c.d': input_dict.get('c.d', 0.7),
            'vegetation': input_dict.get('vegetation', 'mangrove'),
            'growingcondition': input_dict.get('growingcondition', 'natural'),
            'pft': input_dict.get('pft', 'broadleaf')
        }
        X = pd.DataFrame([sample])
        y_pred = model.predict(X)[0]  # .pkl文件直接保留模型的predict推理能力
        return round(float(y_pred), 3)
    except Exception as e:
        raise ValueError(f"预测失败：{e}")
    finally:
        print("收到结构参数：", input_dict)

# 批量样地预测
def batch_predict_carbon_stock(sample_list):
    try:
        records = []
        for i, item in enumerate(sample_list):
            records.append({
                'latitude': item.get('latitude', 19.0),
                'longitude': item.get('longitude', 109.5),
                'mat': item.get('mat', 25.5),
                'map': item.get('map', 1800),
                'age': item.get('age', 25),
                'h.t': float(item['treeHeight']),
                'd.bh': float(item['dbh']),
                'a.cp': float(item['canopy']),
                'c.d': item.get('c.d', 0.7),
                'vegetation': item.get('vegetation', 'mangrove'),
                'growingcondition': item.get('growingcondition', 'natural'),
                'pft': item.get('pft', 'broadleaf')
            })
        df = pd.DataFrame(records)
        y_preds = model.predict(df)
        return [round(float(v), 3) for v in y_preds]
    except Exception as e:
        raise ValueError(f"批量预测失败：{e}")

def sensitivity_analysis(base_input):
    '''
    作用：执行 ±20% 参数扰动的敏感性分析。
    输入：一个包含参数（如树高、胸径、冠幅）的字典 base_input
    对每个参数（treeHeight、dbh、canopy）分别降低 20%、提高 20%，调用 predict_carbon_stock() 预测碳储量
    记录上下限预测值、波动范围，汇总返回结果
    '''
    keys = ['treeHeight', 'dbh', 'canopy']
    original = predict_carbon_stock(base_input)

    results = []
    for key in keys:
        '''尝试从字典 base_input 中获取 key 对应的值，
        如果该 key 不存在，则使用默认值 1.0'''
        varied_input = base_input.copy()
        base_value = float(base_input.get(key, 1.0))

        # -20%
        varied_input[key] = base_value * 0.8
        lower = predict_carbon_stock(varied_input)

        # +20%
        varied_input[key] = base_value * 1.2
        upper = predict_carbon_stock(varied_input)

        results.append({
            "param": key,
            "base": base_value,
            "lower": round(lower, 3),
            "upper": round(upper, 3),
            "range": round(upper - lower, 3),
        })

    return {
        "original": original,
        "sensitivity": results
    }

def save_sensitivity_plot(sens_result, output_dir='sensitivity_plots'):
    import matplotlib.pyplot as plt
    import os, uuid

    if not os.path.exists(output_dir):
        os.makedirs(output_dir)

    params = [r['param'] for r in sens_result['sensitivity']]
    lowers = [r['lower'] for r in sens_result['sensitivity']]
    uppers = [r['upper'] for r in sens_result['sensitivity']]
    base_preds = [(lo + up) / 2 for lo, up in zip(lowers, uppers)]

    # 修复：确保误差为正值
    error_low = [max(base - lo, 0) for base, lo in zip(base_preds, lowers)]
    error_high = [max(up - base, 0) for base, up in zip(base_preds, uppers)]

    # 可视化：误差棒 + 红线标注，红色虚线表示原始预测值，用于直观比较
    fig, ax = plt.subplots(figsize=(8, 5))
    ax.errorbar(
        params, base_preds, yerr=[error_low, error_high],
        fmt='o', ecolor='orange', capsize=8,
        color='teal', elinewidth=2, marker='s', markersize=8
    )

    ax.axhline(
        sens_result["original"], color='red', linestyle='--',
        label=f'Original prediction: {sens_result["original"]:.2f} t/ha'
    )

    ax.set_title("Prediction Sensitivity to Parameter Perturbations", fontsize=14)
    ax.set_ylabel("Predicted Carbon Stock (t/ha)", fontsize=12)
    ax.set_xlabel("Perturbed Parameters", fontsize=12)
    ax.set_ylim(min(lowers) - 1, max(uppers) + 1)
    ax.grid(True, linestyle='--', alpha=0.5)
    ax.legend()

    plt.tight_layout()
    filename = f"sensitivity_{uuid.uuid4().hex[:8]}.png"
    path = os.path.join(output_dir, filename)
    plt.savefig(path, dpi=150)
    plt.close()

    return f"http://localhost:8880/sensitivity_plots/{filename}"

# 结构文本解释函数
def explain_sensitivity(sens_result):
    lines = [f"样地碳储预测为 **{sens_result['original']} t/ha**。"]
    for item in sens_result["sensitivity"]:
        lines.append(
            f"- 当 `{item['param']}` 变化 ±20% 时，预测区间为 **{item['lower']}–{item['upper']} t/ha**，"
            f"波动幅度为 **{item['range']} t/ha**。"
        )
    lines.append("下面开始分析这些变量对碳储预测的敏感性及其生态学含义。")
    return "\n".join(lines)

