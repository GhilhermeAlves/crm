package com.becommerce.crm.masterdata.anamnesis.domain.exception;

import java.util.UUID;

/** Modelo de anamnese inexistente ou pertencente a outro tenant (resposta 404). */
public class AnamnesisModelNotFoundException extends RuntimeException {

    public AnamnesisModelNotFoundException(UUID id) {
        super("Modelo de anamnese não encontrado: " + id);
    }
}
