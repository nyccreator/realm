package com.realm.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT Token Provider service for secure authentication in the Realm PKM system.
 * 
 * This service handles JWT token generation, validation, and parsing for the single-user
 * authentication system. It provides secure token operations with proper expiration
 * handling and claim management.
 * 
 * Security Features:
 * - HMAC-SHA256 signature algorithm
 * - Configurable token expiration
 * - Comprehensive token validation
 * - User identity extraction from tokens
 * - Secure key management
 */
@Service
@Slf4j
public class JwtTokenProvider {
    
    private final SecretKey secretKey;
    private final long tokenValidityMilliseconds;
    
    public JwtTokenProvider(
            @Value("${realm.jwt.secret}") String secret,
            @Value("${realm.jwt.expiration}") long tokenValidityMilliseconds) {
        
        // Ensure the secret is long enough for HMAC-SHA256
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            log.warn("JWT secret is shorter than recommended 256 bits. Consider using a longer secret.");
        }
        
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.tokenValidityMilliseconds = tokenValidityMilliseconds;
        
        log.info("JWT Token Provider initialized with expiration: {} ms", tokenValidityMilliseconds);
    }
    
    /**
     * Generate JWT token from Authentication object
     */
    public String generateToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        return generateTokenFromUsername(userPrincipal.getUsername());
    }
    
    /**
     * Generate JWT token from username
     */
    public String generateTokenFromUsername(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + tokenValidityMilliseconds);
        
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setIssuer("realm-pkm")
                .setAudience("realm-user")
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }
    
    /**
     * Generate refresh token with longer expiration
     */
    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + (tokenValidityMilliseconds * 7)); // 7 times longer
        
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setIssuer("realm-pkm")
                .setAudience("realm-refresh")
                .claim("type", "refresh")
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }
    
    /**
     * Extract username from JWT token
     */
    public String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Failed to extract username from token: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Validate JWT token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token);
            
            return true;
        } catch (SignatureException e) {
            log.debug("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.debug("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.debug("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.debug("JWT claims string is empty: {}", e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            return claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true; // Consider invalid tokens as expired
        }
    }
    
    /**
     * Get token expiration date
     */
    public Date getExpirationDateFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            return claims.getExpiration();
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Failed to extract expiration date from token: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Get token issued date
     */
    public Date getIssuedDateFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            return claims.getIssuedAt();
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Failed to extract issued date from token: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if this is a refresh token
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            return "refresh".equals(claims.get("type", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Get remaining time until token expires (in milliseconds)
     */
    public long getRemainingTimeToExpire(String token) {
        Date expirationDate = getExpirationDateFromToken(token);
        if (expirationDate == null) {
            return 0;
        }
        
        long remaining = expirationDate.getTime() - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
    
    /**
     * Extract all claims from token
     */
    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Failed to extract claims from token: {}", e.getMessage());
            return null;
        }
    }
}