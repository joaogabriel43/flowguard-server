package com.flowguard.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowguard.application.dto.FlagChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class FlagChangePublisher {

    private static final Logger logger = LoggerFactory.getLogger(FlagChangePublisher.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public FlagChangePublisher(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(FlagChangeEvent event) {
        String channel = "flag-changes:" + event.tenantId();
        try {
            String message = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(channel, message);
            logger.debug("Published flag change event to channel {}: {}", channel, message);
        } catch (Exception e) {
            logger.error("Failed to publish flag change event to Redis channel " + channel, e);
        }
    }
}
