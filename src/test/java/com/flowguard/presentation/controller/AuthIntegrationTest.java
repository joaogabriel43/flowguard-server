package com.flowguard.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowguard.BaseIntegrationTest;
import com.flowguard.application.dto.LoginCommand;
import com.flowguard.application.dto.RegisterCommand;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRegisterAndThenLoginSuccessfully() throws Exception {
        String email = "admin-" + System.currentTimeMillis() + "@flowguard.com";
        String tenantName = "Tenant-" + System.currentTimeMillis();

        RegisterCommand registerCommand = new RegisterCommand(tenantName, email, "password123");

        // 1. Register
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerCommand)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", notNullValue()));

        // 2. Login
        LoginCommand loginCommand = new LoginCommand(email, "password123");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginCommand)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()));
    }

    @Test
    void shouldFailLoginWithInvalidCredentials() throws Exception {
        LoginCommand loginCommand = new LoginCommand("nonexistent@flowguard.com", "wrongpassword");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginCommand)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Invalid credentials")));
    }

    @Test
    void shouldFailRegisterWithDuplicateEmail() throws Exception {
        String email = "dup-" + System.currentTimeMillis() + "@flowguard.com";
        
        RegisterCommand register1 = new RegisterCommand("Tenant 1", email, "password123");
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register1)))
                .andExpect(status().isCreated());

        RegisterCommand register2 = new RegisterCommand("Tenant 2", email, "password123");
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }
}
