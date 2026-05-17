package com.flowguard.application.service;

import java.util.Map;

public interface TokenService {
    String generateToken(String subject, Map<String, Object> claims);
    boolean validateToken(String token);
    String getSubject(String token);
    <T> T getClaim(String token, String claimName, Class<T> requiredType);
}
