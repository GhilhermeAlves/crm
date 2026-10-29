package com.becommerce.crm.masterdata.anamnesis.application.port.output;

import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisModel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de saída dos modelos de anamnese ({@code anamnesis_models}, V091). RLS FORCE isola o tenant. */
public interface AnamnesisModelRepository {

    AnamnesisModel save(AnamnesisModel model);

    /** Grava já no banco: necessário antes de marcar outro padrão (índice único parcial). */
    AnamnesisModel saveAndFlush(AnamnesisModel model);

    Optional<AnamnesisModel> findById(UUID id);

    List<AnamnesisModel> findByCompanyIdOrderByName(UUID companyId);

    boolean existsByCompanyId(UUID companyId);

    void delete(AnamnesisModel model);
}
