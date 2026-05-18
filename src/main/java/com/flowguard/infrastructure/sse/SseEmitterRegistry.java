package com.flowguard.infrastructure.sse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SseEmitterRegistry {

    private static final Logger logger = LoggerFactory.getLogger(SseEmitterRegistry.class);

    private final Map<UUID, List<SseEmitter>> registry = new ConcurrentHashMap<>();

    public SseEmitter register(UUID tenantId, SseEmitter emitter) {
        registry.computeIfAbsent(tenantId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        
        emitter.onCompletion(() -> remove(tenantId, emitter));
        emitter.onTimeout(() -> remove(tenantId, emitter));
        emitter.onError((ex) -> remove(tenantId, emitter));
        
        logger.debug("Registered new SSE emitter for tenant {}. Total active emitters: {}", tenantId, getCount(tenantId));
        return emitter;
    }

    public void remove(UUID tenantId, SseEmitter emitter) {
        List<SseEmitter> emitters = registry.get(tenantId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                registry.remove(tenantId);
            }
            logger.debug("Removed SSE emitter for tenant {}. Total remaining: {}", tenantId, getCount(tenantId));
        }
    }

    public void broadcast(UUID tenantId, String eventName, Object data) {
        List<SseEmitter> emitters = registry.get(tenantId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        logger.debug("Broadcasting event '{}' to {} emitters for tenant {}", eventName, emitters.size(), tenantId);
        
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (IOException | IllegalStateException e) {
                logger.debug("Failed to send SSE event to emitter. Marking as dead.", e);
                deadEmitters.add(emitter);
            }
        }

        for (SseEmitter dead : deadEmitters) {
            remove(tenantId, dead);
        }
    }

    public int getCount(UUID tenantId) {
        List<SseEmitter> list = registry.get(tenantId);
        return list != null ? list.size() : 0;
    }

    public int getTotalCount() {
        return registry.values().stream().mapToInt(List::size).sum();
    }

    // B-1: send a heartbeat event to every connected emitter across all tenants
    public void broadcastHeartbeat() {
        registry.forEach((tenantId, emitters) -> {
            if (emitters == null || emitters.isEmpty()) {
                return;
            }
            List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().name("heartbeat").data(""));
                } catch (IOException | IllegalStateException e) {
                    deadEmitters.add(emitter);
                }
            }
            for (SseEmitter dead : deadEmitters) {
                remove(tenantId, dead);
            }
        });
    }
}
