package com.flowguard.presentation.controller;

import com.flowguard.BaseIntegrationTest;
import com.flowguard.application.dto.CreateFeatureFlagCommand;
import com.flowguard.application.service.FeatureFlagService;
import com.flowguard.domain.model.Tenant;
import com.flowguard.domain.model.TenantContext;
import com.flowguard.domain.repository.FeatureFlagRepository;
import com.flowguard.domain.repository.TenantRepository;
import com.flowguard.infrastructure.sse.SseEmitterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

class SseIsolationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private FeatureFlagService featureFlagService;

    @Autowired
    private FeatureFlagRepository featureFlagRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private SseEmitterRegistry sseEmitterRegistry;

    private UUID tenantA;
    private UUID tenantB;

    private SseEmitter emitterA;
    private SseEmitter emitterB;

    private List<SseEmitter.SseEventBuilder> eventsReceivedA;
    private List<SseEmitter.SseEventBuilder> eventsReceivedB;

    @BeforeEach
    void setUp() throws IOException {
        // Clear existing database
        featureFlagRepository.deleteAll();
        tenantRepository.deleteAll();

        // Save both tenants in database and retrieve generated IDs
        Tenant tenantObjA = Tenant.builder()
                .name("Tenant A")
                .apiKey("key-a")
                .build();
        Tenant tenantObjB = Tenant.builder()
                .name("Tenant B")
                .apiKey("key-b")
                .build();
        
        tenantObjA = tenantRepository.saveAndFlush(tenantObjA);
        tenantObjB = tenantRepository.saveAndFlush(tenantObjB);

        tenantA = tenantObjA.getId();
        tenantB = tenantObjB.getId();

        emitterA = Mockito.mock(SseEmitter.class);
        emitterB = Mockito.mock(SseEmitter.class);

        eventsReceivedA = new CopyOnWriteArrayList<>();
        eventsReceivedB = new CopyOnWriteArrayList<>();

        // Capture events sent to Emitter A
        doAnswer(invocation -> {
            eventsReceivedA.add(invocation.getArgument(0));
            return null;
        }).when(emitterA).send(any(SseEmitter.SseEventBuilder.class));

        // Capture events sent to Emitter B
        doAnswer(invocation -> {
            eventsReceivedB.add(invocation.getArgument(0));
            return null;
        }).when(emitterB).send(any(SseEmitter.SseEventBuilder.class));

        // Register both emitters
        sseEmitterRegistry.register(tenantA, emitterA);
        sseEmitterRegistry.register(tenantB, emitterB);
    }

    @AfterEach
    void tearDown() {
        sseEmitterRegistry.remove(tenantA, emitterA);
        sseEmitterRegistry.remove(tenantB, emitterB);
        TenantContext.clear();
    }

    @Test
    void shouldIsolateEventsBetweenTenantsUsingRedisPubSubAndSse() {
        // 1. Set Tenant context to Tenant A and create a flag
        TenantContext.setTenantId(tenantA);
        featureFlagService.createFeatureFlag(new CreateFeatureFlagCommand("my-flag", "My Flag", "Desc", true, 50));

        // 2. Perform Toggle on Tenant A flag. This publishes to Redis -> subscriber broadcasts to Tenant A SSE.
        featureFlagService.toggleFeatureFlag("my-flag", "tester");

        // 3. Use Awaitility to await asynchronous processing through Redis Pub/Sub into the SSE broadcaster
        // Since CREATED and TOGGLED will be published, Tenant A should receive events.
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertTrue(eventsReceivedA.size() >= 2, "Tenant A should have received at least 2 events (created and toggled)");
        });

        // 4. Assert isolation: Tenant B must NOT have received any events
        assertTrue(eventsReceivedB.isEmpty(), "Tenant B should not receive any events intended for Tenant A");
    }
}
