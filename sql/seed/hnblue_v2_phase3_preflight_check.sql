-- HNBLUE V2.0 Phase 3 preflight check SQL
-- Read-only verification. This file contains SELECT statements only.
-- Do not run against the legacy `hnblue` schema as an import target.

SET NAMES utf8mb4;

-- 1. Required schemas.
SELECT
  s.SCHEMA_NAME AS schema_name,
  CASE WHEN s.SCHEMA_NAME IS NULL THEN 'MISSING' ELSE 'EXISTS' END AS status
FROM (
  SELECT 'hainan_blue_carbon_core' AS schema_name
  UNION ALL SELECT 'hainan_blue_carbon_satellite'
  UNION ALL SELECT 'hainan_blue_carbon_intl'
) expected
LEFT JOIN information_schema.SCHEMATA s
  ON s.SCHEMA_NAME = expected.schema_name;

-- 2. Guard: legacy hnblue may exist but is not a target in this phase.
SELECT
  'legacy_hnblue_guard' AS check_name,
  CASE WHEN COUNT(*) = 0 THEN 'legacy schema absent'
       ELSE 'legacy schema exists; do not write it in Phase 3'
  END AS status
FROM information_schema.SCHEMATA
WHERE SCHEMA_NAME = 'hnblue';

-- 3. Required target tables.
WITH expected_tables AS (
SELECT 'hainan_blue_carbon_core' AS target_schema, 't_data_source' AS target_table
UNION ALL
SELECT 'hainan_blue_carbon_core' AS target_schema, 't_region' AS target_table
UNION ALL
SELECT 'hainan_blue_carbon_satellite' AS target_schema, 't_satellite_mangrove_cover' AS target_table
UNION ALL
SELECT 'hainan_blue_carbon_satellite' AS target_schema, 't_satellite_region_metric' AS target_table
UNION ALL
SELECT 'hainan_blue_carbon_intl' AS target_schema, 't_literature_reference' AS target_table
UNION ALL
SELECT 'hainan_blue_carbon_intl' AS target_schema, 't_literature_carbon_record' AS target_table
UNION ALL
SELECT 'hainan_blue_carbon_intl' AS target_schema, 't_allometry_equation' AS target_table
)
SELECT
  e.target_schema,
  e.target_table,
  CASE WHEN t.TABLE_NAME IS NULL THEN 'MISSING' ELSE 'EXISTS' END AS status
FROM expected_tables e
LEFT JOIN information_schema.TABLES t
  ON t.TABLE_SCHEMA = e.target_schema
 AND t.TABLE_NAME = e.target_table
ORDER BY e.target_schema, e.target_table;

