package com.example.jpaspringboot.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class V2DataIdentityMetadataTest {

    @Test
    @SuppressWarnings("unchecked")
    void authorizedSchoolModeDoesNotImplyGovernmentReview() {
        V2DataService service = new V2DataService(new JdbcTemplate());
        ReflectionTestUtils.setField(service, "dataMode", "AUTHORIZED_OFFICIAL");
        ReflectionTestUtils.setField(service, "datasetIdentity", "HNBLUE_FORMAL_MYSQL_20261003");
        ReflectionTestUtils.setField(service, "reviewStatus", "material-scope-unproven");
        ReflectionTestUtils.setField(service, "accessStatus", "school-approved");
        ReflectionTestUtils.setField(service, "governmentReviewed", false);

        Map<String, Object> payload = ReflectionTestUtils.invokeMethod(service, "base", "test");
        Map<String, Object> meta = (Map<String, Object>) payload.get("meta");

        assertEquals("AUTHORIZED_OFFICIAL", meta.get("data_mode"));
        assertEquals("HNBLUE_FORMAL_MYSQL_20261003", meta.get("dataset_identity"));
        assertEquals("material-scope-unproven", meta.get("review_status"));
        assertEquals("school-approved", meta.get("access_status"));
        assertFalse((Boolean) meta.get("government_reviewed"));
    }
}
