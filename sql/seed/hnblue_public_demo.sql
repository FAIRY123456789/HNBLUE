-- HNBLUE public demo seed
-- Every identifier and value below is fabricated for interface testing.

INSERT INTO t_data_source
    (source_code, source_category, source_name, publisher, license_text, is_simulated, remark)
VALUES
    ('SYNTHETIC-SOURCE-01', 'SIMULATED', 'Synthetic interface fixture',
     'HNBLUE public demo', 'No scientific use', 1,
     'Fabricated record for interface tests only');

INSERT INTO t_region
    (region_code, region_name, region_level, province, city, ecosystem_type, data_scope, remark)
VALUES
    ('DEMO-REGION-A', '示例区域甲', 'OTHER', '示例省', '示例市甲', 'OTHER', 'PUBLIC',
     'Fabricated record for interface tests only'),
    ('DEMO-REGION-B', '示例区域乙', 'OTHER', '示例省', '示例市乙', 'OTHER', 'PUBLIC',
     'Fabricated record for interface tests only');

-- Adapt table/column names to the schema version in use. Do not replace these
-- rows with production exports or real monitoring observations.
