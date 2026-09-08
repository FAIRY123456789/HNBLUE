package com.example.jpaspringboot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

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
