package com.flowguard.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowguard.application.dto.FlagChangeEvent;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonSerializationPropertyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Property
    void shouldSerializeAndDeserializeConsistently(
            @ForAll String flagKey,
            @ForAll String action,
            @ForAll String timestamp) throws Exception {
        
        // Generate a random UUID for tenantId
        UUID tenantId = UUID.randomUUID();
        
        // Create change event DTO
        FlagChangeEvent original = new FlagChangeEvent(flagKey, tenantId, action, timestamp);

        // Serialize to JSON String
        String json = objectMapper.writeValueAsString(original);

        // Deserialize back to DTO
        FlagChangeEvent deserialized = objectMapper.readValue(json, FlagChangeEvent.class);

        // Assert perfect equality
        assertEquals(original.flagKey(), deserialized.flagKey());
        assertEquals(original.tenantId(), deserialized.tenantId());
        assertEquals(original.action(), deserialized.action());
        assertEquals(original.timestamp(), deserialized.timestamp());
    }
}
