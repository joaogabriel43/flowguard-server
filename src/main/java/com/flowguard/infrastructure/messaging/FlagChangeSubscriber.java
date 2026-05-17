package com.flowguard.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowguard.application.dto.FlagChangeEvent;
import com.flowguard.infrastructure.sse.SseEmitterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
public class FlagChangeSubscriber implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(FlagChangeSubscriber.class);

    private final SseEmitterRegistry sseEmitterRegistry;
    private final ObjectMapper objectMapper;

    public FlagChangeSubscriber(SseEmitterRegistry sseEmitterRegistry, ObjectMapper objectMapper) {
        this.sseEmitterRegistry = sseEmitterRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody());
            logger.debug("Received Redis Pub/Sub message: {}", body);

            FlagChangeEvent event = objectMapper.readValue(body, FlagChangeEvent.class);
            
            // Map action to standard SSE event name:
            // - flag-updated: CREATED, UPDATED
            // - flag-deleted: DELETED
            // - flag-toggled: TOGGLED
            String sseEventName;
            switch (event.action()) {
                case "CREATED":
                case "UPDATED":
                    sseEventName = "flag-updated";
                    break;
                case "DELETED":
                    sseEventName = "flag-deleted";
                    break;
                case "TOGGLED":
                    sseEventName = "flag-toggled";
                    break;
                default:
                    sseEventName = "flag-updated";
                    break;
            }

            sseEmitterRegistry.broadcast(event.tenantId(), sseEventName, event);
            
        } catch (Exception e) {
            logger.error("Failed to parse and broadcast feature flag change event", e);
        }
    }
}
