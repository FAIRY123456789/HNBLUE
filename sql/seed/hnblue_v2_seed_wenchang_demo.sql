-- HNBLUE V2.0 Wenchang demo seed
-- 需根据实际 V2.0 表名复核。当前仓库未找到 hnblue_v2_schema.sql。
-- 不要在 V1.0 hnblue 演示库执行。

INSERT IGNORE INTO hainan_blue_carbon_core.t_region
(region_id, region_name, region_type, admin_level, parent_region_id, center_lon, center_lat, bbox_wkt, spatial_scope_note, source_id, source_category, is_simulated, quality_level, status, notes)
VALUES
('REG-HI-0001','海南省','province','province',NULL,109.85,19.20,NULL,'省级行政区。中心点为公开常识级近似值，正式入库需以民政或自然资源边界替换。','SRC-GOV-001','PUBLIC',0,'B','candidate','不写入 hnblue V1.0'),
('REG-WC-0001','文昌市','city','county','REG-HI-0001',110.80,19.55,NULL,'海南省文昌市。中心点为公开常识级近似值，边界待行政区划矢量复核。','SRC-GOV-002','PUBLIC',0,'B','candidate','文昌示范区父级对象'),
('REG-WC-BMB-001','文昌八门湾红树林相关区域','demonstration_area','site','REG-WC-0001',NULL,NULL,NULL,'公开来源明确出现文昌八门湾红树林和蓝碳调查语境。本轮未取得法定边界。','SRC-LIT-001','PUBLIC',0,'A','candidate','不得补写未核验坐标'),
('REG-WC-QLG-001','清澜港红树林相关区域','protected_or_port_context','site','REG-WC-0001',NULL,NULL,NULL,'清澜港和清澜红树林相关公开语境需后续核验行政归属和保护地边界。','SRC-GOV-001','PUBLIC',0,'B','pending_verification','仅做区域名候选');

INSERT IGNORE INTO hainan_blue_carbon_core.t_data_version
(version_id, source_id, version_code, publication_date, temporal_scope, processing_level, processing_method, ingest_status, notes)
VALUES
('VER-GMW-003','SRC-GMW-002','v3.0','2022','1996-2020','L0_metadata_only','source_registered_no_download','pending','后续按文昌区域裁剪统计'),
('VER-S2-HARMONIZED','SRC-RS-001','GEE_COPERNICUS_S2_SR_HARMONIZED','2015-present','2015-present','L0_metadata_only','source_registered_no_export','pending','GEE 计算后生成区域指标版本'),
('VER-MOD17-061','SRC-RS-002','MODIS_061_MOD17A3HGF','2000-present','2000-present','L0_metadata_only','source_registered_no_export','pending','GPP/NPP 作为替代通量'),
('VER-LIT-BMB-SOC-2022','SRC-LIT-001','article_2022','2022','论文采样期见正文','L1_literature_candidate','manual_table_review_required','pending','表号页码复核后录入数值'),
('VER-LIT-HI-GHG-2022','SRC-LIT-002','article_2022','2022','论文统计期见正文','L1_literature_candidate','manual_table_review_required','pending','省域清单和 GHG 方法');

INSERT IGNORE INTO hainan_blue_carbon_core.t_data_asset_index
(asset_id, source_id, version_id, asset_name, asset_type, file_format, remote_url, cold_data_path_suggestion, spatial_scope, temporal_scope, coordinate_system, resolution, download_status, checksum, status, notes)
VALUES
('AST-GMW-V3','SRC-GMW-002','VER-GMW-003','GMW 1996-2020 v3 product files','remote_sensing_dataset','GeoTIFF/vector','https://zenodo.org/records/6894273','cold_data/remote_sensing/gmw/v3/','全球含海南','1996-2020','unknown','产品页面复核','not_downloaded',NULL,'candidate','大体量文件不入仓库'),
('AST-S2-GEE','SRC-RS-001','VER-S2-HARMONIZED','Sentinel-2 SR Harmonized GEE collection','gee_collection','ImageCollection','https://developers.google.com/earth-engine/datasets/catalog/COPERNICUS_S2_SR_HARMONIZED','cold_data/remote_sensing/sentinel2/exports/','全球含海南','2015至今','WGS84','10m/20m/60m','not_downloaded',NULL,'candidate','只登记 GEE 产品'),
('AST-MOD17-GEE','SRC-RS-002','VER-MOD17-061','MODIS MOD17A3HGF GPP NPP','gee_collection','ImageCollection','https://developers.google.com/earth-engine/datasets/catalog/MODIS_061_MOD17A3HGF','cold_data/remote_sensing/modis_mod17/exports/','全球含海南','2000至今','Sinusoidal','500m 产品级别待页面复核','not_downloaded',NULL,'candidate','替代通量');
