package com.becommerce.crm.masterdata.company.domain;

public class CompanyAlreadyExistsException extends RuntimeException {

    public CompanyAlreadyExistsException(String cnpj) {
        super("Empresa já existe com CNPJ: " + cnpj);
    }

    public CompanyAlreadyExistsException(String field, String value) {
        super("Empresa já existe com " + field + ": " + value);
    }
}
