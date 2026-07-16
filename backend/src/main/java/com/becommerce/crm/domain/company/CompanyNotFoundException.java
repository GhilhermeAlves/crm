package com.becommerce.crm.domain.company;

public class CompanyNotFoundException extends RuntimeException {

    public CompanyNotFoundException(String identifier) {
        super("Empresa não encontrada: " + identifier);
    }

    public CompanyNotFoundException(java.util.UUID id) {
        super("Empresa não encontrada com ID: " + id);
    }

    public CompanyNotFoundException(String field, String value) {
        super("Empresa não encontrada com " + field + ": " + value);
    }
}
