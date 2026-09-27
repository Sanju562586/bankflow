package com.bankflow.payment.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DuplicateRequestException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateRequestException ex) {
        log.warn("Duplicate payment request: {}", ex.getMessage());
        Map<String, Object> err = new HashMap<>();
        err.put("timestamp", Instant.now().toString());
        err.put("status", HttpStatus.CONFLICT.value());
        err.put("error", "Conflict - Duplicate Request");
        err.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(PaymentNotFoundException ex) {
        log.warn("Payment not found: {}", ex.getMessage());
        Map<String, Object> err = new HashMap<>();
        err.put("timestamp", Instant.now().toString());
        err.put("status", HttpStatus.NOT_FOUND.value());
        err.put("error", "Not Found");
        err.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadArgs(IllegalArgumentException ex) {
        Map<String, Object> err = new HashMap<>();
        err.put("timestamp", Instant.now().toString());
        err.put("status", HttpStatus.BAD_REQUEST.value());
        err.put("error", "Bad Request");
        err.put("message", ex.getMessage());
        return ResponseEntity.badRequest().body(err);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        Map<String, Object> err = new HashMap<>();
        err.put("timestamp", Instant.now().toString());
        err.put("status", HttpStatus.BAD_REQUEST.value());
        err.put("error", "Validation Failed");
        err.put("details", fieldErrors);
        return ResponseEntity.badRequest().body(err);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Internal server error in payment-service: {}", ex.getMessage(), ex);
        Map<String, Object> err = new HashMap<>();
        err.put("timestamp", Instant.now().toString());
        err.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        err.put("error", "Internal Server Error");
        err.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
    }
}
