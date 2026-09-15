package com.arcarshowcaseserver.exceptions;

import com.sricharan.security.core.exception.SecurityAuthorizationException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<Map<String, Object>> handleRestClientException(RestClientException ex) {
        log.error("Downstream service call failed", ex);
        return body(HttpStatus.SERVICE_UNAVAILABLE, "External API Error",
                "A downstream service is temporarily unavailable. Please try again.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String requiredType = ex.getRequiredType() == null ? "the expected type" : ex.getRequiredType().getSimpleName();
        return body(HttpStatus.BAD_REQUEST, "Invalid Parameter",
                "Parameter '%s' should be of type %s".formatted(ex.getName(), requiredType));
    }

    /**
     * {@code @Valid} failures on a request body. Without this the closest match is the
     * catch-all below, which answered every malformed signup or password reset with a 500.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleBodyValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() == null ? "is invalid" : error.getDefaultMessage(),
                        (first, second) -> first,
                        LinkedHashMap::new));

        String message = fieldErrors.entrySet().stream()
                .map(entry -> entry.getKey() + " " + entry.getValue())
                .collect(Collectors.joining("; "));

        ResponseEntity<Map<String, Object>> response = body(HttpStatus.BAD_REQUEST, "Validation Error",
                message.isBlank() ? "Request contains invalid values." : message);
        response.getBody().put("fieldErrors", fieldErrors);
        return response;
    }

    /** {@code @Validated} failures on path variables and request params. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .collect(Collectors.joining("; "));

        return body(HttpStatus.BAD_REQUEST, "Validation Error",
                message.isBlank() ? "Request contains invalid values." : message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return body(HttpStatus.BAD_REQUEST, "Bad Request Error", "Request body is missing or malformed.");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        return body(HttpStatus.valueOf(ex.getStatusCode().value()), "Validation Error", ex.getReason());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return body(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    @ExceptionHandler({BadRequestException.class, InvalidInputException.class})
    public ResponseEntity<Map<String, Object>> handleBadRequest(RuntimeException ex) {
        return body(HttpStatus.BAD_REQUEST, "Bad Request Error", ex.getMessage());
    }

    @ExceptionHandler(DuplicateLikeException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateLike(DuplicateLikeException ex) {
        return body(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(SecurityAuthorizationException.class)
    public ResponseEntity<Map<String, Object>> handleSecurityAuthorization(SecurityAuthorizationException ex) {
        return body(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage());
    }

    /**
     * Anything unplanned. The message is deliberately generic: the previous version
     * returned {@code ex.getMessage()}, which leaked internal service URLs, SQL
     * fragments and class names to the mobile client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        log.error("Unhandled exception", ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "Something went wrong on our side. Please try again.");
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String error, String message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("error", error);
        payload.put("message", message);
        return new ResponseEntity<>(payload, status);
    }
}
