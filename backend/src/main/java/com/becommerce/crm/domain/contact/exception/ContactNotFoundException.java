package com.becommerce.crm.domain.contact.exception;

import java.util.UUID;

public class ContactNotFoundException extends RuntimeException {

    public ContactNotFoundException(UUID id) {
        super("Contato não encontrado: " + id);
    }
}