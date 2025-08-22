package com.realm.security;

import com.realm.service.AuthService;
import com.realm.service.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter for the Realm PKM system.
 * 
 * This filter intercepts incoming requests, extracts JWT tokens from the Authorization header,
 * validates them, and sets up the security context for authenticated requests. It integrates
 * seamlessly with Spring Security's authentication architecture.
 * 
 * Features:
 * - Bearer token extraction from Authorization header
 * - JWT token validation and parsing
 * - Security context setup for valid tokens
 * - Comprehensive error handling and logging
 * - Performance optimization with early exits
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthService authService;
    
    @Autowired
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, AuthService authService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.authService = authService;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // Extract JWT token from request
            String jwt = getJwtFromRequest(request);
            
            // If no token found, continue with the filter chain
            if (!StringUtils.hasText(jwt)) {
                filterChain.doFilter(request, response);
                return;
            }
            
            // Validate token
            if (!jwtTokenProvider.validateToken(jwt)) {
                log.debug("Invalid JWT token for request: {} {}", request.getMethod(), request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }
            
            // Extract username from token
            String username = jwtTokenProvider.getUsernameFromToken(jwt);
            if (!StringUtils.hasText(username)) {
                log.debug("Could not extract username from JWT token");
                filterChain.doFilter(request, response);
                return;
            }
            
            // If we have a valid token and no authentication is set in the context
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                // Load user details
                UserDetails userDetails = authService.loadUserByUsername(username);
                
                // Double-check token validity with user details
                if (jwtTokenProvider.validateToken(jwt)) {
                    // Create authentication token
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            userDetails, 
                            null, 
                            userDetails.getAuthorities()
                        );
                    
                    // Set authentication details
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // Set authentication in security context
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.debug("Successfully authenticated user: {} for request: {} {}", 
                             username, request.getMethod(), request.getRequestURI());
                }
            }
            
        } catch (Exception e) {
            log.error("Cannot set user authentication in security context: {}", e.getMessage(), e);
            // Don't fail the request, just continue without authentication
        }
        
        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }
    
    /**
     * Extract JWT token from the Authorization header
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7); // Remove "Bearer " prefix
            log.debug("Extracted JWT token from Authorization header");
            return token;
        }
        
        // Alternative: check for token in query parameter (less secure, use with caution)
        String tokenParam = request.getParameter("token");
        if (StringUtils.hasText(tokenParam)) {
            log.debug("Extracted JWT token from query parameter");
            return tokenParam;
        }
        
        return null;
    }
    
    /**
     * Determine if this filter should be applied to the request
     * Skip processing for certain endpoints to improve performance
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        
        // Skip JWT processing for public endpoints
        return path.startsWith("/api/auth/") ||
               path.startsWith("/api/public/") ||
               path.startsWith("/health") ||
               path.startsWith("/actuator/health") ||
               path.startsWith("/error") ||
               path.startsWith("/static/") ||
               path.startsWith("/css/") ||
               path.startsWith("/js/") ||
               path.startsWith("/images/") ||
               path.startsWith("/webjars/") ||
               path.startsWith("/v3/api-docs/") ||
               path.startsWith("/swagger-ui/") ||
               path.equals("/favicon.ico");
    }
}