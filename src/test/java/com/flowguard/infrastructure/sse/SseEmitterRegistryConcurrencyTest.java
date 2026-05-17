package com.flowguard.infrastructure.sse;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SseEmitterRegistryConcurrencyTest {

    @RepeatedTest(5)
    void shouldHandleHighConcurrencyWithoutLeaksOrErrors() throws InterruptedException {
        SseEmitterRegistry registry = new SseEmitterRegistry();
        UUID tenantId = UUID.randomUUID();
        int threadCount = 50;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<SseEmitter> emitters = new ArrayList<>();

        // Register emitters concurrently
        for (int i = 0; i < threadCount; i++) {
            SseEmitter emitter = new SseEmitter(5000L);
            emitters.add(emitter);
            executor.submit(() -> {
                registry.register(tenantId, emitter);
            });
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        // Registry should have registered all
        assertEquals(threadCount, registry.getCount(tenantId));
        assertEquals(threadCount, registry.getTotalCount());

        // Concurrently trigger timeout callbacks and broadcast
        ExecutorService executor2 = Executors.newFixedThreadPool(threadCount);
        for (SseEmitter emitter : emitters) {
            executor2.submit(() -> {
                registry.broadcast(tenantId, "test-event", "data");
                registry.remove(tenantId, emitter);
            });
        }

        executor2.shutdown();
        assertTrue(executor2.awaitTermination(5, TimeUnit.SECONDS));

        // Registry should be completely clean
        assertEquals(0, registry.getCount(tenantId));
        assertEquals(0, registry.getTotalCount());
    }
}
