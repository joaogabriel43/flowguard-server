package com.flowguard.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowguard.BaseIntegrationTest;
import com.flowguard.application.dto.*;
import com.flowguard.domain.model.RuleOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EvaluateFlagIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        String email = "admin-eval-" + UUID.randomUUID() + "@flowguard.com";
        RegisterCommand reg = new RegisterCommand("Tenant Eval", email, "password");
        MvcResult res = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn();
        TokenResponse tokenResponse = objectMapper.readValue(res.getResponse().getContentAsString(), TokenResponse.class);
        token = tokenResponse.token();
    }

    @Test
    void shouldEvaluateFlagInStrictOrder() throws Exception {
        String flagKey = "eval-flag-" + UUID.randomUUID();

        // 1. Create a Feature Flag (Disabled, Rollout 50%)
        CreateFeatureFlagCommand createCommand = new CreateFeatureFlagCommand(
                flagKey, "Eval Flag", "For testing evaluation pipeline", false, 50
        );

        mockMvc.perform(post("/api/flags")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCommand)))
                .andExpect(status().isCreated());

        // 2. Add rule (plan equals premium)
        CreateFlagRuleCommand createRuleCommand = new CreateFlagRuleCommand(
                "plan", RuleOperator.EQUALS, "premium"
        );

        mockMvc.perform(post("/api/flags/" + flagKey + "/rules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRuleCommand)))
                .andExpect(status().isCreated());

        // 3. Evaluate when flag is DISABLED (Should fail with DISABLED reason immediately)
        EvaluateRequest requestPremium = new EvaluateRequest("user-1", Map.of("plan", "premium"));
        mockMvc.perform(post("/api/flags/" + flagKey + "/evaluate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestPremium)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.reason").value("DISABLED"));

        // 4. Enable the flag via toggle endpoint (PATCH /api/flags/{key}/toggle)
        mockMvc.perform(patch("/api/flags/" + flagKey + "/toggle")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        // 5. Evaluate with attributes that don't match the rule (plan equals free -> RULE_MISMATCH)
        EvaluateRequest requestFree = new EvaluateRequest("user-1", Map.of("plan", "free"));
        mockMvc.perform(post("/api/flags/" + flagKey + "/evaluate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestFree)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.reason").value("RULE_MISMATCH"));

        // 6. Evaluate with correct attributes (plan equals premium -> passes rules check, triggers ROLLOUT check)
        mockMvc.perform(post("/api/flags/" + flagKey + "/evaluate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestPremium)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason").value("ROLLOUT"));
    }
}
