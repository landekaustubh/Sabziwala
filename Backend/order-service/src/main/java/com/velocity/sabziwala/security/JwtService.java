package com.velocity.sabziwala.security;

import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class JwtService {
	private final SecretKey signingKey;

    public JwtService(@Value("${jwt.secret}") String secret) {
        // Same key that IAM Service uses to SIGN tokens
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
    
    /** Extract email (stored in JWT "sub" claim) */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /** Extract userId from custom claim */
    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", String.class));
    }
    
    /**
     * Extract role from custom claim.
     * IAM sets this as: "CUSTOMER", "ADMIN", or "DELIVERY"
     * Our filter prefixes it with "ROLE_" for Spring Security.
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /** Extract username from custom claim */
    public String extractUsername(String token) {
        return extractClaim(token, claims -> claims.get("username", String.class));
    }

    /** Generic claim extractor */
    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }
    
    /**
     * Validate the token: check signature + expiry.
     *
     * Note: We do NOT check Redis blacklist here because Order Service
     * doesn't have Redis. If the token was blacklisted (user logged out),
     * the API Gateway should reject it before it reaches us.
     *
     * In a production system:
     * (a) API Gateway validates against Redis (recommended)
     */
    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);  // Throws if invalid signature or expired
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired: {}", e.getMessage());
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT invalid: {}", e.getMessage());
            return false;
        }
    }
    
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
