package com.becommerce.crm.domain.company;

public class CompanyAlreadyExistsException extends RuntimeException {

    public CompanyAlreadyExistsException(String cnpj) {
        super("Empresa já existe com CNPJ: " + cnpj);
    }

    public CompanyAlreadyExistsException(String field, String value) {
        super("Empresa já existe com " + field + ": " + value);
    }
}
