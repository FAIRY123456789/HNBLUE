"""
Flask碳储量预测API服务

功能概述：
• 提供地上生物量和碳储量的单样本/批量预测接口
• 支持敏感性分析及可视化结果生成
• 返回文献数据碳储量统计范围参考
• 提供模型预测解释性分析

API端点说明：
• /api/predict-carbon - 单样本碳储量预测
• /api/predict-carbon/batch - 批量样本预测
• /api/literature-range - 获取历史数据碳储量统计范围
• /api/sensitivity-analysis - 特征敏感性分析
• /api/sensitivity-plot - 生成敏感性分析图表
• /api/sensitivity-explain - 敏感性结果解释说明

技术特性：
• 支持CORS跨域请求（前端开发友好）
• 统一的JSON响应格式
• 错误处理与日志输出
• 静态文件服务（敏感性分析图表）

数据流：
• 前端JSON请求 → 模型预测服务 → 标准化JSON响应
• 敏感性分析结果 → 可视化图表生成 → 静态资源URL返回

依赖服务：
• predict_service - 核心预测逻辑模块
• 预训练CatBoost模型管道
• 历史数据集(BAAD_cleaned.csv)统计参考
"""
from flask import Flask, request, Response, jsonify, send_from_directory
import json
from predict_service import predict_carbon_stock, batch_predict_carbon_stock, sensitivity_analysis, save_sensitivity_plot, explain_sensitivity
from flask_cors import CORS  # 引入 CORS 支持

# Flask 应用对象
app = Flask(__name__)
CORS(app, origins=["http://localhost:8080"])

# 所有路由都是以app对象为基础，所以称为@app.route
@app.route("/api/predict-carbon", methods=["POST"])
def predict():
    print("/api/predict-carbon 被调用了！")
    try:
        input_data = request.get_json(force=True)
        result = predict_carbon_stock(input_data)

        # 构造 JSON 字符串，并手动设置正确的 Content-Type
        response_data = json.dumps({"prediction": result})
        return Response(response=response_data, status=200, mimetype="application/json")

    except Exception as e:
        error_data = json.dumps({"error": str(e)})
        return Response(response=error_data, status=500, mimetype="application/json")

@app.route("/api/predict-carbon/batch", methods=["POST"])
def batch_predict():
    print("/api/predict-carbon/batch 被调用了！")
    try:
        sample_list = request.get_json(force=True)
        if not isinstance(sample_list, list):
            raise ValueError("请提交样地数组（list），而不是单个样地对象。")

        results = batch_predict_carbon_stock(sample_list)
        return jsonify({"predictions": results})

    except Exception as e:
        error_data = json.dumps({"error": str(e)})
        return Response(response=error_data, status=500, mimetype="application/json")

@app.route("/api/literature-range", methods=["GET"])
def literature_range():
    import pandas as pd
    try:
        df = pd.read_csv("../BAAD_cleaned.csv")  # 路径视部署位置调整
        if "m.so" not in df.columns:
            return jsonify({"error": "字段 m.so 不存在"}), 400

        cf = 0.48  # 碳分数
        carbon = df["m.so"].dropna() * cf
        stats = carbon.describe(percentiles=[0.25, 0.5, 0.75])
        result = {
            "min": round(stats["min"], 2),
            "25%": round(stats["25%"], 2),
            "median": round(stats["50%"], 2),
            "75%": round(stats["75%"], 2),
            "max": round(stats["max"], 2),
            "mean": round(stats["mean"], 2),
            "std": round(stats["std"], 2),
            "count": int(stats["count"])
        }
        # 打印日志到终端，方便检查
        print("碳储量统计区间（单位：tC/ha）")
        for k, v in result.items():
            print(f"🔹 {k}: {v}")

        return jsonify(result)
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route("/api/sensitivity-analysis", methods=["POST"])
def sensitivity():
    try:
        input_data = request.get_json(force=True)
        result = sensitivity_analysis(input_data)
        return jsonify(result)
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route("/api/sensitivity-plot", methods=["POST"])
def sensitivity_plot():
    try:
        input_data = request.get_json(force=True)
        result = sensitivity_analysis(input_data)
        img_url = save_sensitivity_plot(result)  # 获取完整可访问 URL
        result["img"] = img_url
        return jsonify(result)
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/sensitivity_plots/<filename>')
def serve_sensitivity_plot(filename):
    return send_from_directory('sensitivity_plots', filename)

@app.route("/api/sensitivity-explain", methods=["POST"])
def sensitivity_explain():
    input_data = request.get_json(force=True)
    result = sensitivity_analysis(input_data)
    explanation = explain_sensitivity(result)
    return jsonify({
        "result": result,
        "explanation": explanation
    })

# 用作测试
@app.route("/ping", methods=["GET"])
def ping():
    print("/ping 路由被调用了！")
    return jsonify({"msg": "pong"})

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8880)
