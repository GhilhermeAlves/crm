package com.becommerce.crm.clinical.anamnesis.application.dto;

/**
 * Resposta de uma pergunta: valor livre (string, número, booleano, lista de
 * strings ou nulo) e complemento opcional.
 */
public record PatientAnamnesisAnswer(Object value, String complement) {
}
