package com.example.jpaspringboot.controller;

import com.example.jpaspringboot.JpAspringbootApplication;
import com.example.jpaspringboot.entity.SystemSecret;
import com.example.jpaspringboot.repository.SystemSecretRepository;
import com.example.jpaspringboot.service.ai.ApiSecretService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = JpAspringbootApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:hnblue_provider_setup_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "security.secret.master-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "api.password="
})
class ProviderSetupControllerTest {
    private static final String TEST_KEY = "unit-test-provider-value-1234567890-ABCD";
    private static final String REPLACEMENT_KEY = "unit-test-provider-value-9876543210-WXYZ";

    @Autowired private MockMvc mockMvc;
    @Autowired private SystemSecretRepository secretRepository;
    @Autowired private ApiSecretService apiSecretService;

    @BeforeEach
    void setUp() {
        secretRepository.deleteAll();
    }

    @Test
    void exposesOnlyMinimalUnlockedStatusWithoutLogin() throws Exception {
        String response = mockMvc.perform(get("/api/internal/provider-setup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locked").value(false))
                .andExpect(jsonPath("$.encryptionReady").value(true))
                .andReturn().getResponse().getContentAsString();

        assertFalse(response.contains("masked"));
        assertFalse(response.contains("updated"));
        assertFalse(response.contains("source"));
        assertFalse(response.toLowerCase().contains("key"));
    }

    @Test
    void storesCiphertextOnceWithoutLoginAndNeverReturnsSecretMetadata() throws Exception {
        String response = mockMvc.perform(post("/api/internal/provider-setup")
                        .header("Origin", "http://127.0.0.1:18080")
                        .with(request -> { request.setRemoteAddr("127.0.0.1"); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apiKey\":\"" + TEST_KEY + "\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:18080"))
                .andExpect(jsonPath("$.locked").value(true))
                .andExpect(jsonPath("$.encryptionReady").value(true))
                .andReturn().getResponse().getContentAsString();

        assertFalse(response.contains(TEST_KEY));
        assertFalse(response.contains("masked"));
        SystemSecret stored = secretRepository.findBySecretName(ApiSecretService.DEEPSEEK_SECRET).orElseThrow();
        assertNotEquals(TEST_KEY, stored.getCipherText());
        assertEquals("hidden", stored.getLastFour());
        assertEquals(TEST_KEY, apiSecretService.getDeepSeekApiKey().orElseThrow());
    }

    @Test
    void permanentlyRejectsReplacementAndKeepsOriginalSecret() throws Exception {
        apiSecretService.saveDeepSeekApiKeyOnce(TEST_KEY);

        mockMvc.perform(post("/api/internal/provider-setup")
                        .with(request -> { request.setRemoteAddr("127.0.0.1"); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apiKey\":\"" + REPLACEMENT_KEY + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.locked").value(true))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(TEST_KEY))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(REPLACEMENT_KEY))));

        assertEquals(TEST_KEY, apiSecretService.getDeepSeekApiKey().orElseThrow());
        assertEquals(1, secretRepository.count());
    }

    @Test
    void offersNoDeleteOrLegacyMutationRoute() throws Exception {
        mockMvc.perform(delete("/api/internal/provider-setup"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(put("/admin/integrations/deepseek")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apiKey\":\"" + TEST_KEY + "\"}"))
                .andExpect(status().isNotFound());
        assertTrue(secretRepository.findBySecretName(ApiSecretService.DEEPSEEK_SECRET).isEmpty());
    }

    @Test
    void rejectsSubmissionOverPublicPlainHttp() throws Exception {
        mockMvc.perform(post("/api/internal/provider-setup")
                        .header("Origin", "http://127.0.0.1:18080")
                        .header("X-Forwarded-Proto", "http")
                        .header("X-Real-IP", "203.0.113.25")
                        .with(request -> { request.setRemoteAddr("127.0.0.1"); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apiKey\":\"" + TEST_KEY + "\"}"))
                .andExpect(status().isUpgradeRequired())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:18080"))
                .andExpect(jsonPath("$.message").value("API Key 只能通过 HTTPS、本机回环地址或 SSH 加密隧道设置"));
        assertTrue(secretRepository.findBySecretName(ApiSecretService.DEEPSEEK_SECRET).isEmpty());
    }
}
