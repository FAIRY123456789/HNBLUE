-- HNBLUE V2.0 public record seed generated from second deep search.
-- Representative inserts only. Full staging records are in data/staging/**/*.csv.
-- Do not execute against the V1.0 hnblue demo schema.

INSERT INTO hainan_blue_carbon_intl.t_literature_reference
(doi, title, authors, publication_year, journal_or_source, url, study_area, ecosystem_type, citation_text)
VALUES
('10.3390/w14203278','Spatial Variation of Soil Organic Carbon from Bamen Bay Mangrove in Southern China','See publisher page',2022,'Water','https://www.mdpi.com/2073-4441/14/20/3278','Bamen Bay, Wenchang, Hainan','mangrove','Use DOI and publisher citation'),
('10.3389/fmars.2022.932984','Incorporating coastal blue carbon into subnational greenhouse gas inventories','See publisher page',2022,'Frontiers in Marine Science','https://www.frontiersin.org/journals/marine-science/articles/10.3389/fmars.2022.932984/full','Hainan Province','mangrove;seagrass;coastal wetland','Use DOI and publisher citation'),
('10.1007/s11284-005-0088-4','Common allometric equations for estimating the tree weight of mangroves','Komiyama et al.',2005,'Journal of Tropical Ecology / Springer','https://link.springer.com/article/10.1007/s11284-005-0088-4','Global mangroves','mangrove','Use DOI and publisher citation')
ON DUPLICATE KEY UPDATE title=VALUES(title), url=VALUES(url);

INSERT INTO hainan_blue_carbon_intl.t_literature_carbon_record
(site_name, ecosystem_type, carbon_pool, value_mg_ha, method_note)
VALUES
('Bamen Bay TU/TI','mangrove','SOC',104.410000,'0-80 cm SOC storage from Bamen Bay article abstract'),
('Bamen Bay upper estuary','mangrove','SOC',207.140000,'0-80 cm SOC storage from Bamen Bay article abstract'),
('Bamen Bay lower estuary','mangrove','SOC',228.780000,'0-80 cm SOC storage from Bamen Bay article abstract'),
('Hainan Province','mangrove','AGB',61.700000,'EF_AGB from Frontiers Table 3'),
('Hainan Province','mangrove','SOC',227.400000,'EF_soil from Frontiers Table 3'),
('Hainan Province','mangrove','TOTAL',319.300000,'Total biomass plus soil carbon from Frontiers text'),
('Wenchang','mangrove','AGB',66.200000,'EF_AGB from Frontiers Table 3'),
('Wenchang','mangrove','SOC',337.000000,'EF_soil from Frontiers Table 3'),
('Dongfang','mangrove','SOC',473.300000,'EF_soil from Frontiers Table 3');

INSERT INTO hainan_blue_carbon_intl.t_external_observation
(site_name, country, province, ecosystem_type, scientific_name, indicator_code, indicator_value, unit, observation_year)
VALUES
('Hainan Province','China','Hainan','mangrove',NULL,'MANGROVE_AREA',4190.700000,'ha',2010),
('Hainan Province','China','Hainan','mangrove',NULL,'MANGROVE_AREA',4644.100000,'ha',2020),
('Bamen Bay Nature Reserve','China','Hainan','mangrove',NULL,'MANGROVE_AREA',1223.300000,'hm2',2022),
('Bamen Bay Nature Reserve','China','Hainan','mangrove',NULL,'MANGROVE_AREA',842.200000,'hm2 natural mangrove',2022),
('Bamen Bay Nature Reserve','China','Hainan','mangrove',NULL,'MANGROVE_AREA',381.100000,'hm2 planted mangrove',2022),
('Hainan Province','China','Hainan','mangrove',NULL,'TOTAL_CARBON',239.120000,'MgC/ha mean carbon density',2022),
('Hainan Province','China','Hainan','mangrove',NULL,'TOTAL_CARBON',89.200000,'MgC/ha min carbon density',2022),
('Hainan Province','China','Hainan','mangrove',NULL,'TOTAL_CARBON',431.980000,'MgC/ha max carbon density',2022),
('Hainan Province','China','Hainan','mangrove',NULL,'GHG_FLUX',-26974.300000,'MgCO2e/yr',2020),
('Wenchang','China','Hainan','mangrove',NULL,'GHG_FLUX',-2128.100000,'MgCO2e/yr proxy',2020),
('Wenchang','China','Hainan','aquaculture',NULL,'N2O_FLUX',15454.900000,'MgCO2e/yr proxy',2020);

INSERT INTO hainan_blue_carbon_intl.t_allometry_equation
(country, province, scientific_name, equation_form, component, coefficient_a, coefficient_b, coefficient_c, unit_note)
VALUES
('Global',NULL,'All mangrove species','Wtop=0.251*rho*D^2.46','ABOVEGROUND',0.251000,2.460000,NULL,'D cm; rho g/cm3; W kg'),
('Global',NULL,'All mangrove species','Wr=0.199*rho^0.899*D^2.22','BELOWGROUND',0.199000,0.899000,2.220000,'D cm; rho g/cm3; W kg'),
('China','Hainan','Mangrove inventory parameter','BT=EF_AGB*(1+R)+EF_SOC','BELOWGROUND',0.490000,NULL,NULL,'R root-shoot ratio from Hainan inventory');
