package com.example.jpaspringboot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExternalDatasetServiceTest {

    @TempDir
    Path tempDir;

    private ExternalDatasetService service;

    @BeforeEach
    void setUp() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString(), any(Class.class), any())).thenReturn(List.of());
        service = new ExternalDatasetService(jdbcTemplate, "../data/examples");
    }

    @Test
    void exposesFourDatasetsFromPortableSyntheticSamples() {
        Map<String, Object> response = service.datasets();
        List<Map<String, Object>> datasets = castList(response.get("datasets"));

        assertEquals(4, datasets.size());
        assertEquals(2L, dataset(datasets, "baad").get("recordCount"));
        assertEquals(2L, dataset(datasets, "tallo").get("recordCount"));
        assertEquals(6L, dataset(datasets, "chinallometree").get("recordCount"));
        assertEquals(10L, dataset(datasets, "gwm").get("recordCount"));
    }

    @Test
    void paginatesTalloWithoutLoadingCsvIntoTheClient() {
        Map<String, Object> response = service.records("tallo", "Tallo.csv", 1, 20, null, null, "asc");

        assertEquals(2L, response.get("totalElements"));
        assertEquals(1L, response.get("totalPages"));
        assertEquals(2, castList(response.get("records")).size());
        assertTrue(castList(response.get("columns")).contains("tree_id"));
    }

    @Test
    void rejectsFieldsOutsideTheActualSchema() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.records("baad", "BAAD_cleaned.csv", 1, 20, null, "password", "asc")
        );
        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void resolvesReleaseRootDataFromBackendTargetWorkingDirectory() throws Exception {
        Path releaseRoot = tempDir.resolve("release");
        Path workingDirectory = releaseRoot.resolve("backend/target");
        Path rawRoot = releaseRoot.resolve("data/raw");
        Files.createDirectories(workingDirectory);
        Files.createDirectories(rawRoot);

        assertEquals(rawRoot.toAbsolutePath().normalize(),
                ExternalDatasetService.resolveRawRoot("data/raw", workingDirectory));
    }

    @Test
    void exposesAllAuditedExternalRecordsFromReleaseData() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString(), any(Class.class), any())).thenReturn(List.of());
        ExternalDatasetService releaseService = new ExternalDatasetService(jdbcTemplate, "../data/raw");

        List<Map<String, Object>> datasets = castList(releaseService.datasets().get("datasets"));
        long totalRecords = datasets.stream()
                .mapToLong(item -> ((Number) item.get("recordCount")).longValue())
                .sum();

        assertEquals(4, datasets.size());
        assertEquals(15, datasets.stream().mapToInt(item -> ((Number) item.get("tableCount")).intValue()).sum());
        assertEquals(528_420L, totalRecords);
        assertEquals(21_084L, dataset(datasets, "baad").get("recordCount"));
        assertEquals(7_847L, dataset(datasets, "chinallometree").get("recordCount"));
        assertEquals(651L, dataset(datasets, "gwm").get("recordCount"));
        assertEquals(498_838L, dataset(datasets, "tallo").get("recordCount"));
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> castList(Object value) {
        return (List<T>) value;
    }

    private static Map<String, Object> dataset(List<Map<String, Object>> datasets, String id) {
        return datasets.stream()
                .filter(item -> id.equals(item.get("datasetId")))
                .findFirst()
                .orElseThrow();
    }
}
