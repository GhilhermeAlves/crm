package com.becommerce.crm.masterdata.contact.domain.exception;

import java.util.UUID;

public class ContactNotFoundException extends RuntimeException {

    public ContactNotFoundException(UUID id) {
        super("Contato não encontrado: " + id);
    }
}