package com.becommerce.crm.domain.storage.exception;

import java.util.UUID;

public class StorageObjectNotFoundException extends RuntimeException {

    public StorageObjectNotFoundException(UUID id) {
        super("Arquivo não encontrado: " + id);
    }
}
