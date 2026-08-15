package com.trio.backend.exception;

import com.trio.backend.ai.exception.AIConfigurationException;
import com.trio.backend.ai.exception.AIConnectionException;
import com.trio.backend.ai.exception.AIException;
import com.trio.backend.ai.exception.AIProviderException;
import com.trio.backend.ai.exception.AIResponseException;
import com.trio.backend.common.ApiError;
import com.trio.backend.common.ApiResponse;
import com.trio.backend.reporting.ReportException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(
            ResourceNotFoundException ex
    ) {
        log.debug("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(
            BadRequestException ex
    ) {
        log.debug("Bad request: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(
            ConflictException ex
    ) {
        log.debug("Conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(
            UnauthorizedException ex
    ) {
        log.debug("Unauthorized: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.failure(ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(
            ForbiddenException ex
    ) {
        log.debug("Forbidden: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failure(ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            AccessDeniedException ex
    ) {
        log.debug("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failure("Access denied."));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(
            BadCredentialsException ex
    ) {
        log.warn("Bad credentials: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.failure("Invalid email or password."));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(
            AuthenticationException ex
    ) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.failure("Authentication failed."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<ApiError>>> handleValidation(
            MethodArgumentNotValidException ex
    ) {
        log.debug("Validation failed: {}", ex.getMessage());
        List<ApiError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError(error.getField(), error.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure("Validation failed.", errors));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            ValidationException ex
    ) {
        log.debug("Validation exception: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(ex.getMessage()));
    }

    @ExceptionHandler(ReportException.class)
    public ResponseEntity<ApiResponse<Void>> handleReportException(
            ReportException ex
    ) {
        log.debug("Report exception: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(ex.getMessage()));
    }

    @ExceptionHandler(AIConfigurationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAIConfiguration(
            AIConfigurationException ex
    ) {
        log.debug("AI configuration error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(ex.getMessage()));
    }

    @ExceptionHandler(AIConnectionException.class)
    public ResponseEntity<ApiResponse<Void>> handleAIConnection(
            AIConnectionException ex
    ) {
        log.debug("AI connection error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.failure(ex.getMessage()));
    }

    @ExceptionHandler(AIProviderException.class)
    public ResponseEntity<ApiResponse<Void>> handleAIProvider(
            AIProviderException ex
    ) {
        log.debug("AI provider error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.failure(ex.getMessage()));
    }

    @ExceptionHandler(AIResponseException.class)
    public ResponseEntity<ApiResponse<Void>> handleAIResponse(
            AIResponseException ex
    ) {
        log.debug("AI response error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(ex.getMessage()));
    }

    @ExceptionHandler(AIException.class)
    public ResponseEntity<ApiResponse<Void>> handleAIException(
            AIException ex
    ) {
        log.debug("AI error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex
    ) {
        log.debug("Method argument type mismatch: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure("Invalid parameter: " + ex.getName()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(
            NoResourceFoundException ex
    ) {
        log.debug("No handler/resource found for {}", ex.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure("Resource not found."));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLockingFailure(
            ObjectOptimisticLockingFailureException ex
    ) {
        log.warn("Optimistic locking failure: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure("Concurrent update detected. Please retry."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex
    ) {
        if (containsConstraint(ex, "uk_projects_department_id_name")) {
            log.debug("Project name uniqueness constraint violation: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.failure("Project with this name already exists."));
        }
        log.error("Data integrity violation occurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("An unexpected error occurred."));
    }

    private boolean containsConstraint(Throwable ex, String constraintName) {
        Throwable current = ex;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains(constraintName.toLowerCase())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(
            Exception ex
    ) {
        log.error("Unexpected error occurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("An unexpected error occurred."));
    }

}
