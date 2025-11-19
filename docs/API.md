# HNBLUE 接口说明（API）

本文档描述 HNBLUE 系统的接口约定，覆盖两类调用链：  
（1）前端 → 后端（Spring Boot）；  
（2）前端 → 模型服务（Flask）。  

所有接口均采用 JSON 格式交互，时间统一为 ISO 8601 格式，字符编码为 UTF-8。除登录与注册外，所有接口请求需在请求头中携带 `Authorization: Bearer <token>`。前端直接调用 Spring Boot 获取业务与指标数据，同时直连 Flask 获取模型预测与 SHAP 解释结果。

---

## 1. 基本约定

- **基础路径**  
  - 后端：`/api/...`  
  - 模型服务：`/predict`, `/predict/batch`

- **统一响应结构**  
  - 成功：`{ "code": 0, "message": "OK", "data": ... }`  
  - 失败：`{ "code": <非零>, "message": "<错误信息>", "requestId": "<可追踪ID>" }`  

- **常见错误码**  
  - 401 Unauthorized：令牌缺失或无效  
  - 403 Forbidden：权限不足  
  - 404 Not Found：资源不存在  
  - 429 Too Many Requests：限流触发  
  - 500 Internal Server Error：服务器内部错误  

---

## 2. 认证与用户管理

用户认证与权限控制由 Spring Boot 后端提供。令牌颁发后，前端需在后续请求中统一携带。

- **登录**  
  - POST `/api/login`  
  - Request:  
    ```json
    { "username": "alice", "password": "******" }
    ```  
  - Response:  
    ```json
    {
      "code": 0,
      "message": "OK",
      "data": { "token": "jwt-token", "userType": "Admin" }
    }
    ```

- **注册**  
  - POST `/api/register`  
  - Request:  
    ```json
    { "username": "alice", "password": "******", "email": "a@b.com" }
    ```

- **管理员接口（分页与模糊查询）**  
  - GET `/api/admin/users?page=1&size=20&keyword=ali`  
  - Response:  
    ```json
    {
      "code": 0,
      "message": "OK",
      "data": {
        "total": 235,
        "items": [
          { "userId": 1, "username": "alice", "email": "a@b.com", "userType": "User" }
        ]
      }
    }
    ```

---

## 3. 区域与指标数据

区域与指标信息由 Spring Boot 后端统一管理。前端根据不同页面（总览、详情、专题分析）调用相关接口。

- **区域列表**  
  - GET `/api/regions`  
  - Response:  
    ```json
    {
      "code": 0,
      "message": "OK",
      "data": [ { "id": 46, "name": "海口" }, { "id": 47, "name": "三亚" } ]
    }
    ```

- **当前指标值**  
  - GET `/api/indicators/current?region=海口`  
  - Response:  
    ```json
    {
      "code": 0,
      "message": "OK",
      "data": {
        "region": "海口",
        "asOf": "2025-03-01",
        "metrics": {
          "SOC": 12.34,
          "AGB": 56.78,
          "CO2_flux": -0.45,
          "CH4_flux": 0.03,
          "GWP": 1.2
        }
      }
    }
    ```

- **指标趋势**  
  - GET `/api/indicators/trends?region=海口&from=2015&to=2024`  
  - Response:  
    ```json
    {
      "code": 0,
      "message": "OK",
      "data": {
        "region": "海口",
        "series": [
          { "year": 2015, "SOC": 10.1, "AGB": 50.2 },
          { "year": 2016, "SOC": 10.6, "AGB": 51.0 }
        ]
      }
    }
    ```

---

## 4. 模型预测与解释（Flask）

模型接口由 Flask 服务直接对前端开放，负责碳储预测与 SHAP 可解释性分析。前端在可视化组件中调用这些接口，以获取预测结果或解释图表。

- **单样本预测**  
  - POST `/predict`  
  - Request:  
    ```json
    {
      "features": {
        "lat": 19.3, "lon": 110.4, "MAT": 24.1, "MAP": 1800,
        "age": 12, "dbh": 14.5, "height": 8.3, "crown": 3.2,
        "type": "mangrove_like"
      }
    }
    ```  
  - Response:  
    ```json
    { "code": 0, "message": "OK", "data": { "prediction": 72.45, "unit": "tC/ha" } }
    ```

- **批量预测**  
  - POST `/predict/batch`  
  - Request:  
    ```json
    {
      "rows": [
        { "lat": 19.3, "lon": 110.4, "MAT": 24.1, "MAP": 1800, "age": 12, "dbh": 14.5, "height": 8.3, "crown": 3.2, "type": "mangrove_like" },
        { "lat": 19.1, "lon": 110.2, "MAT": 24.3, "MAP": 1750, "age": 10, "dbh": 13.1, "height": 7.9, "crown": 3.0, "type": "mangrove_like" }
      ]
    }
    ```

- **单样本 SHAP 解释**  
  - POST `/shap/local`  
  - Response:  
    ```json
    {
      "code": 0,
      "message": "OK",
      "data": {
        "baseValue": 60.12,
        "contributions": [
          { "feature": "dbh", "value": 14.5, "shap": 5.31 },
          { "feature": "height", "value": 8.3, "shap": 3.02 }
        ],
        "prediction": 72.45
      }
    }
    ```

- **全局特征重要性**  
  - GET `/shap/global`  
  - Response:  
    ```json
    {
      "code": 0,
      "message": "OK",
      "data": [
        { "feature": "dbh", "mean_abs_shap": 4.12 },
        { "feature": "height", "mean_abs_shap": 3.67 }
      ]
    }
    ```

---

## 5. 专题与可视化模块

前端的专题页面（如详情页、参数敏感性、响应曲线、文献比对、碳价值转换器等）均基于上述接口组合完成。详情页通过区域参数获取指标时间序列，专题分析模块调用 Flask 的模型与 SHAP 接口，并结合后端的区域指标接口进行联动。

- **详情页数据**  
  - GET `/api/regions/{name}/detail?from=2015&to=2024`  
  - Response:  
    ```json
    {
      "code": 0,
      "message": "OK",
      "data": {
        "region": "琼海",
        "metrics": ["SOC", "AGB", "CO2_flux"],
        "series": [
          { "year": 2015, "SOC": 9.8, "AGB": 48.1 },
          { "year": 2016, "SOC": 10.0, "AGB": 49.0 }
        ]
      }
    }
    ```

---

## 6. 并发与限流

后端部分接口应用基于 Redis Cluster 的限流机制。当流量超过阈值时返回错误响应。前端应在 UI 层配合退避或禁用操作，避免重复请求。

- **限流响应示例**  
  ```json
  { "code": 10001, "message": "Too Many Requests", "requestId": "a1b2c3" }
