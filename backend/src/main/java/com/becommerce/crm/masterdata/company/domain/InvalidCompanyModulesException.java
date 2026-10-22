package com.becommerce.crm.masterdata.company.domain;

/** Segmento ou módulo inválido na configuração da empresa (HTTP 400). */
public class InvalidCompanyModulesException extends RuntimeException {

    public InvalidCompanyModulesException(String message) {
        super(message);
    }
}
