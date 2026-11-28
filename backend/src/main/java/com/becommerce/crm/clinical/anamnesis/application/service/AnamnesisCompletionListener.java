package com.becommerce.crm.clinical.anamnesis.application.service;

import java.util.UUID;

/** Avisado quando uma anamnese é concluída (mesma transação). */
public interface AnamnesisCompletionListener {

    void onCompleted(UUID companyId, UUID contactId, UUID anamnesisId);
}
