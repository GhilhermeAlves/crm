package com.becommerce.crm.masterdata.anamnesis.application.port.output;

import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisSection;

import java.util.List;
import java.util.UUID;

/** Porta de saída das seções de anamnese ({@code anamnesis_sections}, V091). */
public interface AnamnesisSectionRepository {

    AnamnesisSection save(AnamnesisSection section);

    List<AnamnesisSection> findByModelIdOrderBySortOrder(UUID modelId);

    void deleteByModelId(UUID modelId);
}
