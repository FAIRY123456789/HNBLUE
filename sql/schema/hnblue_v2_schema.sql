-- HNBLUE V2.0 database reference schema
-- Generated for HNBLUE context package.
-- MySQL version target: MySQL 8.0+
-- Principle: preserve the current hnblue schema as V1.0 visualization/legacy layer.
-- This script builds the V2.0 data-governance layer across hainan_blue_carbon_* schemas.
-- Recommended usage: run in a fresh development database first. Do not execute in production without backup.

SET NAMES utf8mb4;
SET time_zone = '+00:00';
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `hainan_blue_carbon_core` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `hainan_blue_carbon_ground` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `hainan_blue_carbon_flux` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `hainan_blue_carbon_satellite` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `hainan_blue_carbon_uav` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `hainan_blue_carbon_model` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `hainan_blue_carbon_intl` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `hainan_blue_carbon_local` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ============================================================
-- 1. Core domain: region, site, source, version, indicator, asset
-- ============================================================

USE `hainan_blue_carbon_core`;

DROP TABLE IF EXISTS `t_data_asset_index`;
DROP TABLE IF EXISTS `t_indicator_dictionary`;
DROP TABLE IF EXISTS `t_quality_flag`;
DROP TABLE IF EXISTS `t_data_version`;
DROP TABLE IF EXISTS `t_data_source`;
DROP TABLE IF EXISTS `t_site`;
DROP TABLE IF EXISTS `t_project`;
DROP TABLE IF EXISTS `t_region`;

