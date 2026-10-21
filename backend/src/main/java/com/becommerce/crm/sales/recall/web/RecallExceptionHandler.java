package com.becommerce.crm.sales.recall.web;

import com.becommerce.crm.sales.recall.domain.exception.RecallNotFoundException;
import com.becommerce.crm.sales.recall.domain.exception.RecallValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice(assignableTypes = RecallController.class)
public class RecallExceptionHandler {

    @ExceptionHandler(RecallNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleRecallNotFoundException(RecallNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "status", 404,
                        "error", "Not Found",
                        "message", ex.getMessage(),
                        "timestamp", LocalDateTime.now().toString()
                ));
    }

    @ExceptionHandler(RecallValidationException.class)
    public ResponseEntity<Map<String, Object>> handleRecallValidationException(RecallValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "status", 400,
                        "error", "Bad Request",
                        "message", ex.getMessage(),
                        "timestamp", LocalDateTime.now().toString()
                ));
    }
}
