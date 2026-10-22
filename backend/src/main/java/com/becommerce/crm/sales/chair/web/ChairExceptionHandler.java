package com.becommerce.crm.sales.chair.web;

import com.becommerce.crm.sales.chair.domain.exception.ChairNotFoundException;
import com.becommerce.crm.sales.chair.domain.exception.ChairValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice(assignableTypes = ChairController.class)
public class ChairExceptionHandler {

    @ExceptionHandler(ChairNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleChairNotFoundException(ChairNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "status", 404,
                        "error", "Not Found",
                        "message", ex.getMessage(),
                        "timestamp", LocalDateTime.now().toString()
                ));
    }

    @ExceptionHandler(ChairValidationException.class)
    public ResponseEntity<Map<String, Object>> handleChairValidationException(ChairValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "status", 400,
                        "error", "Bad Request",
                        "message", ex.getMessage(),
                        "timestamp", LocalDateTime.now().toString()
                ));
    }
}
