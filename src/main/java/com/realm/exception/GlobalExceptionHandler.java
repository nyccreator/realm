package com.realm.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global Exception Handler for the Realm PKM system.
 * 
 * This handler provides centralized exception handling across all controllers,
 * ensuring consistent error responses and comprehensive logging for security
 * and debugging purposes.
 * 
 * Features:
 * - Structured JSON error responses
 * - Security-aware logging
 * - Validation error handling
 * - Authentication and authorization error handling
 * - Generic exception fallback
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    /**
     * Handle validation errors from @Valid annotations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        log.warn("Validation error in request to {}: {}", 
                request.getDescription(false), ex.getMessage());
        
        Map<String, Object> errorResponse = createErrorResponse(
            "Validation Failed",
            "Invalid input data",
            HttpStatus.BAD_REQUEST.value(),
            request
        );
        
        // Add field-specific errors
        Map<String, String> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                FieldError::getDefaultMessage,
                (existing, replacement) -> existing // Keep first error for duplicate fields
            ));
        
        errorResponse.put("fieldErrors", fieldErrors);
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handle bind exceptions (form data validation)
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, Object>> handleBindExceptions(
            BindException ex, WebRequest request) {
        
        log.warn("Bind error in request to {}: {}", 
                request.getDescription(false), ex.getMessage());
        
        Map<String, Object> errorResponse = createErrorResponse(
            "Validation Failed",
            "Invalid form data",
            HttpStatus.BAD_REQUEST.value(),
            request
        );
        
        Map<String, String> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                FieldError::getDefaultMessage,
                (existing, replacement) -> existing
            ));
        
        errorResponse.put("fieldErrors", fieldErrors);
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handle custom authentication exceptions
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {
        
        log.warn("Authentication error in request to {}: {}", 
                request.getDescription(false), ex.getMessage());
        
        Map<String, Object> errorResponse = createErrorResponse(
            "Authentication Failed",
            ex.getMessage(),
            HttpStatus.UNAUTHORIZED.value(),
            request
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
    
    /**
     * Handle Spring Security authentication exceptions
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentialsException(
            BadCredentialsException ex, WebRequest request) {
        
        log.warn("Bad credentials in request to {}: {}", 
                request.getDescription(false), ex.getMessage());
        
        Map<String, Object> errorResponse = createErrorResponse(
            "Authentication Failed",
            "Invalid email or password",
            HttpStatus.UNAUTHORIZED.value(),
            request
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
    
    /**
     * Handle user not found exceptions
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUsernameNotFoundException(
            UsernameNotFoundException ex, WebRequest request) {
        
        log.warn("User not found in request to {}: {}", 
                request.getDescription(false), ex.getMessage());
        
        Map<String, Object> errorResponse = createErrorResponse(
            "User Not Found",
            "Invalid email or password", // Don't reveal if user exists
            HttpStatus.UNAUTHORIZED.value(),
            request
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
    
    /**
     * Handle access denied exceptions
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        
        log.warn("Access denied in request to {}: {}", 
                request.getDescription(false), ex.getMessage());
        
        Map<String, Object> errorResponse = createErrorResponse(
            "Access Denied",
            "You don't have permission to access this resource",
            HttpStatus.FORBIDDEN.value(),
            request
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }
    
    /**
     * Handle user already exists exceptions
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyExistsException(
            UserAlreadyExistsException ex, WebRequest request) {
        
        log.warn("User already exists in request to {}: {}", 
                request.getDescription(false), ex.getMessage());
        
        Map<String, Object> errorResponse = createErrorResponse(
            "Registration Failed",
            ex.getMessage(),
            HttpStatus.CONFLICT.value(),
            request
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }
    
    /**
     * Handle custom validation exceptions
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            ValidationException ex, WebRequest request) {
        
        log.warn("Validation error in request to {}: {}", 
                request.getDescription(false), ex.getMessage());
        
        Map<String, Object> errorResponse = createErrorResponse(
            "Validation Error",
            ex.getMessage(),
            HttpStatus.BAD_REQUEST.value(),
            request
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handle note not found exceptions
     */
    @ExceptionHandler(NoteNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoteNotFoundException(
            NoteNotFoundException ex, WebRequest request) {
        
        log.warn("Note not found in request to {}: {}", 
                request.getDescription(false), ex.getMessage());
        
        Map<String, Object> errorResponse = createErrorResponse(
            "Note Not Found",
            ex.getMessage(),
            HttpStatus.NOT_FOUND.value(),
            request
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    /**
     * Handle note link not found exceptions
     */
    @ExceptionHandler(NoteLinkNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoteLinkNotFoundException(
            NoteLinkNotFoundException ex, WebRequest request) {
        
        log.warn("Note link not found in request to {}: {}", 
                request.getDescription(false), ex.getMessage());
        
        Map<String, Object> errorResponse = createErrorResponse(
            "Note Link Not Found",
            ex.getMessage(),
            HttpStatus.NOT_FOUND.value(),
            request
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    /**
     * Handle note access denied exceptions
     */
    @ExceptionHandler(NoteAccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleNoteAccessDeniedException(
            NoteAccessDeniedException ex, WebRequest request) {
        
        log.warn("Note access denied in request to {}: {}", 
                request.getDescription(false), ex.getMessage());
        
        Map<String, Object> errorResponse = createErrorResponse(
            "Access Denied",
            "You don't have permission to access this note",
            HttpStatus.FORBIDDEN.value(),
            request
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }
    
    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        log.warn("Illegal argument in request to {}: {}", 
                request.getDescription(false), ex.getMessage());
        
        Map<String, Object> errorResponse = createErrorResponse(
            "Invalid Request",
            "Invalid request parameters",
            HttpStatus.BAD_REQUEST.value(),
            request
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, WebRequest request) {
        
        log.error("Unexpected error in request to {}: {}", 
                request.getDescription(false), ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = createErrorResponse(
            "Internal Server Error",
            "An unexpected error occurred. Please try again later.",
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            request
        );
        
        // Add exception details in debug logs but not in response for security
        if (log.isDebugEnabled()) {
            errorResponse.put("debugInfo", ex.getClass().getSimpleName());
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * Create a standardized error response
     */
    private Map<String, Object> createErrorResponse(String error, String message, 
                                                   int status, WebRequest request) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        errorResponse.put("status", status);
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("path", extractPath(request));
        
        return errorResponse;
    }
    
    /**
     * Extract the request path from WebRequest
     */
    private String extractPath(WebRequest request) {
        String description = request.getDescription(false);
        if (description.startsWith("uri=")) {
            return description.substring(4);
        }
        return description;
    }
}