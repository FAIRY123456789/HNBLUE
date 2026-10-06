package com.example.jpaspringboot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CarbonAssistantControllerTest {

    @Test
    void reportsLocalRagAsTheOnlyKnowledgeProvider() {
        CarbonAssistantController controller = new CarbonAssistantController(new ObjectMapper());
        ReflectionTestUtils.setField(controller, "localRagEnabled", false);

        Map<String, Object> status = controller.knowledgeStatus();

        assertEquals("hnblue-local-hybrid-rag", status.get("provider"));
        assertEquals(false, status.get("configured"));
        assertEquals("disabled", status.get("status"));
        assertEquals(true, status.get("publicDataFallback"));
    }
}
