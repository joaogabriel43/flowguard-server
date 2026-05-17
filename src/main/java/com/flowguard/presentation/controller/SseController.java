package com.flowguard.presentation.controller;

import com.flowguard.application.dto.FeatureFlagDto;
import com.flowguard.application.service.FeatureFlagService;
import com.flowguard.domain.model.TenantContext;
import com.flowguard.infrastructure.sse.SseEmitterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
public class SseController {

    private static final Logger logger = LoggerFactory.getLogger(SseController.class);

    private final SseEmitterRegistry sseEmitterRegistry;
    private final FeatureFlagService featureFlagService;

    public SseController(SseEmitterRegistry sseEmitterRegistry, FeatureFlagService featureFlagService) {
        this.sseEmitterRegistry = sseEmitterRegistry;
        this.featureFlagService = featureFlagService;
    }

    @GetMapping(value = "/api/sse/flags", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamFlags() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context not initialized");
        }

        logger.info("New SSE client connecting for tenant: {}", tenantId);

        // 30 minutes timeout: 30 * 60 * 1000 = 1,800,000 ms
        SseEmitter emitter = new SseEmitter(1800000L);
        
        // Register the emitter in the registry
        sseEmitterRegistry.register(tenantId, emitter);

        // Dispatch initial snapshot immediately
        try {
            List<FeatureFlagDto> flags = featureFlagService.listFeatureFlags();
            emitter.send(SseEmitter.event()
                    .name("flag-snapshot")
                    .data(flags));
            logger.debug("Initial feature flag snapshot dispatched to tenant {}", tenantId);
        } catch (IOException e) {
            logger.error("Failed to send initial snapshot to SSE emitter for tenant " + tenantId, e);
            emitter.completeWithError(e);
        }

        return emitter;
    }
}
