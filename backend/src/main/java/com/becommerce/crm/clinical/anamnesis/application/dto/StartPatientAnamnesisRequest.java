package com.becommerce.crm.clinical.anamnesis.application.dto;

import java.util.UUID;

/** Início de uma anamnese; {@code modelId} nulo usa o modelo padrão da empresa. */
public record StartPatientAnamnesisRequest(UUID modelId) {
}
