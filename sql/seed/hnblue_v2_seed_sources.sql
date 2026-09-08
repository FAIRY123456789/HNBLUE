-- HNBLUE V2.0 seed sources
-- 需根据实际 V2.0 表名复核。当前仓库未找到 hnblue_v2_schema.sql。
-- 不要在 V1.0 hnblue 演示库执行。

INSERT IGNORE INTO hainan_blue_carbon_core.t_data_source
(source_id, source_name, source_category, source_type, publisher, author, publication_date, access_date, source_url, doi, license_note, quality_level, is_simulated, trust_reason, limitations)
VALUES
('SRC-GOV-001','海南省红树林保护规划相关公开页面','PUBLIC','policy','国家林草局/海南相关部门','','2025','2026-05-25','https://www.forestry.gov.cn/','','页面未见明确开放数据许可','B',0,'政府或主管部门公开规划线索','未取得机器可读边界'),
('SRC-GOV-002','海南蓝碳试点与生态产品信息','PUBLIC','government_report','海南省人民政府','','2023-07','2026-05-25','https://en.hainan.gov.cn/hainan/zmgyshj/202307/3f612bd40e1941a080bf7abef3b71c69.shtml','','政府网页使用限制待复核','B',0,'海南省政府公开页面','报道型页面，无原始表格下载'),
('SRC-GMW-002','Global Mangrove Watch 1996-2020 Version 3.0','PUBLIC','remote_sensing_dataset','Global Mangrove Watch/Zenodo','','2022','2026-05-25','https://zenodo.org/records/6894273','10.5281/zenodo.6894273','Zenodo 页面许可和 DOI 引用要求','A',0,'正式 DOI 数据集','冷数据登记，不入仓大文件'),
('SRC-RS-001','Sentinel-2 MSI Level-2A Surface Reflectance Harmonized','PUBLIC','remote_sensing_product','ESA/Copernicus;Google Earth Engine','','2015-present','2026-05-25','https://developers.google.com/earth-engine/datasets/catalog/COPERNICUS_S2_SR_HARMONIZED','','GEE 和 Copernicus 使用条款','A',0,'权威遥感产品目录','需 GEE 计算区域统计'),
('SRC-RS-002','MODIS MOD17A3HGF GPP/NPP','PUBLIC','remote_sensing_product','NASA LP DAAC;Google Earth Engine','','2000-present','2026-05-25','https://developers.google.com/earth-engine/datasets/catalog/MODIS_061_MOD17A3HGF','','NASA LP DAAC 使用和引用要求','A',0,'权威遥感生产力产品','替代通量指标，必须 is_proxy=1'),
('SRC-LIT-001','Composition and distribution of soil organic carbon in mangroves: a case study from Bamen Bay','PUBLIC','journal_article','MDPI Water','见期刊页面','2022','2026-05-25','https://www.mdpi.com/2073-4441/14/20/3278','10.3390/w14203278','CC BY 许可','A',0,'同行评议开放论文','表号页码待人工复核'),
('SRC-LIT-002','Accounting of blue carbon and greenhouse gas emissions in Hainan Province','PUBLIC','journal_article','Frontiers in Marine Science','见期刊页面','2022','2026-05-25','https://www.frontiersin.org/journals/marine-science/articles/10.3389/fmars.2022.932984/full','10.3389/fmars.2022.932984','CC BY 许可','A',0,'同行评议开放论文','省域统计不能拆为样木明细'),
('SRC-METHOD-001','IPCC 2013 Wetlands Supplement','PUBLIC','methodology','IPCC','','2013','2026-05-25','https://www.ipcc-nggip.iges.or.jp/public/wetlands/','','IPCC 使用条款','A',0,'国际核算指南','方法来源，不直接写数值'),
('SRC-METHOD-002','Coastal Blue Carbon Methods Manual','PUBLIC','methodology','Blue Carbon Initiative','Howard et al.','2014','2026-05-25','https://www.thebluecarboninitiative.org/manual','','页面许可需复核','A',0,'蓝碳调查方法手册','方法来源，不直接写数值'),
('SRC-METHOD-003','Verra VM0033 Tidal Wetland and Seagrass Restoration','PUBLIC','methodology','Verra','','v2.1','2026-05-25','https://verra.org/methodologies/vm0033-methodology-for-tidal-wetland-and-seagrass-restoration-v2-1/','','Verra 文档条款','A',0,'蓝碳核证方法学','核证方法，不直接写数值'),
('SRC-ALLO-001','BAAD Biomass And Allometry Database','PUBLIC','allometry_dataset','BAAD authors/GitHub','Falster et al. and contributors','various','2026-05-25','https://github.com/dfalster/baad','','仓库许可和论文引用需保留','A',0,'公开模型参考数据库','非海南专属');
