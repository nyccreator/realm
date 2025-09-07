package com.realm.config;

import com.realm.model.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Custom filter to restore Spring Security authentication context from Redis session
 * 
 * This filter checks for valid session attributes and populates the SecurityContext
 * to enable @AuthenticationPrincipal and other Spring Security features.
 */
@Component
@Slf4j
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        // Skip if security context already has authentication
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }
        
        HttpSession session = request.getSession(false);
        if (session != null) {
            // Check if session has our authentication markers
            Boolean authenticated = (Boolean) session.getAttribute("authenticated");
            User user = (User) session.getAttribute("user");
            
            if (authenticated != null && authenticated && user != null) {
                log.debug("Restoring authentication context for user: {}", user.getEmail());
                
                // Create authentication token with user as principal
                UsernamePasswordAuthenticationToken authToken = 
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // Set in security context
                SecurityContextHolder.getContext().setAuthentication(authToken);
                
                log.debug("Authentication context restored successfully for user: {}", user.getEmail());
            }
        }
        
        filterChain.doFilter(request, response);
    }
}