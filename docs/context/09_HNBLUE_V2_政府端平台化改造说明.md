# HNBLUE V2.0 政府端平台化改造说明

## 新增页面

- `/v2-government`：政府端数据治理与决策支持工作台，包含首页驾驶舱、蓝碳资源一张图雏形、区域指标库、文献碳储证据库、数据治理与来源追溯、AI 上下文调试、待接入数据资产。
- `/v2-public-data` 保持原公开数据包静态展示入口，未删除旧页面和旧接口。

## 新增接口

- `GET /api/v2/health`：返回 V2 API 状态、数据库连通性、核心表可读性和当前时间。
- `GET /api/v2/dashboard/summary`：返回指标字典、数据源、红树林面积、区域指标、文献碳储、source_code、区域数量与更新时间字段边界。
- `GET /api/v2/sources`：支持 `source_type`、`source_code`、`keyword`、`limit`、`offset`。live schema 未提供 `reliability_level`，接口在 `unsupported_filters` 中说明。
- `GET /api/v2/mangrove-cover`：支持 `region`、`region_id`、`year`、`source_code`、`keyword`、`limit`、`offset`。
- `GET /api/v2/region-metrics`：支持 `region`、`region_id`、`metric_category`、`indicator_code`、`year`、`source_code`、`quality_flag`、`keyword`、`limit`、`offset`。
- `GET /api/v2/literature-carbon`：支持 `region`、`region_id`、`carbon_pool`、`data_type`、`method_type`、`source_code`、`keyword`、`limit`、`offset`。
- `GET /api/v2/region-overview/{regionId}`：按 BIGINT `region_id` 兼容字符串输入，也支持 `region_code` 回退。
- `GET /api/v2/ai/context`：基于问题、区域和主题返回 sources、mangrove-cover、region-metrics、literature-carbon 结构化上下文，不直接调用大模型。
- `GET /api/v2/optional/status`：返回模型参数、碳市场价格、CCER 方法学、本地交易案例、政策目标、派生指标、生态节点、治理任务等增强资产的 live table 与记录数状态。

## 依赖数据表

- `hainan_blue_carbon_core.t_indicator_dictionary`
- `hainan_blue_carbon_core.t_data_source`
- `hainan_blue_carbon_core.t_region`
- `hainan_blue_carbon_satellite.t_satellite_mangrove_cover`
- `hainan_blue_carbon_satellite.t_satellite_region_metric`
- `hainan_blue_carbon_intl.t_literature_carbon_record`
- optional：`t_carbon_model_parameter`、`t_carbon_market_price`、`t_carbon_methodology`、`t_local_carbon_trade_case`、`t_policy_target`、`t_derived_metric_definition`、`t_ecological_node`、`t_data_governance_task`

## 字段边界

- `t_data_source` live schema 使用 `source_category`，V2 API 兼容输出为 `source_type`。
- `t_data_source` live schema 未提供 `reliability_level`，前端不伪造可靠性等级。
- 面积记录保留 `classification_method`、`overall_accuracy`、`kappa`、`source_code`，后端不跨来源计算平均值。
- 区域指标保留 `is_proxy`、`is_simulated`、`quality_level`、`method_note` 和 `notes`，后端不把 proxy 解释为实测破坏或实测碳通量。
- 文献碳储记录保留 `carbon_pool`、`value_mg_ha`、`value`、`unit`、`is_proxy`、`is_simulated`、`method_note`、`notes` 和 citation 来源。

## 政府端表达规则

- 观测事实、模型结果、情景预测、proxy 指标分开呈现。
- 生物量、碳量和碳价值估算分开呈现。
- 全国 CEA、CCER、自愿减排行情仅作为参考上下文，不能写成海南本地真实成交价。
- A 类 Total 或样区合计记录的含义取决于来源口径，不能自动解释为海南省总量。
- 图表与表格必须显示 `source_code`、来源说明和边界提示。

## AI 上下文设计

`/api/v2/ai/context` 返回检索上下文，不执行生成。前端展示 `source_code`、`indicator_code`、区域、数值、单位、证据文本，便于后续 LLM 回复引用。后续接入大模型时，应把 proxy、模型、情景预测和碳价边界作为系统提示固定约束。

## optional records 下一步导入计划

1. 对 optional live tables 执行字段级验收，补充缺失的 source 外键映射。
2. 将 `data/processed/hnblue_v2_region_resolution/import_ready_updated` 中的增强数据建立批次导入日志。
3. 在政府端页面增加 optional records 详情表，不将参考价格转写为本地收益。
4. 将数据治理任务表接入后台任务流，支持核验状态、阻塞原因和责任页面追踪。
