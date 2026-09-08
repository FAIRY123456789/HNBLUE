package com.example.jpaspringboot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ExternalDatasetService {

    private static final Set<Integer> PAGE_SIZES = Set.of(20, 50, 100);
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z0-9_]+\\z");
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "passwd", "salt", "token", "secret", "api_key", "access_key", "private_key"
    );
    private static final String MYSQL = "MYSQL";
    private static final String CSV = "LOCAL_CSV";

    private final JdbcTemplate jdbcTemplate;
    private final Path rawRoot;
    private final Map<String, DatasetDefinition> definitions;
    private final Map<Path, CsvProfile> profileCache = new ConcurrentHashMap<>();
    private final Map<Path, CsvShape> shapeCache = new ConcurrentHashMap<>();
    private volatile Boolean databaseMetadataReadable;

    @Autowired
    public ExternalDatasetService(
            JdbcTemplate jdbcTemplate,
            @Value("${hnblue.external-data.root:data/raw}") String configuredRoot
    ) {
        this(jdbcTemplate, resolveRawRoot(configuredRoot), buildDefinitions());
    }

    ExternalDatasetService(JdbcTemplate jdbcTemplate, Path rawRoot, Map<String, DatasetDefinition> definitions) {
        this.jdbcTemplate = jdbcTemplate;
        this.rawRoot = rawRoot;
        this.definitions = definitions;
    }

    public Map<String, Object> datasets() {
        List<Map<String, Object>> items = definitions.values().stream()
                .map(this::datasetOverview)
                .toList();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("datasets", items);
        response.put("sourcePriority", List.of("MYSQL_VERIFIED_TABLE", "LOCAL_RAW_FILE"));
        response.put("database", currentDatabase());
        return response;
    }

    public Map<String, Object> tables(String datasetId) {
        DatasetDefinition dataset = requireDataset(datasetId);
        List<Map<String, Object>> tables = runtimeTables(dataset).stream().map(this::tableSummary).toList();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("datasetId", dataset.id());
        response.put("tables", tables);
        response.put("tableCount", tables.size());
        return response;
    }

    public Map<String, Object> schema(String datasetId, String tableName, int page, int size, String keyword) {
        DatasetDefinition dataset = requireDataset(datasetId);
        RuntimeTable table = requireTable(dataset, tableName);
        int safePage = Math.max(page, 1);
        int safeSize = normalizeSize(size);
        String normalizedKeyword = trim(keyword);
        List<Map<String, Object>> columns = schemaFor(table);
        if (normalizedKeyword != null) {
            String needle = normalizedKeyword.toLowerCase(Locale.ROOT);
            columns = columns.stream().filter(column -> column.values().stream()
                    .filter(value -> value != null)
                    .map(String::valueOf)
                    .map(value -> value.toLowerCase(Locale.ROOT))
                    .anyMatch(value -> value.contains(needle))).toList();
        }
        long total = columns.size();
        int from = (int) Math.min((long) (safePage - 1) * safeSize, total);
        int to = (int) Math.min((long) from + safeSize, total);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("datasetId", dataset.id());
        response.put("tableName", table.apiName());
        response.put("page", safePage);
        response.put("size", safeSize);
        response.put("totalElements", total);
        response.put("totalPages", pages(total, safeSize));
        response.put("columns", columns.subList(from, to));
        response.put("source", sourceMap(dataset, table));
        return response;
    }

    public Map<String, Object> records(
            String datasetId,
            String tableName,
            int page,
            int size,
            String keyword,
            String sortField,
            String sortDirection
    ) {
        DatasetDefinition dataset = requireDataset(datasetId);
        RuntimeTable table = requireTable(dataset, tableName);
        int safePage = Math.max(page, 1);
        int safeSize = normalizeSize(size);
        boolean descending = "desc".equalsIgnoreCase(sortDirection);
        RecordPage recordPage = MYSQL.equals(table.sourceType())
                ? queryDatabase(table, safePage, safeSize, keyword, sortField, descending)
                : queryCsv(table, safePage, safeSize, keyword, sortField, descending);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("datasetId", dataset.id());
        response.put("tableName", table.apiName());
        response.put("page", safePage);
        response.put("size", safeSize);
        response.put("totalElements", recordPage.total());
        response.put("totalPages", pages(recordPage.total(), safeSize));
        response.put("columns", recordPage.columns());
        response.put("records", recordPage.records());
        response.put("source", sourceMap(dataset, table));
        return response;
    }

    private Map<String, Object> datasetOverview(DatasetDefinition dataset) {
        List<RuntimeTable> tables = runtimeTables(dataset);
        long records = tables.stream().mapToLong(this::countRows).sum();
        int fieldCount = tables.stream().mapToInt(this::columnCount).max().orElse(0);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("datasetId", dataset.id());
        item.put("name", dataset.name());
        item.put("fullName", dataset.fullName());
        item.put("description", dataset.description());
        item.put("tableCount", tables.size());
        item.put("recordCount", records);
        item.put("fieldCount", fieldCount);
        item.put("keyFieldCount", Math.min(dataset.keyFieldCount(), fieldCount));
        item.put("source", dataset.sourceOrganization());
        item.put("usage", dataset.usage());
        item.put("sourceUrl", dataset.sourceUrl());
        return item;
    }

    private List<RuntimeTable> runtimeTables(DatasetDefinition dataset) {
        List<RuntimeTable> result = new ArrayList<>();
        for (TableDefinition table : dataset.tables()) {
            String databaseTable = findDatabaseTable(table.databaseCandidates());
            if (databaseTable != null) {
                result.add(new RuntimeTable(table, MYSQL, databaseTable, null));
            } else {
                Path path = rawRoot.resolve(table.relativeFile()).normalize();
                if (!path.startsWith(rawRoot.normalize()) || !Files.isRegularFile(path)) {
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "外部数据文件不存在：" + table.relativeFile());
                }
                result.add(new RuntimeTable(table, CSV, path.getFileName().toString(), path));
            }
        }
        return result;
    }

    private RuntimeTable requireTable(DatasetDefinition dataset, String requested) {
        String key = trim(requested);
        if (key == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tableName 不能为空");
        }
        return runtimeTables(dataset).stream()
                .filter(table -> key.equalsIgnoreCase(table.definition().id())
                        || key.equalsIgnoreCase(table.apiName())
                        || key.equalsIgnoreCase(Path.of(table.definition().relativeFile()).getFileName().toString()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "数据表不存在：" + requested));
    }

    private DatasetDefinition requireDataset(String id) {
        DatasetDefinition dataset = definitions.get(String.valueOf(id).toLowerCase(Locale.ROOT));
        if (dataset == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "外部数据集不存在：" + id);
        }
        return dataset;
    }

    private Map<String, Object> tableSummary(RuntimeTable table) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("tableId", table.definition().id());
        item.put("tableName", table.apiName());
        item.put("displayName", table.definition().displayName());
        item.put("recordCount", countRows(table));
        item.put("fieldCount", columnCount(table));
        item.put("sourceType", table.sourceType());
        item.put("sourceFile", table.path() == null ? null : relativePath(table.path()));
        return item;
    }

    private List<Map<String, Object>> schemaFor(RuntimeTable table) {
        if (MYSQL.equals(table.sourceType())) {
            return databaseSchema(table).stream().map(column -> fieldDictionary(table, column)).toList();
        }
        CsvProfile profile = csvProfile(table);
        List<Map<String, Object>> result = new ArrayList<>();
        for (CsvColumn column : profile.columns()) {
            result.add(fieldDictionary(table, new ColumnInfo(column.name(), column.dataType(), column.nullable(), null)));
        }
        return result;
    }

    private Map<String, Object> fieldDictionary(RuntimeTable table, ColumnInfo column) {
        FieldDefinition curated = FIELD_DICTIONARY.get(column.name().toLowerCase(Locale.ROOT));
        String displayName = curated == null ? humanize(column.name()) : curated.displayName();
        String unit = curated == null ? inferUnit(column.name()) : curated.unit();
        String description = curated == null ? "源数据字段“" + column.name() + "”，保留原始表定义。" : curated.description();
        String category = curated == null ? inferCategory(column.name()) : curated.category();
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("columnName", column.name());
        field.put("displayNameCn", displayName);
        field.put("dataType", normalizeDataType(column.dataType()));
        field.put("unit", unit);
        field.put("description", description);
        field.put("category", category);
        field.put("displayPriority", curated == null ? 100 : curated.priority());
        field.put("nullable", column.nullable());
        field.put("sourceTable", table.apiName());
        return field;
    }

    private RecordPage queryDatabase(
            RuntimeTable table, int page, int size, String keyword, String sortField, boolean descending
    ) {
        List<ColumnInfo> schema = databaseSchema(table);
        Set<String> allowed = schema.stream().map(ColumnInfo::name).collect(Collectors.toCollection(LinkedHashSet::new));
        String tableSql = quoted(table.apiName());
        String normalizedKeyword = trim(keyword);
        List<Object> params = new ArrayList<>();
        String where = "";
        if (normalizedKeyword != null && !allowed.isEmpty()) {
            where = " where " + allowed.stream()
                    .map(column -> "cast(" + quoted(column) + " as char) like ?")
                    .collect(Collectors.joining(" or "));
            for (int i = 0; i < allowed.size(); i++) params.add("%" + normalizedKeyword + "%");
        }
        Long total = jdbcTemplate.queryForObject("select count(1) from " + tableSql + where, Long.class, params.toArray());
        String order = "";
        String normalizedSort = trim(sortField);
        if (normalizedSort != null) {
            if (!allowed.contains(normalizedSort)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不可排序字段：" + normalizedSort);
            }
            order = " order by " + quoted(normalizedSort) + (descending ? " desc" : " asc");
        }
        List<Object> queryParams = new ArrayList<>(params);
        queryParams.add(size);
        queryParams.add((page - 1L) * size);
        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                "select " + allowed.stream().map(this::quoted).collect(Collectors.joining(","))
                        + " from " + tableSql + where + order + " limit ? offset ?",
                queryParams.toArray()
        ).stream().map(this::sanitizeDatabaseRow).toList();
        return new RecordPage(total == null ? 0 : total, new ArrayList<>(allowed), records);
    }

    private RecordPage queryCsv(
            RuntimeTable table, int page, int size, String keyword, String sortField, boolean descending
    ) {
        List<String> columns = csvShape(table).columns();
        String normalizedSort = trim(sortField);
        int sortIndex = -1;
        if (normalizedSort != null) {
            sortIndex = indexOfIgnoreCase(columns, normalizedSort);
            if (sortIndex < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不可排序字段：" + normalizedSort);
            }
        }
        String needle = trim(keyword);
        if (needle != null) needle = needle.toLowerCase(Locale.ROOT);
        long offset = (page - 1L) * size;
        List<IndexedCsvRow> sortedRows = sortIndex >= 0 ? new ArrayList<>() : null;
        List<Map<String, Object>> pageRows = new ArrayList<>();
        long matched = 0;
        try (CsvReader reader = new CsvReader(table.path(), table.definition().charset())) {
            reader.readRecord();
            List<String> row;
            while ((row = reader.readRecord()) != null) {
                if (isBlankRecord(row)) continue;
                if (!matches(row, needle)) continue;
                if (sortIndex >= 0) {
                    sortedRows.add(new IndexedCsvRow(matched, normalizeRow(row, columns.size()), rowValue(row, sortIndex)));
                } else if (matched >= offset && pageRows.size() < size) {
                    pageRows.add(toRecord(columns, row));
                }
                matched++;
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "读取外部数据文件失败", exception);
        }
        if (sortedRows != null) {
            Comparator<IndexedCsvRow> comparator = Comparator
                    .comparing(IndexedCsvRow::sortValue, ExternalDatasetService::compareValues)
                    .thenComparingLong(IndexedCsvRow::ordinal);
            if (descending) comparator = comparator.reversed();
            sortedRows.sort(comparator);
            int from = (int) Math.min(offset, sortedRows.size());
            int to = Math.min(from + size, sortedRows.size());
            for (IndexedCsvRow row : sortedRows.subList(from, to)) {
                pageRows.add(toRecord(columns, row.values()));
            }
        }
        return new RecordPage(matched, columns, pageRows);
    }

    private CsvProfile csvProfile(RuntimeTable table) {
        return profileCache.computeIfAbsent(table.path(), path -> {
            try (CsvReader reader = new CsvReader(path, table.definition().charset())) {
                List<String> header = reader.readRecord();
                if (header == null) return new CsvProfile(List.of(), 0);
                header = deduplicateHeaders(header);
                int width = header.size();
                long[] populated = new long[width];
                long[] numeric = new long[width];
                long[] missing = new long[width];
                long count = 0;
                List<String> row;
                while ((row = reader.readRecord()) != null) {
                    if (isBlankRecord(row)) continue;
                    count++;
                    for (int index = 0; index < width; index++) {
                        String value = rowValue(row, index).trim();
                        if (isMissing(value)) missing[index]++;
                        else {
                            populated[index]++;
                            if (isNumber(value)) numeric[index]++;
                        }
                    }
                }
                List<CsvColumn> columns = new ArrayList<>();
                for (int index = 0; index < width; index++) {
                    boolean numericColumn = populated[index] > 0 && numeric[index] * 100 >= populated[index] * 95;
                    columns.add(new CsvColumn(header.get(index), numericColumn ? "DECIMAL" : "TEXT", missing[index] > 0));
                }
                return new CsvProfile(columns, count);
            } catch (IOException exception) {
                throw new IllegalStateException("无法审计外部数据文件：" + path, exception);
            }
        });
    }

    private CsvShape csvShape(RuntimeTable table) {
        return shapeCache.computeIfAbsent(table.path(), path -> {
            try (CsvReader csvReader = new CsvReader(path, table.definition().charset())) {
                List<String> rawHeader = csvReader.readRecord();
                List<String> header = rawHeader == null ? List.of() : deduplicateHeaders(rawHeader);
                return new CsvShape(header, countCsvRecords(path, table.definition().charset()));
            } catch (IOException exception) {
                throw new IllegalStateException("无法读取外部数据文件结构：" + path, exception);
            }
        });
    }

    private static long countCsvRecords(Path path, Charset charset) throws IOException {
        long records = 0;
        try (CsvReader csvReader = new CsvReader(path, charset)) {
            if (csvReader.readRecord() == null) return 0;
            List<String> row;
            while ((row = csvReader.readRecord()) != null) {
                if (!isBlankRecord(row)) records++;
            }
        }
        return records;
    }

    private static boolean isBlankRecord(List<String> row) {
        return row.isEmpty() || row.stream().allMatch(String::isBlank);
    }

    private int columnCount(RuntimeTable table) {
        return MYSQL.equals(table.sourceType()) ? databaseSchema(table).size() : csvShape(table).columns().size();
    }
    private List<ColumnInfo> databaseSchema(RuntimeTable table) {
        return jdbcTemplate.query(
                "select column_name, data_type, is_nullable, column_key from information_schema.columns "
                        + "where table_schema = database() and table_name = ? order by ordinal_position",
                (resultSet, rowNum) -> new ColumnInfo(
                        resultSet.getString("column_name"),
                        resultSet.getString("data_type"),
                        "YES".equalsIgnoreCase(resultSet.getString("is_nullable")),
                        resultSet.getString("column_key")
                ),
                table.apiName()
        ).stream().filter(column -> !isSensitive(column.name())).toList();
    }

    private long countRows(RuntimeTable table) {
        if (CSV.equals(table.sourceType())) return csvShape(table).recordCount();
        Long count = jdbcTemplate.queryForObject("select count(1) from " + quoted(table.apiName()), Long.class);
        return count == null ? 0 : count;
    }

    private String findDatabaseTable(List<String> candidates) {
        if (Boolean.FALSE.equals(databaseMetadataReadable)) return null;
        try {
            for (String candidate : candidates) {
                if (!SAFE_IDENTIFIER.matcher(candidate).matches()) continue;
                List<String> matches = jdbcTemplate.queryForList(
                        "select table_name from information_schema.tables "
                                + "where table_schema = database() and lower(table_name) = lower(?) limit 1",
                        String.class,
                        candidate
                );
                databaseMetadataReadable = true;
                if (!matches.isEmpty() && SAFE_IDENTIFIER.matcher(matches.get(0)).matches()) return matches.get(0);
            }
        } catch (DataAccessException ignored) {
            // A missing runtime credential or unavailable database activates the documented local-file fallback.
            databaseMetadataReadable = false;
        }
        return null;
    }

    private Map<String, Object> sourceMap(DatasetDefinition dataset, RuntimeTable table) {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("type", table.sourceType());
        source.put("database", MYSQL.equals(table.sourceType()) ? currentDatabase() : null);
        source.put("table", MYSQL.equals(table.sourceType()) ? table.apiName() : null);
        source.put("file", table.path() == null ? null : relativePath(table.path()));
        source.put("organization", dataset.sourceOrganization());
        source.put("url", dataset.sourceUrl());
        return source;
    }

    private String currentDatabase() {
        if (Boolean.FALSE.equals(databaseMetadataReadable)) return "不可用（使用本地原始文件）";
        try {
            String database = jdbcTemplate.queryForObject("select database()", String.class);
            databaseMetadataReadable = true;
            return database;
        } catch (DataAccessException exception) {
            databaseMetadataReadable = false;
            return "不可用（使用本地原始文件）";
        }
    }

    private Map<String, Object> sanitizeDatabaseRow(Map<String, Object> row) {
        Map<String, Object> safe = new LinkedHashMap<>();
        row.forEach((key, value) -> {
            if (!isSensitive(key)) safe.put(key, normalizeDatabaseValue(value));
        });
        return safe;
    }

    private Object normalizeDatabaseValue(Object value) {
        if (value instanceof byte[] bytes) return "[binary " + bytes.length + " bytes]";
        if (value instanceof Timestamp timestamp) return timestamp.toInstant().toString();
        return value;
    }

    private boolean isSensitive(String column) {
        String normalized = column.toLowerCase(Locale.ROOT);
        return SENSITIVE_FIELDS.contains(normalized)
                || normalized.endsWith("_password")
                || normalized.endsWith("_token")
                || normalized.endsWith("_secret");
    }

    private String quoted(String identifier) {
        if (!SAFE_IDENTIFIER.matcher(identifier).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "非法字段或表名");
        }
        return "`" + identifier + "`";
    }

    private Map<String, Object> toRecord(List<String> columns, List<String> row) {
        Map<String, Object> record = new LinkedHashMap<>();
        for (int index = 0; index < columns.size(); index++) {
            String value = rowValue(row, index).trim();
            record.put(columns.get(index), isMissing(value) ? null : value);
        }
        return record;
    }

    private static boolean matches(List<String> row, String needle) {
        if (needle == null) return true;
        return row.stream().anyMatch(value -> value.toLowerCase(Locale.ROOT).contains(needle));
    }

    private static List<String> normalizeRow(List<String> row, int size) {
        List<String> result = new ArrayList<>(size);
        for (int index = 0; index < size; index++) result.add(rowValue(row, index));
        return result;
    }

    private static String rowValue(List<String> row, int index) {
        return index < row.size() ? row.get(index) : "";
    }

    private static int compareValues(String left, String right) {
        BigDecimal leftNumber = decimal(left);
        BigDecimal rightNumber = decimal(right);
        if (leftNumber != null && rightNumber != null) return leftNumber.compareTo(rightNumber);
        return left.compareToIgnoreCase(right);
    }

    private static BigDecimal decimal(String value) {
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static int indexOfIgnoreCase(List<String> columns, String requested) {
        for (int index = 0; index < columns.size(); index++) {
            if (columns.get(index).equalsIgnoreCase(requested)) return index;
        }
        return -1;
    }

    private static boolean isNumber(String value) {
        return decimal(value) != null;
    }

    private static boolean isMissing(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() || Set.of("na", "n/a", "null", "none", "nan", "-9999").contains(normalized);
    }

    private static int normalizeSize(int size) {
        return PAGE_SIZES.contains(size) ? size : 20;
    }

    private static long pages(long total, int size) {
        return total == 0 ? 0 : (total + size - 1) / size;
    }

    private static String normalizeDataType(String type) {
        String normalized = String.valueOf(type).toLowerCase(Locale.ROOT);
        if (normalized.contains("int") || normalized.contains("decimal") || normalized.contains("double")
                || normalized.contains("float") || normalized.contains("number")) return "数值";
        if (normalized.contains("date") || normalized.contains("time")) return "日期时间";
        if (normalized.contains("bool") || normalized.contains("bit")) return "布尔";
        return "文本";
    }

    private static String inferUnit(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        if (normalized.contains("percent")) return "%";
        if (normalized.endsWith("_cm") || normalized.contains("diameter range")) return "cm";
        if (normalized.endsWith("_m") || normalized.contains("height range")) return "m";
        if (normalized.contains("area")) return "km²";
        if (normalized.equals("mat")) return "°C";
        if (normalized.equals("map")) return "mm/year";
        return "—";
    }

    private static String inferCategory(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        if (normalized.contains("lat") || normalized.contains("lon") || normalized.contains("country")
                || normalized.contains("province") || normalized.contains("location") || normalized.contains("site")) return "地理";
        if (normalized.contains("species") || normalized.contains("family") || normalized.contains("genus")
                || normalized.contains("division")) return "分类";
        if (normalized.contains("height") || normalized.contains("diameter") || normalized.startsWith("d.")
                || normalized.startsWith("h.") || normalized.contains("crown")) return "尺寸";
        if (normalized.startsWith("m.") || normalized.contains("biomass")) return "生物量";
        if (normalized.contains("coeff") || normalized.equals("r2") || normalized.equals("r") || normalized.equals("cf")) return "方程";
        return "背景";
    }

    private static String humanize(String name) {
        return name.replace('_', ' ').replace('.', ' ').trim();
    }

    private String relativePath(Path path) {
        Path working = Paths.get("").toAbsolutePath().normalize();
        Path absolute = path.toAbsolutePath().normalize();
        if (absolute.startsWith(working)) return working.relativize(absolute).toString().replace('\\', '/');
        return absolute.toString().replace('\\', '/');
    }

    private static String trim(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private static Path resolveRawRoot(String configuredRoot) {
        Path working = Paths.get("").toAbsolutePath().normalize();
        Path configured = Paths.get(configuredRoot);
        if (!configured.isAbsolute()) configured = working.resolve(configured).normalize();
        if (Files.isDirectory(configured)) return configured;
        Path parentFallback = working.resolve("../data/raw").normalize();
        if (Files.isDirectory(parentFallback)) return parentFallback;
        Path exampleRoot = working.resolve("data/examples").normalize();
        if (Files.isDirectory(exampleRoot)) return exampleRoot;
        Path parentExampleRoot = working.resolve("../data/examples").normalize();
        return Files.isDirectory(parentExampleRoot) ? parentExampleRoot : configured;
    }

    private static List<String> deduplicateHeaders(List<String> rawHeader) {
        List<String> result = new ArrayList<>();
        Map<String, Integer> counts = new HashMap<>();
        for (int index = 0; index < rawHeader.size(); index++) {
            String header = rawHeader.get(index).replace("\ufeff", "").trim();
            if (header.isEmpty()) header = "column_" + (index + 1);
            int count = counts.merge(header, 1, Integer::sum);
            result.add(count == 1 ? header : header + "_" + count);
        }
        return result;
    }

    private static Map<String, DatasetDefinition> buildDefinitions() {
        Map<String, DatasetDefinition> items = new LinkedHashMap<>();
        items.put("baad", new DatasetDefinition(
                "baad", "BAAD", "Biomass And Allometry Database for woody plants",
                "全球木本植物尺寸、生物量、器官和环境字段记录。",
                "Ecological Society of America · Wiley Online Library", "https://esajournals.onlinelibrary.wiley.com/doi/abs/10.1890/14-1889.1",
                "理解树木尺寸与生物量关系、解释模型变量和异速生长研究背景。", 25,
                List.of(table("baad_cleaned", "BAAD 清洗记录", "baad/BAAD_cleaned.csv", StandardCharsets.UTF_8,
                        "external_baad_record", "baad_cleaned", "baad_record", "t_baad_record"))
        ));
        items.put("tallo", new DatasetDefinition(
                "tallo", "Tallo", "Tallo: A global tree allometry and crown architecture database",
                "全球树木胸径、树高、冠幅、分类与地理记录。",
                "Tallo consortium / Zenodo", "https://doi.org/10.5281/zenodo.6637599",
                "支持树木结构参数理解、异速生长背景查询和遥感结构变量解释。", 13,
                List.of(table("tallo", "Tallo 树木记录", "tallo/Tallo.csv", StandardCharsets.UTF_8,
                        "external_tallo_record", "tallo", "tallo_record", "t_tallo_record"))
        ));
        items.put("chinallometree", new DatasetDefinition(
                "chinallometree", "ChinAllomeTree", "China's normalized tree biomass equation dataset",
                "中国树木样本背景、整理记录与生物量异速生长方程。",
                "PANGAEA / ChinAllomeTree authors", "https://doi.pangaea.de/10.1594/PANGAEA.895244",
                "查询中国树木样本背景、适用对象、方程形式与参数。", 17,
                List.of(
                        table("chinallometree_cleaned", "清洗数据", "chinallometree/ChinAllomeTree_cleaned.csv", StandardCharsets.UTF_8,
                                "external_chinallometree_cleaned", "chinallometree_cleaned", "t_chinallometree_cleaned"),
                        table("chinallometree_general", "样本与背景信息", "chinallometree/ChinAllomeTree.xlsx - General.csv", Charset.forName("GB18030"),
                                "external_chinallometree_general", "chinallometree_general", "t_chinallometree_general"),
                        table("chinallometree_equation", "异速生长方程", "chinallometree/ChinAllomeTree.xlsx - Equation.csv", Charset.forName("GB18030"),
                                "external_chinallometree_equation", "chinallometree_equation", "t_chinallometree_equation")
                )
        ));
        List<TableDefinition> gwmTables = new ArrayList<>();
        for (String ecosystem : List.of("Mangrove", "Seagrass", "Saltmarsh", "Coral_Reef", "Cold_Coral")) {
            for (String level : List.of("country", "global")) {
                String id = (ecosystem + "_" + level).toLowerCase(Locale.ROOT);
                gwmTables.add(table(id, gwmDisplayName(ecosystem, level), "gwm/" + ecosystem + "_" + level + ".csv", StandardCharsets.UTF_8,
                        "external_gwm_" + id, "gwm_" + id, "external_gwm_statistic"));
            }
        }
        items.put("gwm", new DatasetDefinition(
                "gwm", "GWM", "Ocean+ Habitats global coastal and marine habitat statistics",
                "红树林、海草床、盐沼、珊瑚礁和冷水珊瑚的国家级与全球统计。",
                "UNEP-WCMC Ocean+ Habitats", "https://habitats.oceanplus.org/",
                "浏览不同海岸与海洋生态类型的面积及保护覆盖统计，不混合为同一蓝碳口径。", 4,
                List.copyOf(gwmTables)
        ));
        return Collections.unmodifiableMap(items);
    }

    private static String gwmDisplayName(String ecosystem, String level) {
        Map<String, String> names = Map.of(
                "Mangrove", "红树林", "Seagrass", "海草床", "Saltmarsh", "盐沼",
                "Coral_Reef", "珊瑚礁", "Cold_Coral", "冷水珊瑚"
        );
        return names.get(ecosystem) + ("country".equals(level) ? " · 国家/地区" : " · 全球汇总");
    }

    private static TableDefinition table(
            String id, String displayName, String relativeFile, Charset charset, String... databaseCandidates
    ) {
        return new TableDefinition(id, displayName, relativeFile, charset, List.of(databaseCandidates));
    }

    private static final Map<String, FieldDefinition> FIELD_DICTIONARY = buildFieldDictionary();

    private static Map<String, FieldDefinition> buildFieldDictionary() {
        Map<String, FieldDefinition> fields = new HashMap<>();
        add(fields, "studyname", "研究名称", "—", "BAAD 原始研究或数据贡献单元名称。", "来源", 1);
        add(fields, "location", "采样地点", "—", "研究记录对应的地点描述。", "地理", 2);
        add(fields, "latitude", "纬度", "°", "十进制度纬度。", "地理", 3);
        add(fields, "longitude", "经度", "°", "十进制度经度。", "地理", 4);
        add(fields, "vegetation", "植被类型", "—", "记录所在植被或生态系统类型。", "环境", 5);
        add(fields, "map", "年平均降水量", "mm/year", "采样地点年平均降水量。", "环境", 6);
        add(fields, "mat", "年平均温度", "°C", "采样地点年平均温度。", "环境", 7);
        add(fields, "species", "物种", "—", "源记录中的物种名称。", "分类", 8);
        add(fields, "speciesmatched", "标准化物种名", "—", "经分类学匹配后的物种名称。", "分类", 9);
        add(fields, "family", "科", "—", "植物科名。", "分类", 10);
        add(fields, "genus", "属", "—", "植物属名。", "分类", 10);
        add(fields, "division", "植物类群", "—", "Tallo 使用的高阶植物分类。", "分类", 11);
        add(fields, "pft", "植物功能型", "—", "Plant functional type。", "分类", 12);
        add(fields, "age", "年龄", "year", "植株或林分年龄。", "背景", 13);
        add(fields, "a.lf", "总叶面积", "m²", "植株叶片总面积。", "器官", 20);
        add(fields, "a.cp", "冠层投影面积", "m²", "树冠水平投影面积。", "尺寸", 21);
        add(fields, "h.t", "树高", "m", "植株总高度。", "尺寸", 22);
        add(fields, "d.ba", "基径", "m", "茎干基部直径。", "尺寸", 23);
        add(fields, "d.bh", "胸径", "m", "胸高位置的茎干直径。", "尺寸", 24);
        add(fields, "d.cr", "冠径", "m", "树冠直径。", "尺寸", 25);
        add(fields, "m.lf", "叶生物量", "kg", "叶片干生物量。", "生物量", 30);
        add(fields, "m.st", "茎生物量", "kg", "茎干干生物量。", "生物量", 31);
        add(fields, "m.so", "地上生物量", "kg", "植株地上部分干生物量。", "生物量", 32);
        add(fields, "m.br", "枝生物量", "kg", "枝条干生物量。", "生物量", 33);
        add(fields, "m.rt", "根生物量", "kg", "根系干生物量。", "生物量", 34);
        add(fields, "m.to", "总生物量", "kg", "植株总干生物量。", "生物量", 35);
        add(fields, "tree_id", "树木记录编号", "—", "Tallo 树木记录标识。", "标识", 1);
        add(fields, "stem_diameter_cm", "茎径", "cm", "树干直径记录。", "尺寸", 2);
        add(fields, "height_m", "树高", "m", "树木总高度。", "尺寸", 3);
        add(fields, "crown_radius_m", "冠幅半径", "m", "树冠水平半径。", "尺寸", 4);
        add(fields, "height_outlier", "树高离群标记", "—", "Tallo 对树高记录给出的离群标识。", "质量", 5);
        add(fields, "crown_radius_outlier", "冠幅离群标记", "—", "Tallo 对冠幅记录给出的离群标识。", "质量", 6);
        add(fields, "reference_id", "来源编号", "—", "关联 Tallo 文献来源表的编号。", "来源", 7);
        add(fields, "id", "记录编号", "—", "源数据记录或样本编号。", "标识", 1);
        add(fields, "province", "省份", "—", "中国样本所在省份。", "地理", 2);
        add(fields, "study site", "研究地点", "—", "样本或方程对应的研究地点。", "地理", 3);
        add(fields, "altitude", "海拔", "m", "研究地点海拔。", "环境", 6);
        add(fields, "forest type", "森林类型", "—", "林分或森林类型。", "环境", 7);
        add(fields, "dominant species", "优势物种", "—", "样本林分优势物种。", "分类", 8);
        add(fields, "stand origin", "林分起源", "—", "天然林或人工林等林分起源。", "背景", 9);
        add(fields, "stand age", "林龄", "year", "林分年龄。", "背景", 10);
        add(fields, "tree spacing", "株行距", "m", "树木种植或分布间距。", "背景", 11);
        add(fields, "sources", "文献来源", "—", "样本背景或方程的原始文献来源。", "来源", 12);
        add(fields, "equation number", "方程编号", "—", "同一背景记录中的方程序号。", "标识", 2);
        add(fields, "tree species", "树种", "—", "方程适用树种。", "分类", 3);
        add(fields, "tree component", "树木组分", "—", "方程估算的生物量组分。", "器官", 4);
        add(fields, "predictor variable", "预测变量", "—", "异速生长方程使用的自变量。", "方程", 5);
        add(fields, "equation form", "方程形式", "—", "异速生长方程的数学形式。", "方程", 6);
        add(fields, "coeff. a", "系数 a", "依方程", "方程参数 a。", "方程", 7);
        add(fields, "coeff. b", "系数 b", "依方程", "方程参数 b。", "方程", 8);
        add(fields, "coeff. c", "系数 c", "依方程", "方程参数 c。", "方程", 9);
        add(fields, "coeff. d", "系数 d", "依方程", "方程参数 d。", "方程", 10);
        add(fields, "n", "样本量", "record", "拟合方程使用的样本数量。", "质量", 11);
        add(fields, "r2", "决定系数 R²", "—", "方程拟合优度。", "质量", 12);
        add(fields, "r", "相关系数 R", "—", "方程相关系数。", "质量", 13);
        add(fields, "cf", "校正因子", "—", "对数方程回算等场景使用的校正因子。", "方程", 14);
        add(fields, "method", "方法", "—", "方程拟合或数据处理方法。", "方程", 15);
        add(fields, "diameter range", "直径适用范围", "cm", "方程适用的树径范围。", "范围", 16);
        add(fields, "height range", "树高适用范围", "m", "方程适用的树高范围。", "范围", 17);
        add(fields, "country", "国家或地区代码", "ISO 3166-1 alpha-3", "国家或地区三字母代码。", "地理", 1);
        add(fields, "total_area", "生态系统总面积", "km²", "对应生态类型的面积统计。", "统计", 2);
        add(fields, "total_area_2020", "2020 年生态系统总面积", "km²", "2020 年红树林面积统计。", "统计", 2);
        add(fields, "protected_area", "保护覆盖面积", "km²", "与保护地或保护措施范围重叠的面积。", "统计", 3);
        add(fields, "percent_protected", "保护覆盖比例", "%", "保护覆盖面积占总面积的比例。", "统计", 4);
        return Collections.unmodifiableMap(fields);
    }

    private static void add(
            Map<String, FieldDefinition> fields,
            String name, String displayName, String unit, String description, String category, int priority
    ) {
        fields.put(name.toLowerCase(Locale.ROOT), new FieldDefinition(displayName, unit, description, category, priority));
    }

    record DatasetDefinition(
            String id, String name, String fullName, String description,
            String sourceOrganization, String sourceUrl, String usage, int keyFieldCount,
            List<TableDefinition> tables
    ) {
    }

    record TableDefinition(
            String id, String displayName, String relativeFile, Charset charset, List<String> databaseCandidates
    ) {
    }

    record RuntimeTable(TableDefinition definition, String sourceType, String apiName, Path path) {
    }

    record CsvColumn(String name, String dataType, boolean nullable) {
    }

    record CsvProfile(List<CsvColumn> columns, long recordCount) {
    }

    record CsvShape(List<String> columns, long recordCount) {
    }

    record ColumnInfo(String name, String dataType, boolean nullable, String keyType) {
    }

    record RecordPage(long total, List<String> columns, List<Map<String, Object>> records) {
    }

    record IndexedCsvRow(long ordinal, List<String> values, String sortValue) {
    }

    record FieldDefinition(String displayName, String unit, String description, String category, int priority) {
    }

    static final class CsvReader implements AutoCloseable {
        private final PushbackReader reader;
        private boolean firstCharacter = true;

        CsvReader(Path path, Charset charset) throws IOException {
            BufferedReader buffered = Files.newBufferedReader(path, charset);
            this.reader = new PushbackReader(buffered, 2);
        }

        List<String> readRecord() throws IOException {
            List<String> cells = new ArrayList<>();
            StringBuilder cell = new StringBuilder();
            boolean quoted = false;
            boolean sawCharacter = false;
            while (true) {
                int current = reader.read();
                if (current < 0) {
                    if (!sawCharacter && cells.isEmpty() && cell.isEmpty()) return null;
                    cells.add(clean(cell.toString()));
                    return cells;
                }
                char character = (char) current;
                if (firstCharacter) {
                    firstCharacter = false;
                    if (character == '\ufeff') continue;
                }
                sawCharacter = true;
                if (character == '"') {
                    if (quoted) {
                        int next = reader.read();
                        if (next == '"') cell.append('"');
                        else {
                            quoted = false;
                            if (next >= 0) reader.unread(next);
                        }
                    } else if (cell.isEmpty()) {
                        quoted = true;
                    } else {
                        cell.append(character);
                    }
                } else if (character == ',' && !quoted) {
                    cells.add(clean(cell.toString()));
                    cell.setLength(0);
                } else if ((character == '\n' || character == '\r') && !quoted) {
                    if (character == '\r') {
                        int next = reader.read();
                        if (next != '\n' && next >= 0) reader.unread(next);
                    }
                    cells.add(clean(cell.toString()));
                    return cells;
                } else {
                    cell.append(character);
                }
            }
        }

        private String clean(String value) {
            return value.replace("\u0000", "").trim();
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }
    }
}
