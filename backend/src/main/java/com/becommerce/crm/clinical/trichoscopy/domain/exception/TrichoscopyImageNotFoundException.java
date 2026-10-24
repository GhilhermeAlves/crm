package com.becommerce.crm.clinical.trichoscopy.domain.exception;

import java.util.UUID;

public class TrichoscopyImageNotFoundException extends RuntimeException {

    public TrichoscopyImageNotFoundException(UUID id) {
        super("Imagem de tricoscopia não encontrada: " + id);
    }
}
