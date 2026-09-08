# 外部来源字段到 HNBLUE V2.0 字段映射

`hnblue_v2_schema.sql` 当前仓库未找到。以下映射依据 `docs/context/04_database_and_data_context.md` 中的 V2.0 Schema 边界和建议表名生成，正式导入前需按实际 SQL 文件复核字段名、主键、外键和枚举值。

| source_id | 外部字段 | 原单位 | HNBLUE 目标表 | 目标字段 | 目标单位 | 转换规则 | 必填 | 置信度 | 说明 |
|---|---|---|---|---|---|---|---|---|---|
| 全部 | title/name | 无 | hainan_blue_carbon_core.t_data_source | source_name | 无 | 原样清洗，保留英文名 | 是 | 高 | 来源登记主字段 |
| 全部 | publisher/organization | 无 | hainan_blue_carbon_core.t_data_source | publisher | 无 | 机构规范化 | 是 | 高 | 政府部门和数据平台保留全称 |
| 全部 | url | URL | hainan_blue_carbon_core.t_data_source | source_url | URL | 原样记录 | 是 | 高 | 每条数据必须可追溯 |
| 全部 | access_date | date | hainan_blue_carbon_core.t_data_source | access_date | date | 统一为 2026-05-25 | 是 | 高 | 不替代数据年份 |
| 全部 | license | text | hainan_blue_carbon_core.t_data_source | license_note | text | 摘要化记录 | 是 | 中 | 不复制长文本 |
| SRC-GMW-002 | year | year | hainan_blue_carbon_satellite.t_satellite_product_metadata | temporal_scope | year range | 1996-2020 | 是 | 高 | GMW 年份序列 |
| SRC-GMW-002 | mangrove_extent | raster/vector | hainan_blue_carbon_satellite.t_mangrove_cover_result | cover_area | ha 或 km2 | 后续按区域裁剪统计 | 否 | 中 | 本轮不计算数值 |
| SRC-RS-001 | CLOUDY_PIXEL_PERCENTAGE/QA60/SCL | percent/code | hainan_blue_carbon_satellite.t_satellite_product_metadata | quality_band | code/text | 记录质量字段名称 | 否 | 高 | 用于云掩膜 |
| SRC-RS-001 | B2/B3/B4/B8 | reflectance | hainan_blue_carbon_satellite.t_satellite_region_metric | ndvi/evi inputs | reflectance | 后续 GEE 计算 NDVI | 否 | 高 | 仅元数据登记 |
| SRC-RS-002 | Gpp | kg C/m2/year 或产品单位 | hainan_blue_carbon_flux.t_flux_or_proxy_candidate | proxy_value | 按产品说明 | 保留原始单位，转换规则待脚本实现 | 否 | 高 | is_proxy=1 |
| SRC-RS-002 | Npp | kg C/m2/year 或产品单位 | hainan_blue_carbon_flux.t_flux_or_proxy_candidate | proxy_value | 按产品说明 | 保留原始单位 | 否 | 高 | 替代通量指标 |
| SRC-LIT-001 | SOC/soil organic carbon | 论文原单位 | hainan_blue_carbon_intl.t_literature_carbon_record | carbon_value | 原单位和标准单位双记 | 表格值录入，禁止图估读混写 | 否 | 中 | 页码或表号待复核 |
| SRC-LIT-001 | soil depth | cm | hainan_blue_carbon_ground.t_soil_sample_candidate | soil_depth_cm | cm | 原样记录 | 否 | 中 | 公开来源若只给均值则不造样品 |
| SRC-LIT-002 | ecosystem type | text | hainan_blue_carbon_intl.t_literature_carbon_record | ecosystem_type | text | mangrove/seagrass 等规范化 | 是 | 高 | 省级清单来源 |
| SRC-LIT-002 | GHG emissions | 论文原单位 | hainan_blue_carbon_flux.t_flux_or_proxy_candidate | flux_value | 原单位 | 不换算或注明换算 | 否 | 中 | 需区分 CO2/CH4/N2O |
| SRC-ALLO-001 | dbh | cm | hainan_blue_carbon_model.t_model_reference_dataset | dbh_cm | cm | 原样或单位换算 | 否 | 高 | CatBoost 参考变量 |
| SRC-ALLO-001 | height | m | hainan_blue_carbon_model.t_model_reference_dataset | tree_height_m | m | 原样 | 否 | 高 | 模型输入变量 |
| SRC-ALLO-001 | biomass | kg 或 Mg | hainan_blue_carbon_model.t_model_reference_dataset | biomass_value | 原单位 | 保留原始单位和换算说明 | 否 | 高 | 非海南实测 |
| SRC-METHOD-001 | pool/emission factor/method | text | hainan_blue_carbon_knowledge.t_knowledge_source | knowledge_topic | text | 按主题拆分 | 否 | 高 | AI 碳助手候选 |
