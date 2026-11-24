package com.example.jpaspringboot.controller;

import com.example.jpaspringboot.JpAspringbootApplication;
import com.example.jpaspringboot.entity.ProfessionalAccount;
import com.example.jpaspringboot.repository.ProfessionalAccountRepository;
import com.example.jpaspringboot.security.AccountRole;
import com.example.jpaspringboot.service.impl.AdminServiceImpl;
import com.example.jpaspringboot.util.JwtUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = JpAspringbootApplication.class)
@AutoConfigureMockMvc
class ProfessionalAccountSecurityTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private AdminServiceImpl adminService;
    @Autowired private ProfessionalAccountRepository professionals;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void professionalLifecycleEnforcesServerSideRoleStatusAndTokenRevocation() throws Exception {
        String suffix = Long.toString(System.nanoTime(), 36);
        String adminName = "stage1_admin_" + suffix;
        int adminId = adminService.addAdmin(adminName, "AdminPass42");
        String adminToken = JwtUtils.generateAccessToken(adminName, AccountRole.ADMIN, (long) adminId, 0);

        mockMvc.perform(post("/admin/professionals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"unauthorized_professional\"}"))
                .andExpect(status().isUnauthorized());

        MvcResult created = mockMvc.perform(post("/admin/professionals/batch")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("prefix", "pro" + suffix, "count", 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created.length()").value(2))
                .andExpect(jsonPath("$.skipped.length()").value(0))
                .andReturn();

        JsonNode credential = mapper.readTree(created.getResponse().getContentAsString()).path("created").get(0);
        long professionalId = credential.path("accountId").asLong();
        String username = credential.path("username").asText();
        String temporaryPassword = credential.path("temporaryPassword").asText();
        ProfessionalAccount stored = professionals.findByUsername(username).orElseThrow();
        assertFalse(stored.isEnabled());
        assertTrue(stored.isPasswordChangeRequired());
        assertNotEquals(temporaryPassword, stored.getPasswordHash());
        assertTrue(passwordEncoder.matches(temporaryPassword, stored.getPasswordHash()));

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", username, "password", temporaryPassword))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/admin/professionals/{id}/status", professionalId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        String temporaryToken = login(username, temporaryPassword, true);
        mockMvc.perform(post("/api/agent/tasks")
                        .header("Authorization", "Bearer " + temporaryToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(agentRequest("BASIC_QA")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("password_change_required"));

        String newPassword = "NewSecure42";
        mockMvc.perform(post("/api/professional/change-password")
                        .header("Authorization", "Bearer " + temporaryToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "currentPassword", temporaryPassword,
                                "newPassword", newPassword))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenRevoked").value(true));

        mockMvc.perform(get("/api/agent/tasks/not-a-real-task")
                        .header("Authorization", "Bearer " + temporaryToken))
                .andExpect(status().isUnauthorized());

        String activeToken = login(username, newPassword, false);
        mockMvc.perform(post("/api/agent/tasks")
                        .header("Authorization", "Bearer " + activeToken)
                        .header("Idempotency-Key", "stage1-" + suffix)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(agentRequest("PROFESSIONAL_ANALYSIS")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.taskId").value("stage1-" + suffix))
                .andExpect(jsonPath("$.capability").value("PROFESSIONAL_ANALYSIS"));

        mockMvc.perform(put("/api/indicators/update")
                        .header("Authorization", "Bearer " + activeToken)
                        .param("region", "琼海")
                        .param("indicator", "mangrove_area")
                        .param("value", "121.5"))
                .andExpect(status().isForbidden());
    }

    @Test
    void publicRegistrationAndLoginRemainAvailableButCannotAdministerProfessionals() throws Exception {
        String suffix = Long.toString(System.nanoTime(), 36);
        String username = "pub_" + suffix;
        String password = "PublicPass42";
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", password,
                                "email", username + "@example.com",
                                "birthdate", "2000-01-01"))))
                .andExpect(status().isOk());

        String publicToken = loginPublic(username, password);
        mockMvc.perform(post("/admin/professionals")
                        .header("Authorization", "Bearer " + publicToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"forbidden_professional\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/indicators/update")
                        .header("Authorization", "Bearer " + publicToken)
                        .param("region", "琼海")
                        .param("indicator", "mangrove_area")
                        .param("value", "999"))
                .andExpect(status().isForbidden());
    }

    private String login(String username, String password, boolean passwordChangeRequired) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userType").value("Professional"))
                .andExpect(jsonPath("$.passwordChangeRequired").value(passwordChangeRequired))
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).path("token").asText();
    }

    private String loginPublic(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userType").value("User"))
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).path("token").asText();
    }

    private String agentRequest(String capability) throws Exception {
        return mapper.writeValueAsString(Map.of(
                "capability", capability,
                "tokenBudget", 1000,
                "timeoutSeconds", 30,
                "message", "Summarize a local blue-carbon knowledge entry."));
    }
}
