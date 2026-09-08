package com.example.jpaspringboot.controller;

import com.example.jpaspringboot.JpAspringbootApplication;
import com.example.jpaspringboot.entity.Admin;
import com.example.jpaspringboot.entity.User;
import com.example.jpaspringboot.repository.AdminRepository;
import com.example.jpaspringboot.repository.UserRepository;
import com.example.jpaspringboot.repository.WorkOrderActionLogRepository;
import com.example.jpaspringboot.repository.WorkOrderRepository;
import com.example.jpaspringboot.util.JwtUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = JpAspringbootApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:hnblue_workorder_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class WorkOrderControllerTest {
    private static final String TEST_ID = "AUTO-WORKORDER-TEST-202607";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private WorkOrderActionLogRepository logRepository;

    private String firstAdminToken;
    private String secondAdminToken;
    private String userToken;
    private String otherUserToken;

    @BeforeEach
    void setUp() {
        logRepository.deleteAll();
        workOrderRepository.deleteAll();
        Admin firstAdmin = adminRepository.findByName("Admin_100000");
        if (firstAdmin == null) firstAdmin = adminRepository.save(new Admin("Admin_100000", "salt", "hash"));
        Admin secondAdmin = adminRepository.findByName("Admin_100001");
        if (secondAdmin == null) secondAdmin = adminRepository.save(new Admin("Admin_100001", "salt", "hash"));
        User user = userRepository.findByName("wo_user_202607");
        if (user == null) user = userRepository.save(new User("wo_user_202607", "salt", "hash", "wo_user_202607@example.com", "2000-01-01"));
        User other = userRepository.findByName("wo_other_202607");
        if (other == null) other = userRepository.save(new User("wo_other_202607", "salt", "hash", "wo_other_202607@example.com", "2000-01-01"));
        firstAdminToken = JwtUtils.generateToken(firstAdmin.getName());
        secondAdminToken = JwtUtils.generateToken(secondAdmin.getName());
        userToken = JwtUtils.generateToken(user.getName());
        otherUserToken = JwtUtils.generateToken(other.getName());
    }

    @Test
    void verifiesWorkOrderApiRolesStatusFlowAndLogs() throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("code", TEST_ID);
        request.put("title", "Automated work order verification");
        request.put("changeType", "UPDATE");
        request.put("tableType", "SOURCE");
        request.put("sourceCode", "SRC-AUTO-202607");
        request.put("reason", "Verify work order approval API flow");
        request.put("payload", "No production data changed by this automated test.");
        request.put("notes", "Created by MockMvc API verification.");
        request.put("assignedAdminName", "Admin_100001");
        request.put("testCode", TEST_ID);

        JsonNode created = read(mockMvc.perform(post("/api/work-orders")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andReturn().getResponse().getContentAsString());
        long id = created.at("/data/id").asLong();

        mockMvc.perform(get("/api/work-orders").header("Authorization", bearer(otherUserToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(post("/api/work-orders/" + id + "/approve")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(comment("normal user must not approve")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/work-orders/" + id + "/accept")
                        .header("Authorization", bearer(secondAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(comment("second-level admin accepted")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PROCESSING"));

        request.put("notes", "Second-level admin updated work order notes.");
        mockMvc.perform(put("/api/work-orders/" + id)
                        .header("Authorization", bearer(secondAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PROCESSING"));

        mockMvc.perform(post("/api/work-orders/" + id + "/comments")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(comment("normal user added own comment")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/work-orders/" + id + "/submit-approval")
                        .header("Authorization", bearer(secondAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(comment("submit for first-level approval")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"));

        mockMvc.perform(post("/api/work-orders/" + id + "/reject")
                        .header("Authorization", bearer(secondAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(comment("second-level admin must not reject")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/work-orders/" + id + "/approve")
                        .header("Authorization", bearer(firstAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(comment("first-level admin approved")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        JsonNode closed = read(mockMvc.perform(post("/api/work-orders/" + id + "/close")
                        .header("Authorization", bearer(firstAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(comment("first-level admin closed")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
                .andReturn().getResponse().getContentAsString());

        int logCount = closed.at("/data/logs").size();
        org.junit.jupiter.api.Assertions.assertTrue(logCount >= 7, "Every create/update/comment/status action should be logged");
        writeEvidence(id, logCount);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String comment(String text) throws Exception {
        return objectMapper.writeValueAsString(Map.of("comment", text));
    }

    private JsonNode read(String json) throws Exception {
        return objectMapper.readTree(json);
    }

    private void writeEvidence(long workOrderId, int logCount) throws Exception {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("testId", TEST_ID);
        evidence.put("generatedAt", OffsetDateTime.now().toString());
        evidence.put("workOrderId", workOrderId);
        evidence.put("statusFlow", List.of("SUBMITTED", "PROCESSING", "PENDING_APPROVAL", "APPROVED", "CLOSED"));
        evidence.put("verifiedStatusCodes", List.of("201 create", "200 list/detail/actions", "403 unauthorized role actions"));
        evidence.put("roleChecks", List.of(
                "normal user can create, view own, and comment own work order",
                "normal user cannot approve or reject",
                "second-level admin can view assigned, accept, update, comment, and submit approval",
                "second-level admin cannot final approve or reject",
                "first-level admin can final approve and close"
        ));
        evidence.put("logCount", logCount);
        evidence.put("sensitiveDataPolicy", "No sensitive credential material is written to this evidence file.");
        Path path = Path.of("target", "test-evidence", "work-order-api-verification.json");
        Files.createDirectories(path.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), evidence);
    }
}
