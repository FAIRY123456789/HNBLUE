package com.example.jpaspringboot.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class V2DataService {

    private static final String INDICATOR_DICTIONARY_TABLE = "hainan_blue_carbon_core.t_indicator_dictionary";
    private static final String DATA_SOURCE_TABLE = "hainan_blue_carbon_core.t_data_source";
    private static final String REGION_TABLE = "hainan_blue_carbon_core.t_region";
    private static final String MANGROVE_COVER_TABLE = "hainan_blue_carbon_satellite.t_satellite_mangrove_cover";
    private static final String REGION_METRIC_TABLE = "hainan_blue_carbon_satellite.t_satellite_region_metric";
    private static final String LITERATURE_CARBON_TABLE = "hainan_blue_carbon_intl.t_literature_carbon_record";
    private static final String CARBON_MODEL_PARAMETER_TABLE = "t_carbon_model_parameter";
    private static final String CARBON_MARKET_PRICE_TABLE = "t_carbon_market_price";
    private static final String CARBON_METHODOLOGY_TABLE = "t_carbon_methodology";
    private static final String LOCAL_CARBON_TRADE_CASE_TABLE = "t_local_carbon_trade_case";
    private static final String POLICY_TARGET_TABLE = "t_policy_target";
    private static final String DERIVED_METRIC_DEFINITION_TABLE = "t_derived_metric_definition";
    private static final String ECOLOGICAL_NODE_TABLE = "t_ecological_node";
    private static final String DATA_GOVERNANCE_TASK_TABLE = "t_data_governance_task";

    private final JdbcTemplate jdbcTemplate;

    public V2DataService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> health() {
        Map<String, Object> payload = base("v2 api is readable");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("api_status", "UP");
        data.put("current_time", OffsetDateTime.now().toString());
        data.put("database", checkDatabase());
        data.put("tables", tableHealth());
        payload.put("data", data);
        return payload;
    }

    @Cacheable(cacheNames = "dashboard-summary", key = "T(com.example.jpaspringboot.service.V2CacheKeys).all()", unless = "#result == null || #result['success'] == false")
    public Map<String, Object> dashboardSummary() {
        Map<String, Object> payload = base("dashboard summary");
        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("indicator_dictionary", safeCountOrDefault(INDICATOR_DICTIONARY_TABLE, 0L));
        counts.put("data_source", safeCountOrDefault(DATA_SOURCE_TABLE, 0L));
        counts.put("mangrove_cover", safeCountOrDefault(MANGROVE_COVER_TABLE, 0L));
        counts.put("region_metric", safeCountOrDefault(REGION_METRIC_TABLE, 0L));
        counts.put("literature_carbon", safeCountOrDefault(LITERATURE_CARBON_TABLE, 0L));
        counts.put("source_code", safeScalar(
                "select count(distinct source_code) from " + DATA_SOURCE_TABLE,
                Long.class,
                0L
        ));
        counts.put("identified_region", safeScalar(
                "select count(distinct region_id) from " + REGION_TABLE,
                Long.class,
                0L
        ));
        data.put("counts", counts);
        data.put("update_time", updateTimeSummary());
        data.put("boundary_notes", Arrays.asList(
                "Dashboard counts are table-level governance indicators, not merged ecological conclusions.",
                "Records from different sources, scales and methods remain separated in downstream APIs.",
                "Carbon price and CCER records are optional context assets and are not local verified revenue."
        ));
        payload.put("data", data);
        return payload;
    }

    @Cacheable(cacheNames = "sources", key = "T(com.example.jpaspringboot.service.V2CacheKeys).filters(#filters)", unless = "#result == null || #result['success'] == false")
    public Map<String, Object> sources(Map<String, String> filters) {
        filters = safeFilters(filters);
        Page page = page(filters);
        SqlParts sql = new SqlParts();
        sql.select = "select source_id, source_code, source_category as source_type, source_name, publisher, " +
                "author_or_team, source_url as url, citation_text as citation, license_text, version_label, " +
                "is_simulated, remark as description, access_date, download_date, created_at, updated_at " +
                "from " + DATA_SOURCE_TABLE + " where 1=1";
        addEquals(sql, "source_category", filters.get("source_type"));
        addEquals(sql, "source_code", filters.get("source_code"));
        String keyword = trim(filters.get("keyword"));
        if (keyword != null) {
            sql.where.add("(source_code like ? or source_name like ? or citation_text like ? or remark like ?)");
            addLike(sql, keyword, 4);
        }
        sql.order = " order by updated_at desc, source_id desc";
        return listResponse("sources", sql, page, Arrays.asList(
                "live schema uses source_category as source_type compatible field.",
                "reliability_level is absent in live t_data_source; reliability filtering is recorded as unsupported when supplied."
        ), unsupported(filters, "reliability_level"));
    }

    @Cacheable(cacheNames = "mangrove-cover", key = "T(com.example.jpaspringboot.service.V2CacheKeys).filters(#filters)", unless = "#result == null || #result['success'] == false")
    public Map<String, Object> mangroveCover(Map<String, String> filters) {
        filters = safeFilters(filters);
        Page page = page(filters);
        SqlParts sql = new SqlParts();
        sql.select = "select c.cover_id, c.region_id, r.region_code, r.region_name, r.region_full_name, " +
                "c.metric_year as year, c.mangrove_area_ha as area_value, 'ha' as unit, c.cover_ratio, " +
                "c.classification_method as method, c.overall_accuracy, c.kappa, " +
                "c.qc_flag_id as quality_flag, s.source_code, s.source_name, c.created_at " +
                "from " + MANGROVE_COVER_TABLE + " c " +
                "left join " + REGION_TABLE + " r on r.region_id = c.region_id " +
                "left join " + DATA_SOURCE_TABLE + " s on s.source_id = c.source_id where 1=1";
        addRegion(sql, "c.region_id", "r.region_name", filters);
        addIntEquals(sql, "c.metric_year", filters.get("year"));
        addEquals(sql, "s.source_code", filters.get("source_code"));
        String keyword = trim(filters.get("keyword"));
        if (keyword != null) {
            sql.where.add("(r.region_name like ? or r.region_full_name like ? or s.source_code like ? or c.classification_method like ?)");
            addLike(sql, keyword, 4);
        }
        sql.order = " order by c.metric_year desc, r.region_name asc, c.cover_id desc";
        Map<String, Object> response = listResponse("mangrove_cover", sql, page, Arrays.asList(
                "Mangrove cover records keep source-specific method and accuracy fields; the API does not average across sources.",
                "A-class Total or sample aggregate rows must be interpreted through source caliber and are not automatic Hainan total area."
        ), List.of());
        return response;
    }

    @Cacheable(cacheNames = "region-metrics", key = "T(com.example.jpaspringboot.service.V2CacheKeys).filters(#filters)", unless = "#result == null || #result['success'] == false")
    public Map<String, Object> regionMetrics(Map<String, String> filters) {
        filters = safeFilters(filters);
        Page page = page(filters);
        SqlParts sql = new SqlParts();
        sql.select = "select m.metric_id, m.region_id, r.region_code, r.region_name, r.region_full_name, " +
                "m.indicator_code, coalesce(m.indicator_name, d.indicator_name_cn) as indicator_name, " +
                "d.indicator_domain as metric_category, m.metric_year as year, m.metric_month, m.metric_date, " +
                "m.value, m.unit, m.stat_method, s.source_code, s.source_name, m.quality_level as quality_flag, " +
                "m.is_proxy, m.is_simulated, coalesce(m.notes, m.method_note, d.description) as description, " +
                "m.method_note, m.source_record_id, m.created_at " +
                "from " + REGION_METRIC_TABLE + " m " +
                "left join " + REGION_TABLE + " r on r.region_id = m.region_id " +
                "left join " + DATA_SOURCE_TABLE + " s on s.source_id = m.source_id " +
                "left join " + INDICATOR_DICTIONARY_TABLE + " d on d.indicator_code = m.indicator_code where 1=1";
        addRegion(sql, "m.region_id", "r.region_name", filters);
        addEquals(sql, "d.indicator_domain", filters.get("metric_category"));
        addEquals(sql, "m.indicator_code", filters.get("indicator_code"));
        addIntEquals(sql, "m.metric_year", filters.get("year"));
        addEquals(sql, "s.source_code", filters.get("source_code"));
        addEquals(sql, "m.quality_level", filters.get("quality_flag"));
        String keyword = trim(filters.get("keyword"));
        if (keyword != null) {
            sql.where.add("(r.region_name like ? or m.indicator_code like ? or m.indicator_name like ? or d.indicator_name_cn like ? or m.notes like ?)");
            addLike(sql, keyword, 5);
        }
        sql.order = " order by m.metric_year desc, r.region_name asc, m.indicator_code asc, m.metric_id desc";
        return listResponse("region_metrics", sql, page, Arrays.asList(
                "Proxy indicators are environmental background or pressure explanatory variables.",
                "Proxy indicators are not direct evidence of observed ecological damage or observed carbon flux.",
                "Model output and simulated flags remain visible and are not merged with observations."
        ), List.of());
    }

    @Cacheable(cacheNames = "literature-carbon", key = "T(com.example.jpaspringboot.service.V2CacheKeys).filters(#filters)", unless = "#result == null || #result['success'] == false")
    public Map<String, Object> literatureCarbon(Map<String, String> filters) {
        filters = safeFilters(filters);
        Page page = page(filters);
        SqlParts sql = new SqlParts();
        sql.select = "select l.record_id, l.region_id, r.region_code, r.region_name, r.region_full_name, " +
                "l.site_name, l.latitude, l.longitude, l.ecosystem_type, l.carbon_pool, " +
                "case when l.is_simulated = 1 then 'MODEL_OR_SCENARIO' when l.is_proxy = 1 then 'PROXY' else 'OBSERVATION_OR_LITERATURE' end as data_type, " +
                "l.value_mg_ha, l.value, coalesce(l.unit, 'Mg/ha') as unit, l.depth_top_cm, l.depth_bottom_cm, " +
                "l.method_note as method_type, l.source_record_id, l.is_proxy, l.is_simulated, l.indicator_code, l.indicator_name, " +
                "l.quality_level, l.notes as evidence_text, s.source_code, s.source_name, s.citation_text as citation, s.source_url as url, l.created_at " +
                "from " + LITERATURE_CARBON_TABLE + " l " +
                "left join " + REGION_TABLE + " r on r.region_id = l.region_id " +
                "left join " + DATA_SOURCE_TABLE + " s on s.source_id = l.source_id where 1=1";
        addRegion(sql, "l.region_id", "r.region_name", filters);
        addEquals(sql, "l.carbon_pool", filters.get("carbon_pool"));
        addEquals(sql, "s.source_code", filters.get("source_code"));
        String dataType = trim(filters.get("data_type"));
        if (dataType != null) {
            String normalized = dataType.toLowerCase(Locale.ROOT);
            if (normalized.contains("model") || normalized.contains("scenario") || normalized.contains("simulated")) {
                sql.where.add("l.is_simulated = ?");
                sql.params.add(1);
            } else if (normalized.contains("proxy")) {
                sql.where.add("l.is_proxy = ?");
                sql.params.add(1);
            } else if (normalized.contains("observ") || normalized.contains("literature")) {
                sql.where.add("l.is_simulated = ? and l.is_proxy = ?");
                sql.params.add(0);
                sql.params.add(0);
            }
        }
        String methodType = trim(filters.get("method_type"));
        if (methodType != null) {
            sql.where.add("l.method_note like ?");
            sql.params.add("%" + methodType + "%");
        }
        String keyword = trim(filters.get("keyword"));
        if (keyword != null) {
            sql.where.add("(r.region_name like ? or l.site_name like ? or l.method_note like ? or l.notes like ? or s.citation_text like ?)");
            addLike(sql, keyword, 5);
        }
        sql.order = " order by l.created_at desc, l.record_id desc";
        return listResponse("literature_carbon", sql, page, Arrays.asList(
                "Biomass and carbon amount are separated by indicator and unit; value_mg_ha is not a transaction value.",
                "Model results and observation facts remain separated through is_simulated and data_type.",
                "Scenario predictions are conditional results under explicit assumptions."
        ), List.of());
    }

    @Cacheable(cacheNames = "region-overview", key = "T(com.example.jpaspringboot.service.V2CacheKeys).region(#regionId)", unless = "#result == null || #result['success'] == false")
    public Map<String, Object> regionOverview(String regionId) {
        Map<String, Object> payload = base("region overview");
        Map<String, Object> data = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> regions = jdbcTemplate.queryForList(
                    "select region_id, region_code, region_name, region_full_name, region_level, region_type, " +
                            "province, city, county, ecosystem_type, centroid_lat, centroid_lon, area_ha, protection_status, data_scope, remark " +
                            "from " + REGION_TABLE + " where cast(region_id as char) = ? or region_code = ? limit 1",
                    regionId, regionId
            );
            if (regions.isEmpty()) {
                payload.put("success", false);
                payload.put("message", "region not found");
                payload.put("data", data);
                return payload;
            }
            Map<String, Object> region = regions.get(0);
            String resolvedRegionId = String.valueOf(region.get("region_id"));
            data.put("region", region);
            Map<String, String> filter = new LinkedHashMap<>();
            filter.put("region_id", resolvedRegionId);
            filter.put("limit", "12");
            data.put("mangrove_cover", mangroveCover(filter).get("data"));
            data.put("region_metrics", regionMetrics(filter).get("data"));
            data.put("literature_carbon", literatureCarbon(filter).get("data"));
            data.put("source_codes", sourceCodesForRegion(resolvedRegionId));
            data.put("boundary_notes", Arrays.asList(
                    "region_id is resolved by BIGINT-compatible cast and region_code fallback.",
                    "Overview is a drill-down aggregation for inspection; it does not merge heterogeneous sources into a single conclusion.",
                    "Use source_code and evidence fields before policy interpretation."
            ));
        } catch (DataAccessException ex) {
            payload.put("success", false);
            payload.put("message", "region overview query failed");
            data.put("error", ex.getMostSpecificCause().getMessage());
        }
        payload.put("data", data);
        return payload;
    }

    @Cacheable(cacheNames = "ai-context", key = "T(com.example.jpaspringboot.service.V2CacheKeys).filters(#filters)", unless = "#result == null || #result['success'] == false")
    public Map<String, Object> aiContext(Map<String, String> filters) {
        filters = safeFilters(filters);
        Map<String, Object> payload = base("ai context");
        Map<String, Object> data = new LinkedHashMap<>();
        String question = trim(filters.get("question"));
        String region = trim(filters.get("region"));
        String topic = trim(filters.get("topic"));
        String keyword = firstNonEmpty(question, topic);

        Map<String, String> sourceFilters = new LinkedHashMap<>();
        sourceFilters.put("limit", "8");
        if (keyword != null) {
            sourceFilters.put("keyword", keyword);
        }
        Map<String, String> domainFilters = new LinkedHashMap<>();
        domainFilters.put("limit", "8");
        if (region != null) {
            domainFilters.put("region", region);
        }
        if (keyword != null) {
            domainFilters.put("keyword", keyword);
        }

        data.put("query", filters);
        data.put("sources", extractRows(sources(sourceFilters)));
        data.put("mangrove_cover", extractRows(mangroveCover(domainFilters)));
        data.put("region_metrics", extractRows(regionMetrics(domainFilters)));
        data.put("literature_carbon", extractRows(literatureCarbon(domainFilters)));
        data.put("boundary_notes", Arrays.asList(
                "This endpoint returns retrieval context only and does not call an LLM.",
                "AI answers must cite source_code, indicator_code or evidence_text from this payload.",
                "Proxy, model and scenario records require explicit boundary wording."
        ));
        payload.put("data", data);
        return payload;
    }

    public Map<String, Object> optionalStatus() {
        Map<String, Object> payload = base("optional status");
        List<Map<String, Object>> assets = new ArrayList<>();
        assets.add(optionalAsset("model_parameters", CARBON_MODEL_PARAMETER_TABLE, "Model parameter management"));
        assets.add(optionalAsset("carbon_market_prices", CARBON_MARKET_PRICE_TABLE, "Reference market context"));
        assets.add(optionalAsset("ccer_methodology", CARBON_METHODOLOGY_TABLE, "Methodology traceability"));
        assets.add(optionalAsset("local_trade_cases", LOCAL_CARBON_TRADE_CASE_TABLE, "Local case evidence, not generalized revenue"));
        assets.add(optionalAsset("policy_targets", POLICY_TARGET_TABLE, "Policy target tracking"));
        assets.add(optionalAsset("derived_metric_definitions", DERIVED_METRIC_DEFINITION_TABLE, "Derived indicator governance"));
        assets.add(optionalAsset("ecological_nodes", ECOLOGICAL_NODE_TABLE, "Spatial node candidates"));
        assets.add(optionalAsset("data_governance_tasks", DATA_GOVERNANCE_TASK_TABLE, "Data governance backlog"));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("assets", assets);
        data.put("boundary_notes", Arrays.asList(
                "Optional assets are enhancement records and are not required for core dashboard counts.",
                "National CEA, CCER and voluntary market prices are reference context, not Hainan verified transaction revenue.",
                "Assets without live tables remain documented through data/staging and data/processed import-ready files."
        ));
        payload.put("data", data);
        return payload;
    }

    private Map<String, Object> listResponse(String name, SqlParts sql, Page page, List<String> boundaryNotes, List<String> unsupportedFilters) {
        Map<String, Object> payload = base(name);
        Map<String, Object> data = new LinkedHashMap<>();
        try {
            String where = sql.where.isEmpty() ? "" : " and " + String.join(" and ", sql.where);
            List<Object> countParams = new ArrayList<>(sql.params);
            Long total = jdbcTemplate.queryForObject("select count(1) from (" + sql.select + where + ") x", Long.class, countParams.toArray());
            List<Object> pageParams = new ArrayList<>(sql.params);
            pageParams.add(page.limit);
            pageParams.add(page.offset);
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.select + where + sql.order + " limit ? offset ?", pageParams.toArray());
            data.put("rows", rows);
            data.put("pagination", pagination(total == null ? 0 : total, page));
            data.put("boundary_notes", boundaryNotes);
            data.put("unsupported_filters", unsupportedFilters);
        } catch (DataAccessException ex) {
            payload.put("success", false);
            payload.put("message", name + " query failed");
            data.put("rows", List.of());
            data.put("pagination", pagination(0, page));
            data.put("boundary_notes", boundaryNotes);
            data.put("unsupported_filters", unsupportedFilters);
            data.put("error", ex.getMostSpecificCause().getMessage());
        }
        payload.put("data", data);
        return payload;
    }

    private List<Map<String, Object>> extractRows(Map<String, Object> response) {
        Object data = response.get("data");
        if (!(data instanceof Map<?, ?> dataMap)) {
            return List.of();
        }
        Object rows = dataMap.get("rows");
        if (rows instanceof List<?> rowList) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : rowList) {
                if (item instanceof Map<?, ?> map) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    map.forEach((key, value) -> row.put(String.valueOf(key), value));
                    result.add(row);
                }
            }
            return result;
        }
        return List.of();
    }

    private Map<String, Object> base(String message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", true);
        payload.put("message", message);
        payload.put("meta", Map.of(
                "api_version", "v2",
                "generated_at", OffsetDateTime.now().toString()
        ));
        return payload;
    }

    private Map<String, Object> checkDatabase() {
        Map<String, Object> db = new LinkedHashMap<>();
        try {
            Integer value = jdbcTemplate.queryForObject("select 1", Integer.class);
            db.put("readable", value != null && value == 1);
        } catch (DataAccessException ex) {
            db.put("readable", false);
            db.put("error", ex.getMostSpecificCause().getMessage());
        }
        return db;
    }

    private List<Map<String, Object>> tableHealth() {
        List<String> tables = List.of(
                INDICATOR_DICTIONARY_TABLE,
                DATA_SOURCE_TABLE,
                MANGROVE_COVER_TABLE,
                REGION_METRIC_TABLE,
                LITERATURE_CARBON_TABLE
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (String table : tables) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("database", currentDatabase());
            row.put("table", table);
            try {
                row.put("readable", true);
                row.put("count", safeCount(table));
            } catch (RuntimeException ex) {
                row.put("readable", false);
                row.put("error", ex.getMessage());
            }
            result.add(row);
        }
        return result;
    }

    private Map<String, Object> updateTimeSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("source", "unified datasource database created_at/updated_at inspection");
        summary.put("database", currentDatabase());
        summary.put("field_boundary", "Runtime mode expects all V2 tables in the current datasource database; t_data_source and t_region expose updated_at in the inspected snapshot, satellite and literature tables expose created_at only.");
        summary.put("data_source_updated_at", safeScalar("select max(updated_at) from " + DATA_SOURCE_TABLE, Object.class, null));
        summary.put("region_updated_at", safeScalar("select max(updated_at) from " + REGION_TABLE, Object.class, null));
        summary.put("mangrove_cover_created_at", safeScalar("select max(created_at) from " + MANGROVE_COVER_TABLE, Object.class, null));
        summary.put("region_metric_created_at", safeScalar("select max(created_at) from " + REGION_METRIC_TABLE, Object.class, null));
        summary.put("literature_carbon_created_at", safeScalar("select max(created_at) from " + LITERATURE_CARBON_TABLE, Object.class, null));
        return summary;
    }

    private Long safeCount(String table) {
        Long count = jdbcTemplate.queryForObject("select count(1) from " + table, Long.class);
        return count == null ? 0L : count;
    }

    private <T> T safeScalar(String sql, Class<T> type, T fallback) {
        try {
            T value = jdbcTemplate.queryForObject(sql, type);
            return value == null ? fallback : value;
        } catch (DataAccessException ex) {
            return fallback;
        }
    }

    private Map<String, Object> optionalAsset(String key, String table, String usage) {
        Map<String, Object> asset = new LinkedHashMap<>();
        asset.put("key", key);
        asset.put("database", currentDatabase());
        asset.put("table", table);
        asset.put("usage", usage);
        boolean exists = tableExists(table);
        asset.put("live_table_exists", exists);
        Long recordCount = exists ? safeCountOrDefault(table, 0L) : 0L;
        asset.put("record_count", recordCount);
        asset.put("status", exists ? "LIVE_TABLE_READABLE" : "STAGING_OR_IMPORT_READY_ONLY");
        asset.put("boundary_note", exists
                ? "Live table is readable; downstream pages still need field-level business validation."
                : "Live table is absent; keep the asset in staging or import-ready documentation.");
        return asset;
    }

    private boolean tableExists(String table) {
        try {
            String[] parts = table.split("\\.", 2);
            String schema = parts.length == 2 ? parts[0] : currentDatabase();
            String tableName = parts.length == 2 ? parts[1] : parts[0];
            Long count = jdbcTemplate.queryForObject(
                    "select count(1) from information_schema.tables where table_schema = ? and table_name = ?",
                    Long.class,
                    schema,
                    tableName
            );
            return count != null && count > 0;
        } catch (DataAccessException ex) {
            return false;
        }
    }

    private Set<String> sourceCodesForRegion(String regionId) {
        Set<String> codes = new LinkedHashSet<>();
        addCodes(codes, "select distinct s.source_code from " + MANGROVE_COVER_TABLE + " c " +
                "join " + DATA_SOURCE_TABLE + " s on s.source_id = c.source_id where cast(c.region_id as char) = ?", regionId);
        addCodes(codes, "select distinct s.source_code from " + REGION_METRIC_TABLE + " m " +
                "join " + DATA_SOURCE_TABLE + " s on s.source_id = m.source_id where cast(m.region_id as char) = ?", regionId);
        addCodes(codes, "select distinct s.source_code from " + LITERATURE_CARBON_TABLE + " l " +
                "join " + DATA_SOURCE_TABLE + " s on s.source_id = l.source_id where cast(l.region_id as char) = ?", regionId);
        return codes;
    }

    private void addCodes(Set<String> codes, String sql, String regionId) {
        try {
            jdbcTemplate.queryForList(sql, String.class, regionId).stream()
                    .filter(code -> code != null && !code.isBlank())
                    .forEach(codes::add);
        } catch (DataAccessException ignored) {
        }
    }

    private Map<String, Object> pagination(long total, Page page) {
        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("total", total);
        pagination.put("limit", page.limit);
        pagination.put("offset", page.offset);
        pagination.put("has_next", page.offset + page.limit < total);
        return pagination;
    }

    private Page page(Map<String, String> filters) {
        filters = safeFilters(filters);
        return new Page(parseBounded(filters.get("limit"), 50, 1, 500), parseBounded(filters.get("offset"), 0, 0, Integer.MAX_VALUE));
    }

    private Map<String, String> safeFilters(Map<String, String> filters) {
        return filters == null ? Map.of() : filters;
    }

    private Long safeCountOrDefault(String table, Long fallback) {
        try {
            return safeCount(table);
        } catch (DataAccessException ex) {
            return fallback;
        }
    }

    private String currentDatabase() {
        return safeScalar("select database()", String.class, "current datasource database");
    }

    private int parseBounded(String raw, int fallback, int min, int max) {
        try {
            int value = Integer.parseInt(String.valueOf(raw));
            return Math.max(min, Math.min(max, value));
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private void addRegion(SqlParts sql, String regionIdColumn, String regionNameColumn, Map<String, String> filters) {
        String regionId = trim(filters.get("region_id"));
        if (regionId != null) {
            sql.where.add("cast(" + regionIdColumn + " as char) = ?");
            sql.params.add(regionId);
        }
        String region = trim(filters.get("region"));
        if (region != null) {
            sql.where.add(regionNameColumn + " like ?");
            sql.params.add("%" + region + "%");
        }
    }

    private void addEquals(SqlParts sql, String column, String raw) {
        String value = trim(raw);
        if (value != null) {
            sql.where.add(column + " = ?");
            sql.params.add(value);
        }
    }

    private void addIntEquals(SqlParts sql, String column, String raw) {
        String value = trim(raw);
        if (value != null) {
            try {
                sql.where.add(column + " = ?");
                sql.params.add(Integer.parseInt(value));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void addLike(SqlParts sql, String keyword, int times) {
        for (int i = 0; i < times; i++) {
            sql.params.add("%" + keyword + "%");
        }
    }

    private List<String> unsupported(Map<String, String> filters, String... keys) {
        List<String> result = new ArrayList<>();
        for (String key : keys) {
            if (trim(filters.get(key)) != null) {
                result.add(key);
            }
        }
        return result;
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            String trimmed = trim(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    private String trim(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        return value.isEmpty() ? null : value;
    }

    private static class SqlParts {
        private String select;
        private final List<String> where = new ArrayList<>();
        private final List<Object> params = new ArrayList<>();
        private String order = "";
    }

    private record Page(int limit, int offset) {
    }
}
