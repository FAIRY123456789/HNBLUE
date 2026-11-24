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
import hashlib
import joblib
import math
import pandas as pd
import matplotlib.pyplot as plt
import matplotlib
matplotlib.use('Agg')  # 使用非图形化的后端
import os
import uuid
from pathlib import Path

# Public releases do not ship a trained artifact. A private deployment can
# mount an authorized model through HNBLUE_MODEL_PATH. Without one, the API
# uses a deterministic synthetic fixture so UI and integration tests remain
# runnable without presenting fabricated output as a scientific prediction.
MODEL_PATH = Path(
    os.getenv("HNBLUE_MODEL_PATH", str(Path(__file__).resolve().parent / "catboost_pipeline.pkl"))
).expanduser().resolve()
EXPECTED_MODEL_SHA256 = os.getenv("HNBLUE_MODEL_SHA256", "").strip().lower()
MODEL_SHA256 = hashlib.sha256(MODEL_PATH.read_bytes()).hexdigest() if MODEL_PATH.exists() else ""
if MODEL_PATH.exists() and not EXPECTED_MODEL_SHA256:
    raise RuntimeError("HNBLUE_MODEL_SHA256 is required before loading a model artifact")
if MODEL_PATH.exists() and MODEL_SHA256 != EXPECTED_MODEL_SHA256:
    raise RuntimeError("CatBoost model SHA-256 mismatch")
model = joblib.load(MODEL_PATH) if MODEL_PATH.exists() else None
MODEL_MODE = "authorized-official-artifact" if model is not None else "synthetic-demo"


def _number(input_dict, key, default=None):
    raw = input_dict.get(key, default)
    if raw is None:
        raise ValueError(f"缺少必填字段：{key}")
    value = float(raw)
    if not math.isfinite(value):
        raise ValueError(f"字段 {key} 必须是有限数值")
    return value


def _sample(input_dict):
    sample = {
        'latitude': _number(input_dict, 'latitude', 19.0),
        'longitude': _number(input_dict, 'longitude', 109.5),
        'mat': _number(input_dict, 'mat', 25.5),
        'map': _number(input_dict, 'map', 1800),
        'age': _number(input_dict, 'age', 25),
        'h.t': _number(input_dict, 'treeHeight'),
        'd.bh': _number(input_dict, 'dbh'),
        'a.cp': _number(input_dict, 'canopy'),
        'c.d': _number(input_dict, 'c.d', 0.7),
        'vegetation': str(input_dict.get('vegetation', 'mangrove')).strip(),
        'growingcondition': str(input_dict.get('growingcondition', 'natural')).strip(),
        'pft': str(input_dict.get('pft', 'broadleaf')).strip(),
    }
    if not -90 <= sample['latitude'] <= 90 or not -180 <= sample['longitude'] <= 180:
        raise ValueError("latitude/longitude 超出地理坐标范围")
    if sample['map'] < 0 or sample['age'] < 0 or sample['c.d'] < 0:
        raise ValueError("map、age 和 c.d 不能为负数")
    if sample['h.t'] <= 0 or sample['d.bh'] <= 0 or sample['a.cp'] <= 0:
        raise ValueError("treeHeight、dbh 和 canopy 必须大于 0")
    if not sample['vegetation'] or not sample['growingcondition'] or not sample['pft']:
        raise ValueError("分类特征不能为空")
    return sample


def _predict_frame(frame: pd.DataFrame):
    if model is not None:
        return model.predict(frame)
    height = frame["h.t"].astype(float)
    diameter = frame["d.bh"].astype(float)
    canopy = frame["a.cp"].astype(float)
    age = frame["age"].astype(float)
    # Deliberately simple, non-calibrated fixture. It preserves monotonic UI
    # behavior only and must never be used for science or carbon accounting.
    return 0.18 * height + 0.11 * diameter + 0.07 * canopy + 0.03 * age

# 单样本预测
def predict_carbon_stock(input_dict):
    try:
        sample = _sample(input_dict)
        X = pd.DataFrame([sample])
        y_pred = _predict_frame(X)[0]
        return round(float(y_pred), 3)
    except Exception as e:
        raise ValueError(f"预测失败：{e}")

# 批量样地预测
def batch_predict_carbon_stock(sample_list):
    try:
        if not isinstance(sample_list, list) or not sample_list:
            raise ValueError("批量预测必须包含至少一个样本")
        if len(sample_list) > 100:
            raise ValueError("批量预测最多接受 100 个样本")
        records = [_sample(item) for item in sample_list]
        df = pd.DataFrame(records)
        y_preds = _predict_frame(df)
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
        decreased_prediction = predict_carbon_stock(varied_input)

        # +20%
        varied_input[key] = base_value * 1.2
        increased_prediction = predict_carbon_stock(varied_input)

        lower = min(decreased_prediction, increased_prediction)
        upper = max(decreased_prediction, increased_prediction)

        results.append({
            "param": key,
            "base": base_value,
            "lower": round(lower, 3),
            "upper": round(upper, 3),
            "range": round(abs(upper - lower), 3),
        })

    return {
        "original": original,
        "sensitivity": results
    }

def save_sensitivity_plot(sens_result, output_dir=None):
    import matplotlib.pyplot as plt
    import os, uuid

    output_dir = Path(
        output_dir
        or os.getenv("HNBLUE_MODEL_OUTPUT_DIR", str(Path(__file__).resolve().parent / "sensitivity_plots"))
    ).expanduser().resolve()
    output_dir.mkdir(parents=True, exist_ok=True)

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
        label=f'Original m.so prediction: {sens_result["original"]:.2f}'
    )

    ax.set_title("Prediction Sensitivity to Parameter Perturbations", fontsize=14)
    ax.set_ylabel("Predicted m.so (raw model output)", fontsize=12)
    ax.set_xlabel("Perturbed Parameters", fontsize=12)
    ax.set_ylim(min(lowers) - 1, max(uppers) + 1)
    ax.grid(True, linestyle='--', alpha=0.5)
    ax.legend()

    plt.tight_layout()
    filename = f"sensitivity_{uuid.uuid4().hex[:8]}.png"
    path = output_dir / filename
    plt.savefig(path, dpi=150)
    plt.close()

    public_base = os.getenv("HNBLUE_MODEL_PUBLIC_BASE", "").rstrip("/")
    return f"{public_base}/sensitivity_plots/{filename}"

# 结构文本解释函数
def explain_sensitivity(sens_result):
    lines = [f"m.so 原始模型预测为 **{sens_result['original']}**（尚未换算为单位面积碳储量）。"]
    for item in sens_result["sensitivity"]:
        lines.append(
            f"- 当 `{item['param']}` 变化 ±20% 时，原始预测区间为 **{item['lower']}–{item['upper']}**，"
            f"原始输出波动幅度为 **{item['range']}**。"
        )
    lines.append("下面开始分析这些变量对碳储预测的敏感性及其生态学含义。")
    return "\n".join(lines)
