package com.becommerce.crm.masterdata.contact.registration.domain.exception;

/** Link de cadastro expirado ou já utilizado (410). */
public class RegistrationLinkNoLongerValidException extends RuntimeException {
    public RegistrationLinkNoLongerValidException(String message) {
        super(message);
    }
}
