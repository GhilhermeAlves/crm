package com.becommerce.crm.sales.tag.web;

import com.becommerce.crm.sales.tag.domain.exception.TagNotFoundException;
import com.becommerce.crm.sales.tag.domain.exception.TagValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice(assignableTypes = TagController.class)
public class TagExceptionHandler {

    @ExceptionHandler(TagNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleTagNotFoundException(TagNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "status", 404,
                        "error", "Not Found",
                        "message", ex.getMessage(),
                        "timestamp", LocalDateTime.now().toString()
                ));
    }

    @ExceptionHandler(TagValidationException.class)
    public ResponseEntity<Map<String, Object>> handleTagValidationException(TagValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "status", 400,
                        "error", "Bad Request",
                        "message", ex.getMessage(),
                        "timestamp", LocalDateTime.now().toString()
                ));
    }
}
