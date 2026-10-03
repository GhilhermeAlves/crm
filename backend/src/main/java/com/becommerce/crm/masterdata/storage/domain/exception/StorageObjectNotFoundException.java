package com.becommerce.crm.masterdata.storage.domain.exception;

import java.util.UUID;

public class StorageObjectNotFoundException extends RuntimeException {

    public StorageObjectNotFoundException(UUID id) {
        super("Arquivo não encontrado: " + id);
    }
}
