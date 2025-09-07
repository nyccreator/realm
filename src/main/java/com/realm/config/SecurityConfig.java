package com.realm.config;

import com.realm.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security Configuration for the Realm PKM system.
 * 
 * This configuration sets up Redis session-based authentication for the single-user PKM system,
 * with proper CORS handling, CSRF protection, security headers, and endpoint protection. 
 * The configuration is optimized for a React frontend with a REST API backend architecture.
 * 
 * Security Features:
 * - Redis session-based stateful authentication
 * - BCrypt password encoding
 * - CSRF protection with double-submit cookies
 * - Session fixation protection
 * - CORS configuration for frontend integration with credentials support
 * - Comprehensive security headers
 * - Session event monitoring
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Slf4j
public class SecurityConfig {
    
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final SessionAuthenticationFilter sessionAuthenticationFilter;
    
    @Autowired
    public SecurityConfig(AuthService authService, PasswordEncoder passwordEncoder, SessionAuthenticationFilter sessionAuthenticationFilter) {
        this.authService = authService;
        this.passwordEncoder = passwordEncoder;
        this.sessionAuthenticationFilter = sessionAuthenticationFilter;
    }
    
    
    /**
     * Authentication provider configuration
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(authService);
        authProvider.setPasswordEncoder(passwordEncoder);
        authProvider.setHideUserNotFoundExceptions(false); // For better error messages in single-user system
        return authProvider;
    }
    
    /**
     * Authentication manager bean
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    /**
     * CORS configuration for frontend integration with session support
     * Critical: Must allow credentials for session cookies to work
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow specific origins (add your frontend URLs)
        configuration.setAllowedOriginPatterns(List.of(
            "http://localhost:3000",  // React development server
            "http://localhost:3001",  // Alternative React port
            "http://127.0.0.1:3000",
            "http://127.0.0.1:3001"
        ));
        
        // Allow specific HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        
        // Allow specific headers - including CSRF token header
        configuration.setAllowedHeaders(Arrays.asList(
            "Content-Type", 
            "X-Requested-With",
            "accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "X-CSRF-TOKEN",           // For CSRF protection
            "X-XSRF-TOKEN",           // Alternative CSRF header name used by Spring
            "Cookie"                  // For session cookies
        ));
        
        // Expose headers that frontend needs - including CSRF token
        configuration.setExposedHeaders(Arrays.asList(
            "Set-Cookie",
            "X-CSRF-TOKEN",
            "X-Total-Count"
        ));
        
        // CRITICAL: Allow credentials for session-based authentication
        configuration.setAllowCredentials(true);
        
        // Cache preflight requests for 1 hour
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
    
    /**
     * Session event publisher for monitoring session lifecycle
     * Enables session creation/destruction event handling
     */
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
    
    /**
     * Main security filter chain configuration for session-based authentication
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("Configuring Redis session-based security filter chain for Realm PKM system");
        
        http
            // Enable CSRF protection with cookie repository for session-based auth
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/api/auth/test", "/api/auth/register", "/api/auth/login", "/api/notes/**") // Allow auth and notes endpoints without CSRF for debugging
            )
            
            // Enable CORS with credentials support
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Configure session management - STATEFUL for Redis sessions
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation(sessionFixation -> sessionFixation.migrateSession())  // Prevent session fixation attacks
                .invalidSessionUrl("/api/auth/session-expired")
                .maximumSessions(5)  // Allow up to 5 concurrent sessions per user
                .maxSessionsPreventsLogin(false)  // Allow new login, expire oldest session
            )
            
            // Configure authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public endpoints - no authentication required
                .requestMatchers(
                    "/api/auth/login",        // Login endpoint
                    "/api/auth/register",     // Registration endpoint
                    "/api/auth/validate",     // Session validation endpoint  
                    "/api/auth/test",         // Test endpoint for debugging
                    "/api/auth/csrf",         // CSRF token endpoint (must be public)
                    "/api/public/**",         // Public API endpoints
                    "/health",                // Health check
                    "/actuator/health",       // Actuator health
                    "/error",                 // Error page
                    "/favicon.ico"            // Favicon
                ).permitAll()
                
                // Static resources - no authentication required
                .requestMatchers(
                    "/static/**",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/webjars/**"
                ).permitAll()
                
                // API documentation endpoints (if enabled)
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            
            // Configure logout
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessUrl("/api/auth/logout-success")
                .invalidateHttpSession(true)
                .deleteCookies("REALMSESSIONID", "XSRF-TOKEN")
                .clearAuthentication(true)
                .permitAll()
            )
            
            // Configure security headers
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.deny())  // Prevent clickjacking
                .contentTypeOptions(contentTypeOptions -> {})       // Prevent MIME type sniffing
                .httpStrictTransportSecurity(hstsConfig -> 
                    hstsConfig
                        .maxAgeInSeconds(31536000)        // 1 year
                        .includeSubDomains(true)
                        .preload(true)
                )
                .referrerPolicy(referrerPolicy -> 
                    referrerPolicy.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )
            )
            
            // Set authentication provider
            .authenticationProvider(authenticationProvider())
            
            // Add custom session authentication filter to restore SecurityContext from session
            .addFilterBefore(sessionAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        log.info("Redis session-based security filter chain configured successfully");
        return http.build();
    }
}