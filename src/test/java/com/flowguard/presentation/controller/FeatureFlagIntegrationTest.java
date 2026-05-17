package com.flowguard.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowguard.BaseIntegrationTest;
import com.flowguard.application.dto.*;
import com.flowguard.domain.model.AuditLog;
import com.flowguard.domain.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FeatureFlagIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() throws Exception {
        // Register Tenant A
        String emailA = "admin-a-" + UUID.randomUUID() + "@flowguard.com";
        RegisterCommand regA = new RegisterCommand("Tenant A", emailA, "password");
        MvcResult resA = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regA)))
                .andExpect(status().isCreated())
                .andReturn();
        TokenResponse tokenResponseA = objectMapper.readValue(resA.getResponse().getContentAsString(), TokenResponse.class);
        tokenA = tokenResponseA.token();

        // Register Tenant B
        String emailB = "admin-b-" + UUID.randomUUID() + "@flowguard.com";
        RegisterCommand regB = new RegisterCommand("Tenant B", emailB, "password");
        MvcResult resB = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regB)))
                .andExpect(status().isCreated())
                .andReturn();
        TokenResponse tokenResponseB = objectMapper.readValue(resB.getResponse().getContentAsString(), TokenResponse.class);
        tokenB = tokenResponseB.token();
    }

    @Test
    void shouldPerformCompleteCrudOnFeatureFlagsAndIsolateTenants() throws Exception {
        // 1. Create a flag for Tenant A
        CreateFeatureFlagCommand createCommand = new CreateFeatureFlagCommand(
                "my-flag", "My Flag", "Description", false, 25
        );

        MvcResult createRes = mockMvc.perform(post("/api/flags")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCommand)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value("my-flag"))
                .andExpect(jsonPath("$.enabled").value(false))
                .andReturn();

        FeatureFlagDto createdFlag = objectMapper.readValue(createRes.getResponse().getContentAsString(), FeatureFlagDto.class);
        assertNotNull(createdFlag);

        // 2. Try to access Tenant A's flag using Tenant B's token (Should return 404 Not Found)
        mockMvc.perform(get("/api/flags/my-flag")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // 3. Retrieve Tenant A's flag using Tenant A's token (Should work)
        mockMvc.perform(get("/api/flags/my-flag")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("my-flag"));

        // 4. Update the flag for Tenant A
        UpdateFeatureFlagCommand updateCommand = new UpdateFeatureFlagCommand(
                "My Flag Updated", "New Desc", false, 50
        );

        mockMvc.perform(put("/api/flags/my-flag")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCommand)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("My Flag Updated"))
                .andExpect(jsonPath("$.description").value("New Desc"))
                .andExpect(jsonPath("$.rolloutPercentage").value(50));

        // 5. Toggle the flag for Tenant A (PATCH)
        mockMvc.perform(patch("/api/flags/my-flag/toggle")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        // 6. Verify that an Audit Log was created in the database for Tenant A
        List<AuditLog> auditLogs = auditLogRepository.findAllByTenantId(createdFlag.tenantId());
        assertFalse(auditLogs.isEmpty());
        AuditLog toggleAudit = auditLogs.stream()
                .filter(log -> log.getFlagId().equals(createdFlag.id()))
                .findFirst()
                .orElse(null);

        assertNotNull(toggleAudit);
        assertEquals("ACTIVATED", toggleAudit.getAction());

        // 7. Delete the flag for Tenant A
        mockMvc.perform(delete("/api/flags/my-flag")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());

        // 8. Try to retrieve deleted flag (Should return 404 Not Found)
        mockMvc.perform(get("/api/flags/my-flag")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }
}
