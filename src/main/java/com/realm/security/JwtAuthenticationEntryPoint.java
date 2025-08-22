package com.realm.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Authentication Entry Point for the Realm PKM system.
 * 
 * This class handles authentication failures and unauthorized access attempts.
 * When a user tries to access a protected resource without proper authentication
 * or with an invalid JWT token, this entry point returns a structured JSON
 * error response instead of redirecting to a login page.
 * 
 * Features:
 * - Structured JSON error responses
 * - Comprehensive error logging
 * - Client-friendly error messages
 * - Proper HTTP status codes
 * - Request context information
 */
@Component
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public void commence(HttpServletRequest request, 
                        HttpServletResponse response,
                        AuthenticationException authException) throws IOException, ServletException {
        
        // Log the authentication failure
        log.warn("Unauthorized access attempt - IP: {}, URI: {} {}, User-Agent: {}, Error: {}", 
                getClientIpAddress(request),
                request.getMethod(),
                request.getRequestURI(), 
                request.getHeader("User-Agent"),
                authException.getMessage());
        
        // Set response properties
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        // Create error response body
        Map<String, Object> errorResponse = createErrorResponse(request, authException);
        
        // Write JSON response
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
    
    /**
     * Create a structured error response
     */
    private Map<String, Object> createErrorResponse(HttpServletRequest request, AuthenticationException authException) {
        Map<String, Object> errorResponse = new HashMap<>();
        
        // Basic error information
        errorResponse.put("error", "Unauthorized");
        errorResponse.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        errorResponse.put("message", determineErrorMessage(request, authException));
        errorResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        // Request context
        errorResponse.put("path", request.getRequestURI());
        errorResponse.put("method", request.getMethod());
        
        // Additional context for debugging (exclude in production if needed)
        if (log.isDebugEnabled()) {
            errorResponse.put("exception", authException.getClass().getSimpleName());
            errorResponse.put("details", authException.getMessage());
        }
        
        return errorResponse;
    }
    
    /**
     * Determine appropriate error message based on the request and exception
     */
    private String determineErrorMessage(HttpServletRequest request, AuthenticationException authException) {
        String authHeader = request.getHeader("Authorization");
        
        // No authorization header
        if (authHeader == null || authHeader.trim().isEmpty()) {
            return "Authentication token is required to access this resource";
        }
        
        // Invalid authorization header format
        if (!authHeader.startsWith("Bearer ")) {
            return "Invalid authorization header format. Use 'Bearer <token>'";
        }
        
        // Token present but invalid
        if (authHeader.length() <= 7) { // "Bearer " is 7 characters
            return "Authentication token is missing or empty";
        }
        
        // Check if it looks like a JWT token (has dots)
        String token = authHeader.substring(7);
        if (!token.contains(".")) {
            return "Invalid authentication token format";
        }
        
        // Generic message for other cases (expired, malformed, etc.)
        return "Authentication token is invalid or expired. Please log in again";
    }
    
    /**
     * Extract client IP address from request, considering proxy headers
     */
    private String getClientIpAddress(HttpServletRequest request) {
        // Check for IP from proxy headers first
        String ipAddress = request.getHeader("X-Forwarded-For");
        
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_CLIENT_IP");
        }
        
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        
        // If there are multiple IPs (from proxy chain), take the first one
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        
        return ipAddress != null ? ipAddress : "unknown";
    }
}