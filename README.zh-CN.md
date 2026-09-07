# HNBLUE

[English](README.md) · 简体中文

HNBLUE 是一个蓝碳研究全栈原型，组合了环境数据探索、模型推理与解释、交互式可视化、应用 API 和知识辅助分析。

本仓库展示系统集成能力，不是业务运行中的碳核算平台；已有实验不能证明生产准确率、科学外推能力或高可用性能。

## 组成

| 模块 | 技术 | 作用 |
|---|---|---|
| `backend/` | Java 17、Spring Boot、JPA、MySQL、Redis、JWT、MQTT | API、持久化、鉴权与集成 |
| `frontend/` | Vue 3、ECharts | 地图、时间序列、对比和报告界面 |
| `flask_model/` | Flask、CatBoost、scikit-learn | 模型推理、响应曲线与分析脚本 |
| `redis_cluster/` | Redis 配置与 Shell 脚本 | 限流和互斥锁的本地集群实验 |
| `docs/` | Markdown 报告 | 架构、API、功能、测试与实验记录 |
| `figures/` | 生成图像 | 模型和界面示意 |

知识辅助模块可以连接外部 OpenAI 兼容模型或本地知识工具，但依赖具体环境，属于可选集成。

## 架构

```text
Vue / ECharts
      |
      v
Spring Boot API ---- MySQL
      |       \
      |        -> Redis 实验集群
      |        -> MQTT 集成点
      v
Flask 模型服务 -> CatBoost 推理 / 解释工件

可选外部知识助手
```

## 本地运行

仓库包含多个独立组件，不提供单命令生产安装器。

后端：

```powershell
cd backend
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

前端：

```powershell
cd frontend
npm ci
npm run serve
```

模型服务：

```powershell
python -m pip install -r flask_model/carbon_model_api/requirements.txt
python flask_model/carbon_model_api/app.py
```

MySQL、Redis、MQTT 和可选模型服务配置需在本地提供。启动集成环境前应先阅读模块文档与配置文件。

## 证据与限制

- 留存模型输出和图表记录的是实验，不等同于独立验证；
- 仓库内任何模型指标都不能解释为对新生态系统或业务数据的性能保证；
- Redis 脚本是集群实验，不证明生产级持续并发或可用性；
- 样例数据可能有独立的来源与复用条件；
- 外部 AI 回答必须复核来源，不能作为科学权威结论。

## 安全

不得提交数据库密码、JWT 签名材料、邮件凭据、MQTT 凭据、模型 API Key、内部地址或私有数据。环境专用配置应只保存在本地；任何曾经泄露的凭据都应轮换。

## 许可证

仓库软件采用 [Apache License 2.0](LICENSE)。第三方数据、模型工件、图片和依赖可能具有独立条款。