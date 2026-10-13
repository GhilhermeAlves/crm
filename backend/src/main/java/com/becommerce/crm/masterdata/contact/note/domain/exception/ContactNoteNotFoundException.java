package com.becommerce.crm.masterdata.contact.note.domain.exception;

import java.util.UUID;

public class ContactNoteNotFoundException extends RuntimeException {

    public ContactNoteNotFoundException(UUID id) {
        super("Anotação não encontrada: " + id);
    }
}
