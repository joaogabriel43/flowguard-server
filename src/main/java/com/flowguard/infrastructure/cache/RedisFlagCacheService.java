package com.flowguard.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowguard.application.dto.FeatureFlagDto;
import com.flowguard.application.service.FlagCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class RedisFlagCacheService implements FlagCacheService {

    private static final Logger logger = LoggerFactory.getLogger(RedisFlagCacheService.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisFlagCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    private String buildKey(UUID tenantId, String key) {
        return "flags:" + tenantId + ":" + key;
    }

    @Override
    public Optional<FeatureFlagDto> getFlag(UUID tenantId, String key) {
        String redisKey = buildKey(tenantId, key);
        try {
            String value = redisTemplate.opsForValue().get(redisKey);
            if (value == null) {
                logger.debug("Cache miss for key: {}", redisKey);
                return Optional.empty();
            }
            logger.debug("Cache hit for key: {}", redisKey);
            FeatureFlagDto flagDto = objectMapper.readValue(value, FeatureFlagDto.class);
            return Optional.of(flagDto);
        } catch (Exception e) {
            logger.error("Failed to read flag from Redis cache for key: " + redisKey, e);
            return Optional.empty();
        }
    }

    @Override
    public void putFlag(UUID tenantId, String key, FeatureFlagDto flag) {
        String redisKey = buildKey(tenantId, key);
        try {
            String json = objectMapper.writeValueAsString(flag);
            redisTemplate.opsForValue().set(redisKey, json);
            logger.debug("Cached flag under key: {}", redisKey);
        } catch (Exception e) {
            logger.error("Failed to write flag to Redis cache for key: " + redisKey, e);
        }
    }

    @Override
    public void evictFlag(UUID tenantId, String key) {
        String redisKey = buildKey(tenantId, key);
        try {
            redisTemplate.delete(redisKey);
            logger.debug("Evicted flag from cache for key: {}", redisKey);
        } catch (Exception e) {
            logger.error("Failed to evict flag from Redis cache for key: " + redisKey, e);
        }
    }
}
