package com.becommerce.crm.masterdata.person;

import org.springframework.http.HttpStatus;

import java.util.List;

/** Erro de regra do cadastro de pessoas, com o status HTTP e, na validação, os campos pendentes. */
public class PersonException extends RuntimeException {

    public record MissingField(String fieldKey, String label) {}

    private final HttpStatus status;
    private final String code;
    private final List<MissingField> fields;

    public PersonException(HttpStatus status, String code, String message) {
        this(status, code, message, List.of());
    }

    public PersonException(HttpStatus status, String code, String message, List<MissingField> fields) {
        super(message);
        this.status = status;
        this.code = code;
        this.fields = fields == null ? List.of() : List.copyOf(fields);
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public List<MissingField> fields() {
        return fields;
    }

    public static PersonException notFound(String what) {
        return new PersonException(HttpStatus.NOT_FOUND, "NOT_FOUND", what + " não encontrado.");
    }

    public static PersonException badRequest(String message) {
        return new PersonException(HttpStatus.BAD_REQUEST, "INVALID", message);
    }

    public static PersonException conflict(String code, String message) {
        return new PersonException(HttpStatus.CONFLICT, code, message);
    }

    public static PersonException forbidden(String message) {
        return new PersonException(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }
}
