package com.flowguard.infrastructure.security;

import com.flowguard.application.service.TokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Service
public class JwtTokenService implements TokenService {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenService.class);

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenService(@Value("${jwt.secret}") String secret,
                           @Value("${jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    @Override
    public String generateToken(String subject, Map<String, Object> claims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    @Override
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            // A-4: warn per failure type — no token value or stack trace exposed
            logger.warn("JWT token is expired");
        } catch (UnsupportedJwtException e) {
            logger.warn("JWT token algorithm or format is unsupported");
        } catch (MalformedJwtException e) {
            logger.warn("JWT token is malformed");
        } catch (SignatureException e) {
            logger.warn("JWT signature validation failed");
        } catch (IllegalArgumentException e) {
            logger.warn("JWT token is null or empty");
        }
        return false;
    }

    @Override
    public String getSubject(String token) {
        return getClaims(token).getSubject();
    }

    @Override
    public <T> T getClaim(String token, String claimName, Class<T> requiredType) {
        Claims claims = getClaims(token);
        return claims.get(claimName, requiredType);
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
