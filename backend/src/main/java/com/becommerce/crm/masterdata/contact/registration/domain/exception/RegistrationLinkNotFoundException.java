package com.becommerce.crm.masterdata.contact.registration.domain.exception;

/** Token de cadastro inexistente (404). */
public class RegistrationLinkNotFoundException extends RuntimeException {
    public RegistrationLinkNotFoundException() {
        super("Link de cadastro inválido ou inexistente.");
    }
}