-- 4. Required columns used by Phase 2/3 import SQL.
WITH expected_columns AS (
SELECT 'hainan_blue_carbon_core' AS target_schema, 't_data_source' AS target_table, 'source_code' AS target_column
UNION ALL
SELECT 'hainan_blue_carbon_core' AS target_schema, 't_data_source' AS target_table, 'source_category' AS target_column
UNION ALL
SELECT 'hainan_blue_carbon_core' AS target_schema, 't_data_source' AS target_table, 'source_name' AS target_column
UNION ALL
SELECT 'hainan_blue_carbon_core' AS target_schema, 't_data_source' AS target_table, 'source_url' AS target_column
UNION ALL
SELECT 'hainan_blue_carbon_core' AS target_schema, 't_data_source' AS target_table, 'citation_text' AS target_column
UNION ALL
SELECT 'hainan_blue_carbon_core' AS target_schema, 't_data_source' AS target_table, 'version_label' AS target_column
UNION ALL
SELECT 'hainan_blue_carbon_core' AS target_schema, 't_data_source' AS target_table, 'is_simulated' AS target_column
UNION ALL
SELECT 'hainan_blue_carbon_core' AS target_schema, 't_data_source' AS target_table, 'remark' AS target_column
UNION ALL
SELECT 'hainan_blue_carbon_core' AS target_schema, 't_region' AS target_table, 'region_code' AS target_column
UNION ALL
SELECT 'hainan_blue_carbon_core' AS target_schema, 't_region' AS target_table, 'parent_region_id' AS target_column
UNION ALL
SELECT 'hainan_blue_carbon_core' AS target_schema, 't_region' AS target_table, 'region_name' AS target_column
UNION ALL
SELECT 'hainan_blue_carbon_core' AS target_schema, 't_region' AS target_table, 'region_level' AS target_column
UNION ALL
SELECT 'hainan_blue_carbon_core' AS target_schema, 't_region' AS target_table, 'province' AS target_column
UNION ALL
SELECT 'hainan_blue_carbon_core' AS target_schema, 't_region' AS target_table, 'city' AS target_column
UNION ALL
SELECT 'hainan_blue_carbon_core' AS target_schema, 't_region' AS target_table, 'county' AS target_column
UNION ALL
SELECT 'hainan_blue_carbon_core' AS target_schema, 't_region' AS target_table, 'ecosystem_type' AS target_column
UNION ALL
SELECT 'hainan_blue_carbon_core' AS target_schema, 't_region' AS target_table, 'data_scope' AS target_column
UNION ALL
SELECT 'hainan_blue_carbon_core' AS target_schema, 't_region' AS target_table, 'remark' AS target_column
UNION ALL
SELECT 'hainan_blue_carbon_satellite' AS target_schema, 't_satellite_mangrove_cover' AS target_table, 'region_id' AS target_column
UNION ALL
SELECT 'hainan_blue_carbon_satellite' AS target_schema, 't_satellite_mangrove_cover' AS target_table, 'metric_year' AS target_column
UNION ALL
SELECT 'hainan_blue_carbon_satellite' AS target_schema, 't_satellite_mangrove_cover' AS target_table, 'mangrove_area_ha' AS target_column
UNION ALL
SELECT 'hainan_blue_carbon_satellite' AS target_schema, 't_satellite_region_metric' AS target_table, 'indicator_code' AS target_column
UNION ALL
SELECT 'hainan_blue_carbon_satellite' AS target_schema, 't_satellite_region_metric' AS target_table, 'value' AS target_column
UNION ALL
SELECT 'hainan_blue_carbon_intl' AS target_schema, 't_literature_reference' AS target_table, 'source_id' AS target_column
UNION ALL
SELECT 'hainan_blue_carbon_intl' AS target_schema, 't_literature_carbon_record' AS target_table, 'value_mg_ha' AS target_column
UNION ALL
SELECT 'hainan_blue_carbon_intl' AS target_schema, 't_allometry_equation' AS target_table, 'equation_form' AS target_column
)
SELECT
  e.target_schema,
  e.target_table,
  e.target_column,
  CASE WHEN c.COLUMN_NAME IS NULL THEN 'MISSING' ELSE 'EXISTS' END AS status,
  c.DATA_TYPE,
  c.COLUMN_TYPE,
  c.IS_NULLABLE
FROM expected_columns e
LEFT JOIN information_schema.COLUMNS c
  ON c.TABLE_SCHEMA = e.target_schema
 AND c.TABLE_NAME = e.target_table
 AND c.COLUMN_NAME = e.target_column
ORDER BY e.target_schema, e.target_table, e.target_column;

-- 5. Current row counts before any minimal import.
SELECT 'hainan_blue_carbon_core.t_data_source' AS table_name, COUNT(*) AS row_count
FROM hainan_blue_carbon_core.t_data_source
UNION ALL
SELECT 'hainan_blue_carbon_core.t_region', COUNT(*)
FROM hainan_blue_carbon_core.t_region
UNION ALL
SELECT 'hainan_blue_carbon_satellite.t_satellite_mangrove_cover', COUNT(*)
FROM hainan_blue_carbon_satellite.t_satellite_mangrove_cover
UNION ALL
SELECT 'hainan_blue_carbon_satellite.t_satellite_region_metric', COUNT(*)
FROM hainan_blue_carbon_satellite.t_satellite_region_metric
UNION ALL
SELECT 'hainan_blue_carbon_intl.t_literature_carbon_record', COUNT(*)
FROM hainan_blue_carbon_intl.t_literature_carbon_record
UNION ALL
SELECT 'hainan_blue_carbon_intl.t_allometry_equation', COUNT(*)
FROM hainan_blue_carbon_intl.t_allometry_equation;

-- 6. Existing core rows matching this public package.
SELECT source_code, source_id
FROM hainan_blue_carbon_core.t_data_source
WHERE source_code IN (
  SELECT source_id FROM hainan_blue_carbon_core.t_data_source WHERE 1 = 0
);
-- Fill the empty subquery above with source_code values from
-- data/results/hnblue_v2_public/import_ready/core_data_sources_import_ready.csv
-- when running manually in a SQL client.
