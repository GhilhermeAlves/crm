package com.becommerce.crm.sales.pipeline.application.port.out;

import com.becommerce.crm.sales.pipeline.domain.Stage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StageRepository {

    Stage save(Stage stage);

    Optional<Stage> findById(UUID id);

    List<Stage> findByPipelineIdOrdered(UUID pipelineId);

    List<Stage> findByCompanyId(UUID companyId);

    int countByPipelineId(UUID pipelineId);

    void delete(Stage stage);
}
