package com.becommerce.crm.clinical.anamnesis.application.dto;

import java.util.Map;
import java.util.UUID;

/**
 * Salva as respostas de uma anamnese em rascunho. Quando {@code complete} é
 * verdadeiro, valida as perguntas obrigatórias e conclui a anamnese.
 */
public record SavePatientAnamnesisRequest(
        Map<UUID, PatientAnamnesisAnswer> answers,
        boolean complete) {
}