CREATE TABLE `t_region` (
  `region_id` bigint NOT NULL AUTO_INCREMENT COMMENT '区域主键',
  `region_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '区域编码，如 HN、HN_WENCHANG、HN_WENCHANG_QINGLAN',
  `parent_region_id` bigint DEFAULT NULL COMMENT '上级区域ID',
  `region_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '区域名称',
  `region_level` enum('PROVINCE','CITY','COUNTY','DEMO_AREA','PROTECTED_AREA','SITE_AREA','GRID','OTHER') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OTHER' COMMENT '区域层级',
  `administrative_code` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '行政区划代码',
  `province` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '海南省' COMMENT '省级名称',
  `city` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '市县名称',
  `county` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '区县/乡镇名称',
  `ecosystem_type` enum('MANGROVE','SEAGRASS','SALT_MARSH','COASTAL_WETLAND','MIXED','OTHER') COLLATE utf8mb4_unicode_ci DEFAULT 'MANGROVE' COMMENT '生态系统类型',
  `centroid_lat` decimal(10,7) DEFAULT NULL COMMENT '中心纬度',
  `centroid_lon` decimal(10,7) DEFAULT NULL COMMENT '中心经度',
  `area_ha` decimal(18,4) DEFAULT NULL COMMENT '区域面积，单位 ha',
  `geometry_wkt` longtext COLLATE utf8mb4_unicode_ci COMMENT '区域边界 WKT；大范围边界可只存冷数据文件路径',
  `protection_status` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '保护地/示范区状态',
  `data_scope` enum('PUBLIC','INTERNAL','RESTRICTED','CONFIDENTIAL') COLLATE utf8mb4_unicode_ci DEFAULT 'INTERNAL' COMMENT '默认数据共享范围',
  `remark` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`region_id`),
  UNIQUE KEY `uk_region_code` (`region_code`),
  KEY `idx_region_parent` (`parent_region_id`),
  KEY `idx_region_level` (`region_level`),
  CONSTRAINT `fk_core_region_parent` FOREIGN KEY (`parent_region_id`) REFERENCES `t_region` (`region_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='核心区域主数据表';

CREATE TABLE `t_project` (
  `project_id` bigint NOT NULL AUTO_INCREMENT COMMENT '项目主键',
  `project_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '项目编码',
  `project_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '项目名称',
  `project_type` enum('RESEARCH','GOVERNMENT_TASK','DEMO','THESIS','DATA_COLLECTION','OTHER') COLLATE utf8mb4_unicode_ci DEFAULT 'RESEARCH' COMMENT '项目类型',
  `lead_org` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '牵头单位',
  `contact_person` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系人',
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `status` enum('PLANNED','ACTIVE','PAUSED','FINISHED','ARCHIVED') COLLATE utf8mb4_unicode_ci DEFAULT 'PLANNED' COMMENT '项目状态',
  `remark` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`project_id`),
  UNIQUE KEY `uk_project_code` (`project_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目与任务主数据表';

CREATE TABLE `t_site` (
  `site_id` bigint NOT NULL AUTO_INCREMENT COMMENT '站点主键',
  `site_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '站点编码',
  `region_id` bigint NOT NULL COMMENT '所属区域',
  `site_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '站点名称',
  `site_type` enum('FLUX_TOWER','GROUND_PLOT','UAV_SAMPLE_AREA','SATELLITE_GRID','TRANSECT','LITERATURE_SITE','OTHER') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OTHER' COMMENT '站点类型',
  `ecosystem_type` enum('MANGROVE','SEAGRASS','SALT_MARSH','COASTAL_WETLAND','MIXED','OTHER') COLLATE utf8mb4_unicode_ci DEFAULT 'MANGROVE' COMMENT '生态系统类型',
  `latitude` decimal(10,7) DEFAULT NULL,
  `longitude` decimal(10,7) DEFAULT NULL,
  `elevation_m` decimal(10,3) DEFAULT NULL COMMENT '海拔或相对高程',
  `geom_wkt` text COLLATE utf8mb4_unicode_ci COMMENT '点、线、面几何 WKT',
  `managing_org` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '管理单位',
  `status` enum('PLANNED','ACTIVE','INACTIVE','ARCHIVED') COLLATE utf8mb4_unicode_ci DEFAULT 'PLANNED',
  `remark` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`site_id`),
  UNIQUE KEY `uk_site_code` (`site_code`),
  KEY `idx_site_region_type` (`region_id`,`site_type`),
  CONSTRAINT `fk_core_site_region` FOREIGN KEY (`region_id`) REFERENCES `t_region` (`region_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站点、样地、样区统一主数据表';

CREATE TABLE `t_data_source` (
  `source_id` bigint NOT NULL AUTO_INCREMENT COMMENT '数据来源主键',
  `source_code` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '来源编码',
  `source_category` enum('IN_SITU','PUBLIC_DATASET','LITERATURE','MODEL_OUTPUT','SIMULATED','DERIVED_PRODUCT','GOVERNMENT_DATA','UNKNOWN') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'UNKNOWN' COMMENT '来源类型',
  `source_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '数据来源名称',
  `publisher` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '发布机构',
  `author_or_team` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '作者或团队',
  `source_url` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '网页或下载地址',
  `access_date` date DEFAULT NULL COMMENT '网页访问日期',
  `download_date` date DEFAULT NULL COMMENT '下载日期',
  `license_text` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '许可证或使用限制',
  `citation_text` text COLLATE utf8mb4_unicode_ci COMMENT '引用格式',
  `version_label` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '来源版本',
  `is_simulated` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否模拟数据来源',
  `remark` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`source_id`),
  UNIQUE KEY `uk_source_code` (`source_code`),
  KEY `idx_source_category` (`source_category`,`is_simulated`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据来源、文献、公开数据和模拟数据登记表';

CREATE TABLE `t_data_version` (
  `version_id` bigint NOT NULL AUTO_INCREMENT COMMENT '版本主键',
  `source_id` bigint NOT NULL COMMENT '关联数据来源',
  `version_code` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '版本编码',
  `version_label` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '版本标签',
  `release_date` date DEFAULT NULL COMMENT '来源发布时间',
  `ingest_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
  `processing_level` enum('RAW','L0','L1','L2','L3','ANALYSIS_READY','DERIVED','AGGREGATED','DEMO') COLLATE utf8mb4_unicode_ci DEFAULT 'RAW' COMMENT '处理级别',
  `method_summary` text COLLATE utf8mb4_unicode_ci COMMENT '处理方法摘要',
  `is_current` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否当前版本',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`version_id`),
  UNIQUE KEY `uk_source_version` (`source_id`,`version_code`),
  KEY `idx_version_current` (`source_id`,`is_current`),
  CONSTRAINT `fk_core_version_source` FOREIGN KEY (`source_id`) REFERENCES `t_data_source` (`source_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据版本与处理级别记录表';

CREATE TABLE `t_quality_flag` (
  `qc_flag_id` bigint NOT NULL AUTO_INCREMENT COMMENT '质量标识主键',
  `flag_code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '质量标识编码',
  `flag_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '质量标识名称',
  `flag_level` tinyint NOT NULL DEFAULT '0' COMMENT '质量等级，0最好，数值越大质量越低',
  `description` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`qc_flag_id`),
  UNIQUE KEY `uk_qc_flag_code` (`flag_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='统一质量标识字典表';

CREATE TABLE `t_indicator_dictionary` (
  `indicator_id` bigint NOT NULL AUTO_INCREMENT COMMENT '指标主键',
  `indicator_code` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '指标编码，如 AGB、SOC、CO2_FLUX、NDVI',
  `indicator_name_cn` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '中文指标名',
  `indicator_name_en` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '英文指标名',
  `indicator_domain` enum('CARBON_STOCK','CARBON_FLUX','VEGETATION_STRUCTURE','SPECIES','REMOTE_SENSING','ECONOMIC_VALUE','MODEL_OUTPUT','ENVIRONMENT','OTHER') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OTHER' COMMENT '指标域',
  `default_unit` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '默认单位',
  `value_type` enum('NUMERIC','TEXT','JSON','GEOMETRY') COLLATE utf8mb4_unicode_ci DEFAULT 'NUMERIC' COMMENT '值类型',
  `description` text COLLATE utf8mb4_unicode_ci,
  `method_note` text COLLATE utf8mb4_unicode_ci COMMENT '计算或获取方法说明',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`indicator_id`),
  UNIQUE KEY `uk_indicator_code` (`indicator_code`),
  KEY `idx_indicator_domain` (`indicator_domain`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='统一指标字典表';

CREATE TABLE `t_data_asset_index` (
  `asset_id` bigint NOT NULL AUTO_INCREMENT COMMENT '全局资产ID',
  `asset_code` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '资产编码',
  `modal_type` enum('GROUND','SATELLITE','UAV','FLUX','MODEL','LITERATURE','VISUALIZATION','OTHER') COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '数据模态',
  `source_id` bigint DEFAULT NULL COMMENT '来源ID',
  `version_id` bigint DEFAULT NULL COMMENT '版本ID',
  `project_id` bigint DEFAULT NULL COMMENT '项目ID',
  `region_id` bigint DEFAULT NULL COMMENT '区域ID',
  `site_id` bigint DEFAULT NULL COMMENT '站点/样地ID',
  `source_category` enum('IN_SITU','PUBLIC_DATASET','LITERATURE','MODEL_OUTPUT','SIMULATED','DERIVED_PRODUCT','GOVERNMENT_DATA','UNKNOWN') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'UNKNOWN' COMMENT '来源类型冗余字段，便于检索',
  `data_stage` enum('RAW','CLEANED','DERIVED','AGGREGATED','DEMO') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'RAW' COMMENT '数据阶段',
  `storage_type` enum('MYSQL','LOCAL_FILE','NAS','OSS','URL','EXTERNAL','UNKNOWN') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'UNKNOWN' COMMENT '存储类型',
  `file_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件名',
  `storage_path` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件路径、URL或对象存储路径',
  `file_format` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件格式，如 tif、csv、xlsx、las',
  `mime_type` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'MIME 类型',
  `file_size_bytes` bigint DEFAULT NULL COMMENT '文件大小',
  `file_hash` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件哈希，建议 SHA256',
  `start_time` datetime DEFAULT NULL COMMENT '数据开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '数据结束时间',
  `spatial_wkt` longtext COLLATE utf8mb4_unicode_ci COMMENT '空间范围 WKT',
  `epsg_code` int DEFAULT NULL COMMENT '坐标系统 EPSG',
  `data_status` enum('ONLINE','ARCHIVED','MISSING','PLANNED') COLLATE utf8mb4_unicode_ci DEFAULT 'ONLINE' COMMENT '数据状态',
  `is_simulated` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否模拟数据',
  `access_scope` enum('PUBLIC','INTERNAL','RESTRICTED','CONFIDENTIAL') COLLATE utf8mb4_unicode_ci DEFAULT 'INTERNAL' COMMENT '共享范围',
  `remark` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`asset_id`),
  UNIQUE KEY `uk_asset_code` (`asset_code`),
  KEY `idx_asset_matrix` (`modal_type`,`source_category`,`data_stage`,`start_time`),
  KEY `idx_asset_region_time` (`region_id`,`start_time`,`end_time`),
  KEY `idx_asset_source` (`source_id`,`version_id`),
  CONSTRAINT `fk_core_asset_source` FOREIGN KEY (`source_id`) REFERENCES `t_data_source` (`source_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_core_asset_version` FOREIGN KEY (`version_id`) REFERENCES `t_data_version` (`version_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_core_asset_project` FOREIGN KEY (`project_id`) REFERENCES `t_project` (`project_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_core_asset_region` FOREIGN KEY (`region_id`) REFERENCES `t_region` (`region_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_core_asset_site` FOREIGN KEY (`site_id`) REFERENCES `t_site` (`site_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='统一数据资产索引表：MySQL 存元数据，冷数据存路径';

-- ============================================================
-- 2. Ground domain: plots, trees, soil, species, carbon summary
-- ============================================================

USE `hainan_blue_carbon_ground`;

DROP TABLE IF EXISTS `t_ground_plot_carbon_summary`;
DROP TABLE IF EXISTS `t_ground_plot_species_composition`;
DROP TABLE IF EXISTS `t_ground_soil_sample`;
DROP TABLE IF EXISTS `t_ground_tree_measurement`;
DROP TABLE IF EXISTS `t_ground_plot`;
DROP TABLE IF EXISTS `t_ground_survey_batch`;

CREATE TABLE `t_ground_survey_batch` (
  `batch_id` bigint NOT NULL AUTO_INCREMENT,
  `batch_code` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '调查批次编码',
  `project_id` bigint DEFAULT NULL,
  `region_id` bigint NOT NULL,
  `source_id` bigint DEFAULT NULL,
  `version_id` bigint DEFAULT NULL,
  `survey_start_date` date DEFAULT NULL,
  `survey_end_date` date DEFAULT NULL,
  `survey_team` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '调查团队',
  `method_standard` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '调查规范或方法标准',
  `weather_note` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `qc_flag_id` bigint DEFAULT NULL,
  `remark` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`batch_id`),
  UNIQUE KEY `uk_ground_batch_code` (`batch_code`),
  KEY `idx_ground_batch_region` (`region_id`,`survey_start_date`),
  CONSTRAINT `fk_ground_batch_project` FOREIGN KEY (`project_id`) REFERENCES `hainan_blue_carbon_core`.`t_project` (`project_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_ground_batch_region` FOREIGN KEY (`region_id`) REFERENCES `hainan_blue_carbon_core`.`t_region` (`region_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_ground_batch_source` FOREIGN KEY (`source_id`) REFERENCES `hainan_blue_carbon_core`.`t_data_source` (`source_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_ground_batch_version` FOREIGN KEY (`version_id`) REFERENCES `hainan_blue_carbon_core`.`t_data_version` (`version_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_ground_batch_qc` FOREIGN KEY (`qc_flag_id`) REFERENCES `hainan_blue_carbon_core`.`t_quality_flag` (`qc_flag_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='地面调查批次表';

CREATE TABLE `t_ground_plot` (
  `plot_id` bigint NOT NULL AUTO_INCREMENT,
  `plot_code` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '样方编码',
  `batch_id` bigint DEFAULT NULL,
  `region_id` bigint NOT NULL,
  `site_id` bigint DEFAULT NULL,
  `asset_id` bigint DEFAULT NULL,
  `plot_type` enum('PERMANENT','TEMPORARY','LITERATURE','SIMULATED','OTHER') COLLATE utf8mb4_unicode_ci DEFAULT 'TEMPORARY' COMMENT '样方类型',
  `ecosystem_type` enum('MANGROVE','SEAGRASS','SALT_MARSH','COASTAL_WETLAND','MIXED','OTHER') COLLATE utf8mb4_unicode_ci DEFAULT 'MANGROVE',
  `dominant_species` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '优势种',
  `latitude` decimal(10,7) DEFAULT NULL,
  `longitude` decimal(10,7) DEFAULT NULL,
  `plot_area_m2` decimal(12,4) DEFAULT NULL COMMENT '样方面积',
  `elevation_m` decimal(10,3) DEFAULT NULL,
  `tidal_zone` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '潮滩或生境分区',
  `geom_wkt` text COLLATE utf8mb4_unicode_ci,
  `qc_flag_id` bigint DEFAULT NULL,
  `remark` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`plot_id`),
  UNIQUE KEY `uk_ground_plot_code` (`plot_code`),
  KEY `idx_ground_plot_region` (`region_id`,`ecosystem_type`),
  KEY `idx_ground_plot_batch` (`batch_id`),
  CONSTRAINT `fk_ground_plot_batch` FOREIGN KEY (`batch_id`) REFERENCES `t_ground_survey_batch` (`batch_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_ground_plot_region` FOREIGN KEY (`region_id`) REFERENCES `hainan_blue_carbon_core`.`t_region` (`region_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_ground_plot_site` FOREIGN KEY (`site_id`) REFERENCES `hainan_blue_carbon_core`.`t_site` (`site_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_ground_plot_asset` FOREIGN KEY (`asset_id`) REFERENCES `hainan_blue_carbon_core`.`t_data_asset_index` (`asset_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_ground_plot_qc` FOREIGN KEY (`qc_flag_id`) REFERENCES `hainan_blue_carbon_core`.`t_quality_flag` (`qc_flag_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='地面调查样方表';

CREATE TABLE `t_ground_tree_measurement` (
  `tree_id` bigint NOT NULL AUTO_INCREMENT,
  `plot_id` bigint NOT NULL,
  `tree_code` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '单株编号',
  `measurement_date` date DEFAULT NULL,
  `scientific_name` varchar(160) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '物种拉丁名',
  `chinese_name` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '中文名',
  `dbh_cm` decimal(10,4) DEFAULT NULL COMMENT '胸径 cm',
  `height_m` decimal(10,4) DEFAULT NULL COMMENT '树高 m',
  `crown_width_m` decimal(10,4) DEFAULT NULL COMMENT '冠幅 m',
  `crown_length_ratio` decimal(8,4) DEFAULT NULL COMMENT '冠长占比',
  `alive_status` enum('ALIVE','DEAD','UNKNOWN') COLLATE utf8mb4_unicode_ci DEFAULT 'ALIVE' COMMENT '存活状态',
  `agb_kg` decimal(18,6) DEFAULT NULL COMMENT '单株地上生物量 kg',
  `bgb_kg` decimal(18,6) DEFAULT NULL COMMENT '单株地下生物量 kg',
  `carbon_stock_kg` decimal(18,6) DEFAULT NULL COMMENT '单株碳储量 kg C',
  `allometry_equation_id` bigint DEFAULT NULL COMMENT '异速生长方程ID，可关联 intl.t_allometry_equation',
  `qc_flag_id` bigint DEFAULT NULL,
  `remark` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`tree_id`),
  KEY `idx_ground_tree_plot` (`plot_id`),
  KEY `idx_ground_tree_species` (`scientific_name`),
  CONSTRAINT `fk_ground_tree_plot` FOREIGN KEY (`plot_id`) REFERENCES `t_ground_plot` (`plot_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_ground_tree_qc` FOREIGN KEY (`qc_flag_id`) REFERENCES `hainan_blue_carbon_core`.`t_quality_flag` (`qc_flag_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='样木结构与生物量测量表';

CREATE TABLE `t_ground_soil_sample` (
  `soil_sample_id` bigint NOT NULL AUTO_INCREMENT,
  `plot_id` bigint NOT NULL,
  `sample_code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '土壤样品编码',
  `sample_date` date DEFAULT NULL,
  `depth_top_cm` decimal(10,3) NOT NULL COMMENT '采样深度上限 cm',
  `depth_bottom_cm` decimal(10,3) NOT NULL COMMENT '采样深度下限 cm',
  `bulk_density_g_cm3` decimal(12,6) DEFAULT NULL COMMENT '容重 g/cm3',
  `soc_percent` decimal(12,6) DEFAULT NULL COMMENT '土壤有机碳含量 %',
  `soc_density_mg_ha` decimal(18,6) DEFAULT NULL COMMENT '土壤有机碳密度 Mg C/ha',
  `lab_method` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '实验方法',
  `qc_flag_id` bigint DEFAULT NULL,
  `remark` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`soil_sample_id`),
  UNIQUE KEY `uk_soil_sample_code` (`sample_code`),
  KEY `idx_soil_plot_depth` (`plot_id`,`depth_top_cm`,`depth_bottom_cm`),
  CONSTRAINT `fk_ground_soil_plot` FOREIGN KEY (`plot_id`) REFERENCES `t_ground_plot` (`plot_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_ground_soil_qc` FOREIGN KEY (`qc_flag_id`) REFERENCES `hainan_blue_carbon_core`.`t_quality_flag` (`qc_flag_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='土壤样品与土壤有机碳测量表';

CREATE TABLE `t_ground_plot_species_composition` (
  `composition_id` bigint NOT NULL AUTO_INCREMENT,
  `plot_id` bigint NOT NULL,
  `survey_date` date DEFAULT NULL,
  `scientific_name` varchar(160) COLLATE utf8mb4_unicode_ci NOT NULL,
  `chinese_name` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `individual_count` int DEFAULT NULL,
  `basal_area_m2_ha` decimal(18,6) DEFAULT NULL,
  `relative_abundance` decimal(10,6) DEFAULT NULL COMMENT '相对多度 0-1',
  `relative_dominance` decimal(10,6) DEFAULT NULL COMMENT '相对优势度 0-1',
  `importance_value` decimal(10,6) DEFAULT NULL COMMENT '重要值',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`composition_id`),
  UNIQUE KEY `uk_plot_species_date` (`plot_id`,`scientific_name`,`survey_date`),
  KEY `idx_plot_species` (`scientific_name`),
  CONSTRAINT `fk_ground_species_plot` FOREIGN KEY (`plot_id`) REFERENCES `t_ground_plot` (`plot_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='样方物种组成表';

CREATE TABLE `t_ground_plot_carbon_summary` (
  `summary_id` bigint NOT NULL AUTO_INCREMENT,
  `plot_id` bigint NOT NULL,
  `summary_date` date DEFAULT NULL,
  `agb_mg_ha` decimal(18,6) DEFAULT NULL COMMENT '地上生物量 Mg/ha',
  `bgb_mg_ha` decimal(18,6) DEFAULT NULL COMMENT '地下生物量 Mg/ha',
  `soc_mg_ha` decimal(18,6) DEFAULT NULL COMMENT '土壤有机碳 Mg C/ha',
  `deadwood_mg_ha` decimal(18,6) DEFAULT NULL COMMENT '死木碳库 Mg/ha',
  `litter_mg_ha` decimal(18,6) DEFAULT NULL COMMENT '枯落物碳库 Mg/ha',
  `total_carbon_mg_ha` decimal(18,6) DEFAULT NULL COMMENT '总碳储量 Mg C/ha',
  `method_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '汇总方法',
  `source_id` bigint DEFAULT NULL,
  `version_id` bigint DEFAULT NULL,
  `qc_flag_id` bigint DEFAULT NULL,
  `is_simulated` tinyint(1) DEFAULT '0',
  `remark` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`summary_id`),
  KEY `idx_plot_carbon` (`plot_id`,`summary_date`),
  CONSTRAINT `fk_ground_carbon_plot` FOREIGN KEY (`plot_id`) REFERENCES `t_ground_plot` (`plot_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_ground_carbon_source` FOREIGN KEY (`source_id`) REFERENCES `hainan_blue_carbon_core`.`t_data_source` (`source_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_ground_carbon_version` FOREIGN KEY (`version_id`) REFERENCES `hainan_blue_carbon_core`.`t_data_version` (`version_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_ground_carbon_qc` FOREIGN KEY (`qc_flag_id`) REFERENCES `hainan_blue_carbon_core`.`t_quality_flag` (`qc_flag_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='样方碳库汇总表';

-- ============================================================
-- 3. Flux domain: station, file, 30-minute observation, daily summary
-- ============================================================

USE `hainan_blue_carbon_flux`;

DROP TABLE IF EXISTS `t_flux_qc_event`;
DROP TABLE IF EXISTS `t_flux_daily_summary`;
DROP TABLE IF EXISTS `t_flux_30min_observation`;
DROP TABLE IF EXISTS `t_flux_file_asset`;
DROP TABLE IF EXISTS `t_flux_site`;

CREATE TABLE `t_flux_site` (
  `flux_site_id` bigint NOT NULL AUTO_INCREMENT,
  `site_id` bigint NOT NULL COMMENT '关联 core.t_site',
  `tower_code` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '通量塔编码',
  `tower_name` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `instrument_model` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '涡度相关仪器型号',
  `tower_height_m` decimal(10,3) DEFAULT NULL COMMENT '塔高',
  `canopy_height_m` decimal(10,3) DEFAULT NULL COMMENT '冠层高度',
  `flux_method` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT 'Eddy Covariance' COMMENT '通量观测方法',
  `data_frequency` varchar(60) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '原始频率，如 10Hz、30min',
  `operation_start_date` date DEFAULT NULL,
  `operation_end_date` date DEFAULT NULL,
  `status` enum('PLANNED','ACTIVE','MAINTENANCE','INACTIVE','ARCHIVED') COLLATE utf8mb4_unicode_ci DEFAULT 'PLANNED',
  `remark` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`flux_site_id`),
  UNIQUE KEY `uk_tower_code` (`tower_code`),
  KEY `idx_flux_site_core` (`site_id`),
  CONSTRAINT `fk_flux_site_core` FOREIGN KEY (`site_id`) REFERENCES `hainan_blue_carbon_core`.`t_site` (`site_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通量塔站点扩展信息表';

CREATE TABLE `t_flux_file_asset` (
  `file_id` bigint NOT NULL AUTO_INCREMENT,
  `flux_site_id` bigint NOT NULL,
  `asset_id` bigint DEFAULT NULL,
  `file_type` enum('FLUX_30MIN','TS_10HZ','METEO','QC_REPORT','OTHER') COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件类型',
  `file_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `storage_path` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `file_size_bytes` bigint DEFAULT NULL,
  `file_hash` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` enum('ONLINE','ARCHIVED','MISSING','PLANNED') COLLATE utf8mb4_unicode_ci DEFAULT 'ONLINE',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`file_id`),
  KEY `idx_flux_file_query` (`flux_site_id`,`file_type`,`start_time`),
  CONSTRAINT `fk_flux_file_site` FOREIGN KEY (`flux_site_id`) REFERENCES `t_flux_site` (`flux_site_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_flux_file_asset` FOREIGN KEY (`asset_id`) REFERENCES `hainan_blue_carbon_core`.`t_data_asset_index` (`asset_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通量原始文件与处理文件索引表';

CREATE TABLE `t_flux_30min_observation` (
  `obs_id` bigint NOT NULL AUTO_INCREMENT,
  `flux_site_id` bigint NOT NULL,
  `asset_id` bigint DEFAULT NULL,
  `monitor_time` datetime NOT NULL COMMENT '30分钟观测时间',
  `co2_flux` decimal(18,6) DEFAULT NULL COMMENT 'CO2 通量，单位按 unit_co2_flux',
  `ch4_flux` decimal(18,6) DEFAULT NULL COMMENT 'CH4 通量',
  `n2o_flux` decimal(18,6) DEFAULT NULL COMMENT 'N2O 通量',
  `ghg_flux` decimal(18,6) DEFAULT NULL COMMENT '综合温室气体通量',
  `gpp` decimal(18,6) DEFAULT NULL COMMENT 'GPP',
  `reco` decimal(18,6) DEFAULT NULL COMMENT '生态系统呼吸',
  `nep` decimal(18,6) DEFAULT NULL COMMENT 'NEP',
  `le_w_m2` decimal(18,6) DEFAULT NULL COMMENT '潜热通量 W/m2',
  `hs_w_m2` decimal(18,6) DEFAULT NULL COMMENT '显热通量 W/m2',
  `rn_w_m2` decimal(18,6) DEFAULT NULL COMMENT '净辐射 W/m2',
  `air_temp_c` decimal(18,6) DEFAULT NULL COMMENT '气温 °C',
  `air_pressure_kpa` decimal(18,6) DEFAULT NULL COMMENT '气压 kPa',
  `co2_concentration` decimal(18,6) DEFAULT NULL COMMENT 'CO2 浓度',
  `h2o_concentration` decimal(18,6) DEFAULT NULL COMMENT 'H2O 浓度',
  `wind_speed_m_s` decimal(18,6) DEFAULT NULL COMMENT '风速 m/s',
  `wind_direction_deg` decimal(18,6) DEFAULT NULL COMMENT '风向 °',
  `precipitation_mm` decimal(18,6) DEFAULT NULL COMMENT '降雨量 mm',
  `soil_heat_flux_w_m2` decimal(18,6) DEFAULT NULL COMMENT '土壤热通量 W/m2',
  `soil_temp_c` decimal(18,6) DEFAULT NULL COMMENT '土温 °C',
  `soil_water_content` decimal(18,6) DEFAULT NULL COMMENT '土壤含水量',
  `qc_flag_id` bigint DEFAULT NULL,
  `gap_filled` tinyint(1) DEFAULT '0' COMMENT '是否插补',
  `remark` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`obs_id`),
  UNIQUE KEY `uk_flux_site_time` (`flux_site_id`,`monitor_time`),
  KEY `idx_flux_time` (`monitor_time`),
  CONSTRAINT `fk_flux_obs_site` FOREIGN KEY (`flux_site_id`) REFERENCES `t_flux_site` (`flux_site_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_flux_obs_asset` FOREIGN KEY (`asset_id`) REFERENCES `hainan_blue_carbon_core`.`t_data_asset_index` (`asset_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_flux_obs_qc` FOREIGN KEY (`qc_flag_id`) REFERENCES `hainan_blue_carbon_core`.`t_quality_flag` (`qc_flag_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通量塔30分钟观测与气象辅助数据表';

CREATE TABLE `t_flux_daily_summary` (
  `daily_id` bigint NOT NULL AUTO_INCREMENT,
  `flux_site_id` bigint NOT NULL,
  `summary_date` date NOT NULL,
  `co2_flux_daily` decimal(18,6) DEFAULT NULL,
  `ch4_flux_daily` decimal(18,6) DEFAULT NULL,
  `n2o_flux_daily` decimal(18,6) DEFAULT NULL,
  `ghg_flux_daily` decimal(18,6) DEFAULT NULL,
  `gpp_daily` decimal(18,6) DEFAULT NULL,
  `reco_daily` decimal(18,6) DEFAULT NULL,
  `nep_daily` decimal(18,6) DEFAULT NULL,
  `valid_record_count` int DEFAULT NULL COMMENT '有效30分钟记录数',
  `gap_fill_ratio` decimal(10,6) DEFAULT NULL COMMENT '插补比例 0-1',
  `qc_flag_id` bigint DEFAULT NULL,
  `source_id` bigint DEFAULT NULL,
  `version_id` bigint DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`daily_id`),
  UNIQUE KEY `uk_flux_daily` (`flux_site_id`,`summary_date`),
  CONSTRAINT `fk_flux_daily_site` FOREIGN KEY (`flux_site_id`) REFERENCES `t_flux_site` (`flux_site_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_flux_daily_qc` FOREIGN KEY (`qc_flag_id`) REFERENCES `hainan_blue_carbon_core`.`t_quality_flag` (`qc_flag_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_flux_daily_source` FOREIGN KEY (`source_id`) REFERENCES `hainan_blue_carbon_core`.`t_data_source` (`source_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_flux_daily_version` FOREIGN KEY (`version_id`) REFERENCES `hainan_blue_carbon_core`.`t_data_version` (`version_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通量日尺度聚合表';

CREATE TABLE `t_flux_qc_event` (
  `event_id` bigint NOT NULL AUTO_INCREMENT,
  `flux_site_id` bigint NOT NULL,
  `event_start_time` datetime NOT NULL,
  `event_end_time` datetime DEFAULT NULL,
  `event_type` enum('MISSING','OUTLIER','INSTRUMENT_FAILURE','MAINTENANCE','GAP_FILL','CALIBRATION','OTHER') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OTHER',
  `affected_variables` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `handler` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`event_id`),
  KEY `idx_flux_qc_event` (`flux_site_id`,`event_start_time`),
  CONSTRAINT `fk_flux_qc_site` FOREIGN KEY (`flux_site_id`) REFERENCES `t_flux_site` (`flux_site_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通量数据质量事件记录表';

-- ============================================================
-- 4. Satellite domain: product metadata and regional/grid metrics
-- ============================================================

USE `hainan_blue_carbon_satellite`;

DROP TABLE IF EXISTS `t_satellite_grid_carbon`;
DROP TABLE IF EXISTS `t_satellite_mangrove_cover`;
DROP TABLE IF EXISTS `t_satellite_region_metric`;
DROP TABLE IF EXISTS `t_satellite_product`;

CREATE TABLE `t_satellite_product` (
  `product_id` bigint NOT NULL AUTO_INCREMENT,
  `asset_id` bigint DEFAULT NULL,
  `source_id` bigint DEFAULT NULL,
  `version_id` bigint DEFAULT NULL,
  `region_id` bigint DEFAULT NULL,
  `platform` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '卫星平台，如 Sentinel-2、Landsat',
  `sensor` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '传感器',
  `product_level` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '产品级别',
  `acquisition_time` datetime DEFAULT NULL COMMENT '获取时间',
  `cloud_cover_percent` decimal(8,4) DEFAULT NULL COMMENT '云量 %',
  `resolution_m` decimal(10,4) DEFAULT NULL COMMENT '空间分辨率 m',
  `band_list_json` json DEFAULT NULL COMMENT '波段列表',
  `epsg_code` int DEFAULT NULL,
  `bbox_wkt` text COLLATE utf8mb4_unicode_ci COMMENT '覆盖范围',
  `processing_level` enum('RAW','L1','L2','L3','ANALYSIS_READY','DERIVED') COLLATE utf8mb4_unicode_ci DEFAULT 'RAW',
  `remark` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`product_id`),
  KEY `idx_sat_product_platform_time` (`platform`,`acquisition_time`),
  KEY `idx_sat_product_region` (`region_id`),
  CONSTRAINT `fk_sat_product_asset` FOREIGN KEY (`asset_id`) REFERENCES `hainan_blue_carbon_core`.`t_data_asset_index` (`asset_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_sat_product_source` FOREIGN KEY (`source_id`) REFERENCES `hainan_blue_carbon_core`.`t_data_source` (`source_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_sat_product_version` FOREIGN KEY (`version_id`) REFERENCES `hainan_blue_carbon_core`.`t_data_version` (`version_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_sat_product_region` FOREIGN KEY (`region_id`) REFERENCES `hainan_blue_carbon_core`.`t_region` (`region_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='卫星遥感产品元数据表';

CREATE TABLE `t_satellite_region_metric` (
  `metric_id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint DEFAULT NULL,
  `region_id` bigint NOT NULL,
  `indicator_code` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `metric_year` int DEFAULT NULL,
  `metric_month` tinyint DEFAULT NULL,
  `metric_date` date DEFAULT NULL,
  `value` decimal(18,6) DEFAULT NULL,
  `unit` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `stat_method` enum('MEAN','MEDIAN','MIN','MAX','SUM','COUNT','AREA','PERCENTILE','OTHER') COLLATE utf8mb4_unicode_ci DEFAULT 'MEAN',
  `source_id` bigint DEFAULT NULL,
  `version_id` bigint DEFAULT NULL,
  `qc_flag_id` bigint DEFAULT NULL,
  `is_simulated` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`metric_id`),
  KEY `idx_sat_metric_region_indicator` (`region_id`,`indicator_code`,`metric_year`,`metric_month`),
  CONSTRAINT `fk_sat_metric_product` FOREIGN KEY (`product_id`) REFERENCES `t_satellite_product` (`product_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_sat_metric_region` FOREIGN KEY (`region_id`) REFERENCES `hainan_blue_carbon_core`.`t_region` (`region_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_sat_metric_indicator` FOREIGN KEY (`indicator_code`) REFERENCES `hainan_blue_carbon_core`.`t_indicator_dictionary` (`indicator_code`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_sat_metric_source` FOREIGN KEY (`source_id`) REFERENCES `hainan_blue_carbon_core`.`t_data_source` (`source_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_sat_metric_version` FOREIGN KEY (`version_id`) REFERENCES `hainan_blue_carbon_core`.`t_data_version` (`version_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_sat_metric_qc` FOREIGN KEY (`qc_flag_id`) REFERENCES `hainan_blue_carbon_core`.`t_quality_flag` (`qc_flag_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='卫星遥感区域统计指标表';

CREATE TABLE `t_satellite_mangrove_cover` (
  `cover_id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint DEFAULT NULL,
  `region_id` bigint NOT NULL,
  `metric_year` int NOT NULL,
  `mangrove_area_ha` decimal(18,6) DEFAULT NULL COMMENT '红树林面积 ha',
  `cover_ratio` decimal(10,6) DEFAULT NULL COMMENT '区域覆盖比例 0-1',
  `classification_method` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `overall_accuracy` decimal(10,6) DEFAULT NULL,
  `kappa` decimal(10,6) DEFAULT NULL,
  `qc_flag_id` bigint DEFAULT NULL,
  `source_id` bigint DEFAULT NULL,
  `version_id` bigint DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`cover_id`),
  UNIQUE KEY `uk_sat_cover_region_year_product` (`region_id`,`metric_year`,`product_id`),
  CONSTRAINT `fk_sat_cover_product` FOREIGN KEY (`product_id`) REFERENCES `t_satellite_product` (`product_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_sat_cover_region` FOREIGN KEY (`region_id`) REFERENCES `hainan_blue_carbon_core`.`t_region` (`region_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_sat_cover_qc` FOREIGN KEY (`qc_flag_id`) REFERENCES `hainan_blue_carbon_core`.`t_quality_flag` (`qc_flag_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_sat_cover_source` FOREIGN KEY (`source_id`) REFERENCES `hainan_blue_carbon_core`.`t_data_source` (`source_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_sat_cover_version` FOREIGN KEY (`version_id`) REFERENCES `hainan_blue_carbon_core`.`t_data_version` (`version_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='红树林覆盖面积与分类精度表';

CREATE TABLE `t_satellite_grid_carbon` (
  `grid_id` bigint NOT NULL AUTO_INCREMENT,
  `grid_code` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `product_id` bigint DEFAULT NULL,
  `region_id` bigint DEFAULT NULL,
  `grid_geom_wkt` text COLLATE utf8mb4_unicode_ci,
  `centroid_lat` decimal(10,7) DEFAULT NULL,
  `centroid_lon` decimal(10,7) DEFAULT NULL,
  `metric_date` date DEFAULT NULL,
  `agb_mg_ha` decimal(18,6) DEFAULT NULL COMMENT '地上生物量 Mg/ha',
  `agc_mg_ha` decimal(18,6) DEFAULT NULL COMMENT '地上碳储量 Mg C/ha',
  `canopy_height_m` decimal(18,6) DEFAULT NULL,
  `ndvi` decimal(10,6) DEFAULT NULL,
  `confidence` decimal(10,6) DEFAULT NULL,
  `source_id` bigint DEFAULT NULL,
  `version_id` bigint DEFAULT NULL,
  `is_simulated` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`grid_id`),
  UNIQUE KEY `uk_sat_grid_product_code` (`product_id`,`grid_code`),
  KEY `idx_sat_grid_region_date` (`region_id`,`metric_date`),
  CONSTRAINT `fk_sat_grid_product` FOREIGN KEY (`product_id`) REFERENCES `t_satellite_product` (`product_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_sat_grid_region` FOREIGN KEY (`region_id`) REFERENCES `hainan_blue_carbon_core`.`t_region` (`region_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_sat_grid_source` FOREIGN KEY (`source_id`) REFERENCES `hainan_blue_carbon_core`.`t_data_source` (`source_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_sat_grid_version` FOREIGN KEY (`version_id`) REFERENCES `hainan_blue_carbon_core`.`t_data_version` (`version_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='卫星遥感网格尺度碳储与冠层高度结果表';

-- ============================================================
-- 5. UAV domain: mission, products, sample metrics, species, carbon inversion
-- ============================================================

USE `hainan_blue_carbon_uav`;

DROP TABLE IF EXISTS `t_uav_carbon_inversion_result`;
DROP TABLE IF EXISTS `t_uav_species_classification`;
DROP TABLE IF EXISTS `t_uav_sample_area_metric`;
DROP TABLE IF EXISTS `t_uav_product`;
DROP TABLE IF EXISTS `t_uav_flight_mission`;

CREATE TABLE `t_uav_flight_mission` (
  `mission_id` bigint NOT NULL AUTO_INCREMENT,
  `mission_code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '航飞任务编码',
  `project_id` bigint DEFAULT NULL,
  `region_id` bigint NOT NULL,
  `mission_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `flight_date` date DEFAULT NULL,
  `uav_model` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sensor_model` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `flight_height_m` decimal(10,3) DEFAULT NULL,
  `front_overlap_percent` decimal(8,4) DEFAULT NULL,
  `side_overlap_percent` decimal(8,4) DEFAULT NULL,
  `weather_note` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `operator_name` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `positioning_mode` enum('NONE','GCP','RTK','PPK','GCP_RTK','OTHER') COLLATE utf8mb4_unicode_ci DEFAULT 'NONE',
  `epsg_code` int DEFAULT NULL,
  `bbox_wkt` text COLLATE utf8mb4_unicode_ci,
  `status` enum('PLANNED','FLOWN','PROCESSED','FAILED','ARCHIVED') COLLATE utf8mb4_unicode_ci DEFAULT 'PLANNED',
  `remark` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`mission_id`),
  UNIQUE KEY `uk_uav_mission_code` (`mission_code`),
  KEY `idx_uav_mission_region_date` (`region_id`,`flight_date`),
  CONSTRAINT `fk_uav_mission_project` FOREIGN KEY (`project_id`) REFERENCES `hainan_blue_carbon_core`.`t_project` (`project_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_uav_mission_region` FOREIGN KEY (`region_id`) REFERENCES `hainan_blue_carbon_core`.`t_region` (`region_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='无人机航飞任务表';

CREATE TABLE `t_uav_product` (
  `product_id` bigint NOT NULL AUTO_INCREMENT,
  `mission_id` bigint NOT NULL,
  `asset_id` bigint DEFAULT NULL,
  `data_type` enum('DOM','DSM','DEM','CHM','POINT_CLOUD','RGB_IMAGE','MULTISPECTRAL','HYPERSPECTRAL','CLASSIFICATION','CARBON_MAP','OTHER') COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '无人机产品类型',
  `product_level` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '处理级别',
  `resolution_m` decimal(10,4) DEFAULT NULL,
  `band_count` int DEFAULT NULL,
  `epsg_code` int DEFAULT NULL,
  `bbox_wkt` text COLLATE utf8mb4_unicode_ci,
  `mean_ndvi` decimal(10,6) DEFAULT NULL,
  `vegetation_coverage` decimal(10,6) DEFAULT NULL COMMENT '植被覆盖度 0-1',
  `processing_software` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`product_id`),
  KEY `idx_uav_product_mission_type` (`mission_id`,`data_type`),
  CONSTRAINT `fk_uav_product_mission` FOREIGN KEY (`mission_id`) REFERENCES `t_uav_flight_mission` (`mission_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_uav_product_asset` FOREIGN KEY (`asset_id`) REFERENCES `hainan_blue_carbon_core`.`t_data_asset_index` (`asset_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='无人机产品元数据表';

CREATE TABLE `t_uav_sample_area_metric` (
  `metric_id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint DEFAULT NULL,
  `region_id` bigint NOT NULL,
  `site_id` bigint DEFAULT NULL,
  `indicator_code` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `metric_date` date DEFAULT NULL,
  `value` decimal(18,6) DEFAULT NULL,
  `unit` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `stat_method` enum('MEAN','MEDIAN','MIN','MAX','SUM','COUNT','AREA','PERCENTILE','OTHER') COLLATE utf8mb4_unicode_ci DEFAULT 'MEAN',
  `source_id` bigint DEFAULT NULL,
  `version_id` bigint DEFAULT NULL,
  `qc_flag_id` bigint DEFAULT NULL,
  `is_simulated` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`metric_id`),
  KEY `idx_uav_metric_region_indicator` (`region_id`,`indicator_code`,`metric_date`),
  CONSTRAINT `fk_uav_metric_product` FOREIGN KEY (`product_id`) REFERENCES `t_uav_product` (`product_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_uav_metric_region` FOREIGN KEY (`region_id`) REFERENCES `hainan_blue_carbon_core`.`t_region` (`region_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_uav_metric_site` FOREIGN KEY (`site_id`) REFERENCES `hainan_blue_carbon_core`.`t_site` (`site_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_uav_metric_indicator` FOREIGN KEY (`indicator_code`) REFERENCES `hainan_blue_carbon_core`.`t_indicator_dictionary` (`indicator_code`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_uav_metric_source` FOREIGN KEY (`source_id`) REFERENCES `hainan_blue_carbon_core`.`t_data_source` (`source_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_uav_metric_version` FOREIGN KEY (`version_id`) REFERENCES `hainan_blue_carbon_core`.`t_data_version` (`version_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_uav_metric_qc` FOREIGN KEY (`qc_flag_id`) REFERENCES `hainan_blue_carbon_core`.`t_quality_flag` (`qc_flag_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='无人机样区统计指标表';

CREATE TABLE `t_uav_species_classification` (
  `class_id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `region_id` bigint DEFAULT NULL,
  `scientific_name` varchar(160) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `chinese_name` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `class_area_ha` decimal(18,6) DEFAULT NULL,
  `tree_count` int DEFAULT NULL,
  `confidence_mean` decimal(10,6) DEFAULT NULL,
  `classification_model` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `accuracy_note` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`class_id`),
  KEY `idx_uav_species_product` (`product_id`,`scientific_name`),
  CONSTRAINT `fk_uav_species_product` FOREIGN KEY (`product_id`) REFERENCES `t_uav_product` (`product_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_uav_species_region` FOREIGN KEY (`region_id`) REFERENCES `hainan_blue_carbon_core`.`t_region` (`region_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='无人机物种识别与分类结果表';

CREATE TABLE `t_uav_carbon_inversion_result` (
  `result_id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint DEFAULT NULL,
  `region_id` bigint NOT NULL,
  `site_id` bigint DEFAULT NULL,
  `metric_date` date DEFAULT NULL,
  `agb_mg_ha` decimal(18,6) DEFAULT NULL,
  `agc_mg_ha` decimal(18,6) DEFAULT NULL,
  `carbon_stock_mg_ha` decimal(18,6) DEFAULT NULL,
  `canopy_height_mean_m` decimal(18,6) DEFAULT NULL,
  `model_run_id` bigint DEFAULT NULL COMMENT '可关联 model.t_model_run',
  `uncertainty` decimal(18,6) DEFAULT NULL,
  `source_id` bigint DEFAULT NULL,
  `version_id` bigint DEFAULT NULL,
  `qc_flag_id` bigint DEFAULT NULL,
  `is_simulated` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`result_id`),
  KEY `idx_uav_carbon_region_date` (`region_id`,`metric_date`),
  CONSTRAINT `fk_uav_carbon_product` FOREIGN KEY (`product_id`) REFERENCES `t_uav_product` (`product_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_uav_carbon_region` FOREIGN KEY (`region_id`) REFERENCES `hainan_blue_carbon_core`.`t_region` (`region_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_uav_carbon_site` FOREIGN KEY (`site_id`) REFERENCES `hainan_blue_carbon_core`.`t_site` (`site_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_uav_carbon_source` FOREIGN KEY (`source_id`) REFERENCES `hainan_blue_carbon_core`.`t_data_source` (`source_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_uav_carbon_version` FOREIGN KEY (`version_id`) REFERENCES `hainan_blue_carbon_core`.`t_data_version` (`version_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_uav_carbon_qc` FOREIGN KEY (`qc_flag_id`) REFERENCES `hainan_blue_carbon_core`.`t_quality_flag` (`qc_flag_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='无人机碳储量反演结果表';

-- ============================================================
-- 6. Model domain: CatBoost, SHAP, model runs and prediction records
-- ============================================================

USE `hainan_blue_carbon_model`;

DROP TABLE IF EXISTS `t_model_explanation_asset`;
DROP TABLE IF EXISTS `t_model_prediction`;
DROP TABLE IF EXISTS `t_model_run`;
DROP TABLE IF EXISTS `t_model_registry`;

CREATE TABLE `t_model_registry` (
  `model_id` bigint NOT NULL AUTO_INCREMENT,
  `model_code` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模型编码',
  `model_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `model_type` enum('CATBOOST','XGBOOST','LIGHTGBM','RANDOM_FOREST','LINEAR','GNN','LLM','OTHER') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OTHER',
  `target_indicator_code` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '目标指标编码',
  `model_version` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'v1',
  `training_dataset_asset_id` bigint DEFAULT NULL,
  `feature_schema_json` json DEFAULT NULL COMMENT '输入特征结构',
  `metric_json` json DEFAULT NULL COMMENT 'RMSE、MAE、R2 等评估指标',
  `model_file_asset_id` bigint DEFAULT NULL COMMENT '模型文件资产',
  `description` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`model_id`),
  UNIQUE KEY `uk_model_code_version` (`model_code`,`model_version`),
  CONSTRAINT `fk_model_training_asset` FOREIGN KEY (`training_dataset_asset_id`) REFERENCES `hainan_blue_carbon_core`.`t_data_asset_index` (`asset_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_model_file_asset` FOREIGN KEY (`model_file_asset_id`) REFERENCES `hainan_blue_carbon_core`.`t_data_asset_index` (`asset_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_model_target_indicator` FOREIGN KEY (`target_indicator_code`) REFERENCES `hainan_blue_carbon_core`.`t_indicator_dictionary` (`indicator_code`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型注册表';

CREATE TABLE `t_model_run` (
  `run_id` bigint NOT NULL AUTO_INCREMENT,
  `model_id` bigint NOT NULL,
  `run_code` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '运行编码',
  `project_id` bigint DEFAULT NULL,
  `region_id` bigint DEFAULT NULL,
  `site_id` bigint DEFAULT NULL,
  `run_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `input_asset_id` bigint DEFAULT NULL,
  `output_asset_id` bigint DEFAULT NULL,
  `parameter_json` json DEFAULT NULL,
  `run_status` enum('SUCCESS','FAILED','RUNNING','PLANNED') COLLATE utf8mb4_unicode_ci DEFAULT 'SUCCESS',
  `error_message` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`run_id`),
  UNIQUE KEY `uk_model_run_code` (`run_code`),
  KEY `idx_model_run_model_time` (`model_id`,`run_time`),
  CONSTRAINT `fk_model_run_model` FOREIGN KEY (`model_id`) REFERENCES `t_model_registry` (`model_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_model_run_project` FOREIGN KEY (`project_id`) REFERENCES `hainan_blue_carbon_core`.`t_project` (`project_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_model_run_region` FOREIGN KEY (`region_id`) REFERENCES `hainan_blue_carbon_core`.`t_region` (`region_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_model_run_site` FOREIGN KEY (`site_id`) REFERENCES `hainan_blue_carbon_core`.`t_site` (`site_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_model_run_input_asset` FOREIGN KEY (`input_asset_id`) REFERENCES `hainan_blue_carbon_core`.`t_data_asset_index` (`asset_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_model_run_output_asset` FOREIGN KEY (`output_asset_id`) REFERENCES `hainan_blue_carbon_core`.`t_data_asset_index` (`asset_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型运行记录表';

CREATE TABLE `t_model_prediction` (
  `prediction_id` bigint NOT NULL AUTO_INCREMENT,
  `run_id` bigint NOT NULL,
  `region_id` bigint DEFAULT NULL,
  `site_id` bigint DEFAULT NULL,
  `plot_id` bigint DEFAULT NULL COMMENT '地面样方ID，可关联 ground.t_ground_plot',
  `prediction_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `target_indicator_code` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `predicted_value` decimal(18,6) NOT NULL,
  `unit` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `input_feature_json` json DEFAULT NULL,
  `uncertainty` decimal(18,6) DEFAULT NULL,
  `is_simulated` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`prediction_id`),
  KEY `idx_model_prediction_run` (`run_id`),
  KEY `idx_model_prediction_region_indicator` (`region_id`,`target_indicator_code`,`prediction_time`),
  CONSTRAINT `fk_model_pred_run` FOREIGN KEY (`run_id`) REFERENCES `t_model_run` (`run_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_model_pred_region` FOREIGN KEY (`region_id`) REFERENCES `hainan_blue_carbon_core`.`t_region` (`region_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_model_pred_site` FOREIGN KEY (`site_id`) REFERENCES `hainan_blue_carbon_core`.`t_site` (`site_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_model_pred_indicator` FOREIGN KEY (`target_indicator_code`) REFERENCES `hainan_blue_carbon_core`.`t_indicator_dictionary` (`indicator_code`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型预测结果表';

CREATE TABLE `t_model_explanation_asset` (
  `explanation_id` bigint NOT NULL AUTO_INCREMENT,
  `run_id` bigint NOT NULL,
  `prediction_id` bigint DEFAULT NULL,
  `explanation_type` enum('SHAP_BAR','SHAP_BEESWARM','SHAP_FORCE','PDP','SENSITIVITY','RESIDUAL','SCATTER','TEXT','OTHER') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OTHER',
  `asset_id` bigint DEFAULT NULL,
  `explanation_json` json DEFAULT NULL,
  `summary_text` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`explanation_id`),
  KEY `idx_model_explain_run` (`run_id`,`explanation_type`),
  CONSTRAINT `fk_model_explain_run` FOREIGN KEY (`run_id`) REFERENCES `t_model_run` (`run_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_model_explain_prediction` FOREIGN KEY (`prediction_id`) REFERENCES `t_model_prediction` (`prediction_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_model_explain_asset` FOREIGN KEY (`asset_id`) REFERENCES `hainan_blue_carbon_core`.`t_data_asset_index` (`asset_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型解释图件、文本与JSON结果表';

-- ============================================================
-- 7. International/public/literature domain
-- ============================================================

USE `hainan_blue_carbon_intl`;

DROP TABLE IF EXISTS `t_literature_carbon_record`;
DROP TABLE IF EXISTS `t_allometry_equation`;
DROP TABLE IF EXISTS `t_external_observation`;
DROP TABLE IF EXISTS `t_literature_reference`;
DROP TABLE IF EXISTS `t_external_dataset_registry`;

CREATE TABLE `t_external_dataset_registry` (
  `dataset_id` bigint NOT NULL AUTO_INCREMENT,
  `source_id` bigint DEFAULT NULL,
  `dataset_code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `dataset_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `dataset_scope` enum('GLOBAL','CHINA','HAINAN','REGIONAL','OTHER') COLLATE utf8mb4_unicode_ci DEFAULT 'GLOBAL',
  `data_domain` enum('BIOMASS','ALLOMETRY','REMOTE_SENSING','GPP','SOIL_CARBON','SPECIES','TRANSECT','OTHER') COLLATE utf8mb4_unicode_ci DEFAULT 'OTHER',
  `license_text` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `citation_text` text COLLATE utf8mb4_unicode_ci,
  `download_url` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `download_date` date DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`dataset_id`),
  UNIQUE KEY `uk_ext_dataset_code` (`dataset_code`),
  CONSTRAINT `fk_intl_dataset_source` FOREIGN KEY (`source_id`) REFERENCES `hainan_blue_carbon_core`.`t_data_source` (`source_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='外部公开数据集登记表';

CREATE TABLE `t_literature_reference` (
  `reference_id` bigint NOT NULL AUTO_INCREMENT,
  `source_id` bigint DEFAULT NULL,
  `doi` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `title` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `authors` text COLLATE utf8mb4_unicode_ci,
  `publication_year` int DEFAULT NULL,
  `journal_or_source` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `url` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `study_area` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ecosystem_type` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `citation_text` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`reference_id`),
  KEY `idx_lit_year` (`publication_year`),
  KEY `idx_lit_doi` (`doi`),
  CONSTRAINT `fk_intl_ref_source` FOREIGN KEY (`source_id`) REFERENCES `hainan_blue_carbon_core`.`t_data_source` (`source_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文献引用表';

CREATE TABLE `t_external_observation` (
  `obs_id` bigint NOT NULL AUTO_INCREMENT,
  `dataset_id` bigint DEFAULT NULL,
  `reference_id` bigint DEFAULT NULL,
  `source_id` bigint DEFAULT NULL,
  `site_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `country` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `province` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `latitude` decimal(10,7) DEFAULT NULL,
  `longitude` decimal(10,7) DEFAULT NULL,
  `ecosystem_type` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `scientific_name` varchar(160) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `indicator_code` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `indicator_value` decimal(18,6) DEFAULT NULL,
  `unit` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `observation_year` int DEFAULT NULL,
  `raw_json` json DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`obs_id`),
  KEY `idx_ext_obs_indicator` (`indicator_code`,`ecosystem_type`,`observation_year`),
  CONSTRAINT `fk_intl_obs_dataset` FOREIGN KEY (`dataset_id`) REFERENCES `t_external_dataset_registry` (`dataset_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_intl_obs_ref` FOREIGN KEY (`reference_id`) REFERENCES `t_literature_reference` (`reference_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_intl_obs_source` FOREIGN KEY (`source_id`) REFERENCES `hainan_blue_carbon_core`.`t_data_source` (`source_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_intl_obs_indicator` FOREIGN KEY (`indicator_code`) REFERENCES `hainan_blue_carbon_core`.`t_indicator_dictionary` (`indicator_code`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='外部公开数据与文献观测记录表';

CREATE TABLE `t_allometry_equation` (
  `equation_id` bigint NOT NULL AUTO_INCREMENT,
  `dataset_id` bigint DEFAULT NULL,
  `reference_id` bigint DEFAULT NULL,
  `country` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `province` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `scientific_name` varchar(160) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `equation_form` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `component` enum('TOTAL','ABOVEGROUND','BELOWGROUND','STEM','BRANCH','LEAF','ROOT','OTHER') COLLATE utf8mb4_unicode_ci DEFAULT 'TOTAL',
  `coefficient_a` double DEFAULT NULL,
  `coefficient_b` double DEFAULT NULL,
  `coefficient_c` double DEFAULT NULL,
  `coefficient_d` double DEFAULT NULL,
  `r_squared` double DEFAULT NULL,
  `sample_size` int DEFAULT NULL,
  `dbh_min_cm` double DEFAULT NULL,
  `dbh_max_cm` double DEFAULT NULL,
  `height_min_m` double DEFAULT NULL,
  `height_max_m` double DEFAULT NULL,
  `unit_note` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`equation_id`),
  KEY `idx_allometry_species_area` (`province`,`scientific_name`),
  CONSTRAINT `fk_intl_eq_dataset` FOREIGN KEY (`dataset_id`) REFERENCES `t_external_dataset_registry` (`dataset_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_intl_eq_ref` FOREIGN KEY (`reference_id`) REFERENCES `t_literature_reference` (`reference_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='异速生长方程表';

CREATE TABLE `t_literature_carbon_record` (
  `record_id` bigint NOT NULL AUTO_INCREMENT,
  `reference_id` bigint DEFAULT NULL,
  `region_id` bigint DEFAULT NULL,
  `site_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `latitude` decimal(10,7) DEFAULT NULL,
  `longitude` decimal(10,7) DEFAULT NULL,
  `ecosystem_type` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `carbon_pool` enum('AGB','BGB','SOC','DEADWOOD','LITTER','TOTAL','OTHER') COLLATE utf8mb4_unicode_ci DEFAULT 'TOTAL',
  `value_mg_ha` decimal(18,6) DEFAULT NULL,
  `depth_top_cm` decimal(10,3) DEFAULT NULL,
  `depth_bottom_cm` decimal(10,3) DEFAULT NULL,
  `method_note` text COLLATE utf8mb4_unicode_ci,
  `source_id` bigint DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`record_id`),
  KEY `idx_lit_carbon_pool` (`carbon_pool`,`ecosystem_type`),
  CONSTRAINT `fk_intl_carbon_ref` FOREIGN KEY (`reference_id`) REFERENCES `t_literature_reference` (`reference_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_intl_carbon_region` FOREIGN KEY (`region_id`) REFERENCES `hainan_blue_carbon_core`.`t_region` (`region_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_intl_carbon_source` FOREIGN KEY (`source_id`) REFERENCES `hainan_blue_carbon_core`.`t_data_source` (`source_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文献碳储量原始记录表';

-- ============================================================
-- 8. Local/display domain: Wenchang demo and legacy visualization adapter
-- ============================================================

USE `hainan_blue_carbon_local`;

DROP VIEW IF EXISTS `v_legacy_visual_indicator_data`;
DROP VIEW IF EXISTS `v_legacy_region_species_composition`;
DROP VIEW IF EXISTS `v_legacy_region_flux_composition`;
DROP VIEW IF EXISTS `v_legacy_region_yearly_carbon`;
DROP TABLE IF EXISTS `t_local_demo_data_note`;
DROP TABLE IF EXISTS `t_local_visualization_metric`;
DROP TABLE IF EXISTS `t_local_region_yearly_metric`;
DROP TABLE IF EXISTS `t_wenchang_demo_region_profile`;

CREATE TABLE `t_wenchang_demo_region_profile` (
  `profile_id` bigint NOT NULL AUTO_INCREMENT,
  `region_id` bigint NOT NULL,
  `profile_title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ecosystem_description` text COLLATE utf8mb4_unicode_ci,
  `dominant_species` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `management_positioning` text COLLATE utf8mb4_unicode_ci COMMENT '管理展示口径',
  `research_positioning` text COLLATE utf8mb4_unicode_ci COMMENT '科研展示口径',
  `education_positioning` text COLLATE utf8mb4_unicode_ci COMMENT '研学与公众展示口径',
  `source_id` bigint DEFAULT NULL,
  `is_current` tinyint(1) DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`profile_id`),
  KEY `idx_wenchang_profile_region` (`region_id`,`is_current`),
  CONSTRAINT `fk_local_profile_region` FOREIGN KEY (`region_id`) REFERENCES `hainan_blue_carbon_core`.`t_region` (`region_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_local_profile_source` FOREIGN KEY (`source_id`) REFERENCES `hainan_blue_carbon_core`.`t_data_source` (`source_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文昌示范区专题简介与展示口径表';

CREATE TABLE `t_local_region_yearly_metric` (
  `metric_id` bigint NOT NULL AUTO_INCREMENT,
  `region_id` bigint NOT NULL,
  `metric_year` int NOT NULL,
  `indicator_code` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `value` decimal(18,6) DEFAULT NULL,
  `unit` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `metric_source_domain` enum('GROUND','FLUX','SATELLITE','UAV','MODEL','LITERATURE','SIMULATED','MANUAL') COLLATE utf8mb4_unicode_ci DEFAULT 'MANUAL' COMMENT '指标来源域',
  `source_id` bigint DEFAULT NULL,
  `version_id` bigint DEFAULT NULL,
  `qc_flag_id` bigint DEFAULT NULL,
  `is_simulated` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`metric_id`),
  UNIQUE KEY `uk_local_year_metric` (`region_id`,`metric_year`,`indicator_code`,`metric_source_domain`),
  KEY `idx_local_metric_indicator` (`indicator_code`,`metric_year`),
  CONSTRAINT `fk_local_metric_region` FOREIGN KEY (`region_id`) REFERENCES `hainan_blue_carbon_core`.`t_region` (`region_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_local_metric_indicator` FOREIGN KEY (`indicator_code`) REFERENCES `hainan_blue_carbon_core`.`t_indicator_dictionary` (`indicator_code`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_local_metric_source` FOREIGN KEY (`source_id`) REFERENCES `hainan_blue_carbon_core`.`t_data_source` (`source_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_local_metric_version` FOREIGN KEY (`version_id`) REFERENCES `hainan_blue_carbon_core`.`t_data_version` (`version_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_local_metric_qc` FOREIGN KEY (`qc_flag_id`) REFERENCES `hainan_blue_carbon_core`.`t_quality_flag` (`qc_flag_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='本地年度区域聚合指标表，供专题展示和旧页面适配';

CREATE TABLE `t_local_visualization_metric` (
  `visual_metric_id` bigint NOT NULL AUTO_INCREMENT,
  `region_id` bigint NOT NULL,
  `indicator_code` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `display_year` int DEFAULT NULL,
  `display_group` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图表分组，如 carbon、flux、species、economy',
  `display_label` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '前端显示标签',
  `value_numeric` decimal(18,6) DEFAULT NULL,
  `value_text` text COLLATE utf8mb4_unicode_ci,
  `value_json` json DEFAULT NULL,
  `unit` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sort_order` int DEFAULT '0',
  `source_id` bigint DEFAULT NULL,
  `is_simulated` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`visual_metric_id`),
  KEY `idx_visual_metric_region_group` (`region_id`,`display_group`,`display_year`),
  CONSTRAINT `fk_visual_metric_region` FOREIGN KEY (`region_id`) REFERENCES `hainan_blue_carbon_core`.`t_region` (`region_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_visual_metric_indicator` FOREIGN KEY (`indicator_code`) REFERENCES `hainan_blue_carbon_core`.`t_indicator_dictionary` (`indicator_code`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_visual_metric_source` FOREIGN KEY (`source_id`) REFERENCES `hainan_blue_carbon_core`.`t_data_source` (`source_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='本地可视化指标表，承接 V1.0 图表字段';

CREATE TABLE `t_local_demo_data_note` (
  `note_id` bigint NOT NULL AUTO_INCREMENT,
  `region_id` bigint DEFAULT NULL,
  `table_name` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `data_scope` enum('REAL','PUBLIC_DATASET','LITERATURE','SIMULATED','DERIVED','MIXED') COLLATE utf8mb4_unicode_ci DEFAULT 'SIMULATED',
  `note_text` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`note_id`),
  KEY `idx_demo_note_region` (`region_id`,`table_name`),
  CONSTRAINT `fk_demo_note_region` FOREIGN KEY (`region_id`) REFERENCES `hainan_blue_carbon_core`.`t_region` (`region_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模拟数据、公开数据和真实数据的展示说明表';

CREATE OR REPLACE VIEW `v_legacy_region_yearly_carbon` AS
SELECT
  r.region_name AS region_name,
  m.metric_year AS `year`,
  MAX(CASE WHEN m.indicator_code IN ('TOTAL_CARBON','CARBON_STORAGE') THEN m.value END) AS carbon_storage,
  MAX(CASE WHEN m.indicator_code IN ('CO2_FLUX','GHG_FLUX','CARBON_FLUX') THEN m.value END) AS carbon_flux
FROM `t_local_region_yearly_metric` m
JOIN `hainan_blue_carbon_core`.`t_region` r ON r.region_id = m.region_id
GROUP BY r.region_name, m.metric_year;

CREATE OR REPLACE VIEW `v_legacy_region_flux_composition` AS
SELECT
  r.region_name AS region_name,
  m.metric_year AS `year`,
  MAX(CASE WHEN m.indicator_code = 'CO2_FLUX' THEN m.value END) AS co2,
  MAX(CASE WHEN m.indicator_code = 'CH4_FLUX' THEN m.value END) AS ch4,
  MAX(CASE WHEN m.indicator_code = 'N2O_FLUX' THEN m.value END) AS n2o,
  MAX(CASE WHEN m.indicator_code = 'GHG_FLUX' THEN m.value END) AS ghg
FROM `t_local_region_yearly_metric` m
JOIN `hainan_blue_carbon_core`.`t_region` r ON r.region_id = m.region_id
GROUP BY r.region_name, m.metric_year;

CREATE OR REPLACE VIEW `v_legacy_region_species_composition` AS
SELECT
  r.region_name AS region_name,
  v.display_label AS species,
  CAST(v.value_numeric AS DOUBLE) AS value
FROM `t_local_visualization_metric` v
JOIN `hainan_blue_carbon_core`.`t_region` r ON r.region_id = v.region_id
WHERE v.display_group = 'species';

CREATE OR REPLACE VIEW `v_legacy_visual_indicator_data` AS
SELECT
  v.visual_metric_id AS id,
  r.region_name AS region_name,
  v.indicator_code AS indicator_key,
  CAST(v.value_numeric AS DOUBLE) AS value,
  v.unit AS unit,
  v.value_text AS description,
  v.display_year AS `year`,
  v.created_at AS created_at,
  v.updated_at AS updated_at
FROM `t_local_visualization_metric` v
JOIN `hainan_blue_carbon_core`.`t_region` r ON r.region_id = v.region_id;

-- ============================================================
-- 9. Minimal dictionaries. These are metadata, not real observations.
-- ============================================================

USE `hainan_blue_carbon_core`;

INSERT INTO `t_quality_flag` (`flag_code`,`flag_name`,`flag_level`,`description`) VALUES
('Q0','高质量',0,'通过基础质量控制，可直接用于展示或分析'),
('Q1','可用',1,'存在轻微缺测或不确定性，适合一般分析'),
('Q2','谨慎使用',2,'存在明显插补、异常或来源不完整'),
('Q3','不可用于正式分析',3,'仅用于演示、占位或待复核')
ON DUPLICATE KEY UPDATE `flag_name`=VALUES(`flag_name`), `flag_level`=VALUES(`flag_level`), `description`=VALUES(`description`);

INSERT INTO `t_indicator_dictionary` (`indicator_code`,`indicator_name_cn`,`indicator_name_en`,`indicator_domain`,`default_unit`,`description`) VALUES
('AGB','地上生物量','Aboveground Biomass','CARBON_STOCK','Mg/ha','地上活生物量或地上生物量密度'),
('BGB','地下生物量','Belowground Biomass','CARBON_STOCK','Mg/ha','地下根系生物量或地下生物量密度'),
('SOC','土壤有机碳','Soil Organic Carbon','CARBON_STOCK','Mg C/ha','土壤有机碳密度'),
('TOTAL_CARBON','总碳储量','Total Carbon Stock','CARBON_STOCK','Mg C/ha','总碳储量或单位面积总碳储量'),
('CARBON_STORAGE','碳储量','Carbon Storage','CARBON_STOCK','t/ha','前端旧页面常用碳储量指标'),
('CO2_FLUX','CO2通量','CO2 Flux','CARBON_FLUX','mg/m2/s','二氧化碳通量'),
('CH4_FLUX','CH4通量','CH4 Flux','CARBON_FLUX','mg/m2/s','甲烷通量'),
('N2O_FLUX','N2O通量','N2O Flux','CARBON_FLUX','mg/m2/s','氧化亚氮通量'),
('GHG_FLUX','温室气体综合通量','Greenhouse Gas Flux','CARBON_FLUX','CO2-eq','温室气体综合指标'),
('GPP','总初级生产力','Gross Primary Productivity','CARBON_FLUX','g C/m2/d','总初级生产力'),
('RECO','生态系统呼吸','Ecosystem Respiration','CARBON_FLUX','g C/m2/d','生态系统呼吸'),
('NEP','净生态系统生产力','Net Ecosystem Productivity','CARBON_FLUX','g C/m2/d','净生态系统生产力'),
('NDVI','归一化植被指数','NDVI','REMOTE_SENSING','index','遥感植被指数'),
('CANOPY_HEIGHT','冠层高度','Canopy Height','VEGETATION_STRUCTURE','m','冠层高度'),
('AGC','地上碳储量','Aboveground Carbon','CARBON_STOCK','Mg C/ha','地上碳储量'),
('MANGROVE_AREA','红树林面积','Mangrove Area','REMOTE_SENSING','ha','红树林覆盖面积'),
('ECONOMIC_VALUE','蓝碳经济价值','Blue Carbon Economic Value','ECONOMIC_VALUE','CNY','蓝碳价值估算结果')
ON DUPLICATE KEY UPDATE `indicator_name_cn`=VALUES(`indicator_name_cn`), `indicator_domain`=VALUES(`indicator_domain`), `default_unit`=VALUES(`default_unit`), `description`=VALUES(`description`);

INSERT INTO `t_region` (`region_code`,`parent_region_id`,`region_name`,`region_level`,`province`,`city`,`ecosystem_type`,`data_scope`,`remark`) VALUES
('DEMO_ROOT',NULL,'示例区域','OTHER','示例省',NULL,'OTHER','PUBLIC','完全合成的公开演示根节点'),
('DEMO_REGION_A',(SELECT region_id FROM (SELECT region_id FROM `t_region` WHERE region_code='DEMO_ROOT') AS tmp),'示例区域甲','OTHER','示例省','示例市甲','OTHER','PUBLIC','完全合成的公开演示节点')
ON DUPLICATE KEY UPDATE `region_name`=VALUES(`region_name`), `region_level`=VALUES(`region_level`), `ecosystem_type`=VALUES(`ecosystem_type`), `remark`=VALUES(`remark`);

INSERT INTO `t_data_source` (`source_code`,`source_category`,`source_name`,`publisher`,`is_simulated`,`remark`) VALUES
('SYNTHETIC_HNBLUE_PUBLIC','SIMULATED','HNBLUE 公开版合成测试数据','HNBLUE',1,'仅用于接口和界面测试，不作为科研或业务数据来源')
ON DUPLICATE KEY UPDATE `source_category`=VALUES(`source_category`), `source_name`=VALUES(`source_name`), `is_simulated`=VALUES(`is_simulated`), `remark`=VALUES(`remark`);

SET FOREIGN_KEY_CHECKS = 1;
