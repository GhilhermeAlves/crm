package com.becommerce.crm.sales.holiday.web;

import com.becommerce.crm.sales.holiday.domain.exception.HolidayNotFoundException;
import com.becommerce.crm.sales.holiday.domain.exception.HolidayValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice(assignableTypes = HolidayController.class)
public class HolidayExceptionHandler {

    @ExceptionHandler(HolidayNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleHolidayNotFoundException(HolidayNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "status", 404,
                        "error", "Not Found",
                        "message", ex.getMessage(),
                        "timestamp", LocalDateTime.now().toString()
                ));
    }

    @ExceptionHandler(HolidayValidationException.class)
    public ResponseEntity<Map<String, Object>> handleHolidayValidationException(HolidayValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "status", 400,
                        "error", "Bad Request",
                        "message", ex.getMessage(),
                        "timestamp", LocalDateTime.now().toString()
                ));
    }
}
