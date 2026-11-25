package com.becommerce.crm.masterdata.person;

import com.becommerce.crm.masterdata.location.LocationController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/** Respostas de erro do cadastro de pessoas: 400/404/409/422 com mensagem e campos pendentes. */
@RestControllerAdvice(assignableTypes = {PersonController.class, PersonListsController.class, LocationController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PersonExceptionHandler {

    @ExceptionHandler(PersonException.class)
    public ResponseEntity<Map<String, Object>> handle(PersonException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", e.status().value());
        body.put("code", e.code());
        body.put("message", e.getMessage());
        if (!e.fields().isEmpty()) body.put("fields", e.fields());
        return ResponseEntity.status(e.status()).body(body);
    }
}
