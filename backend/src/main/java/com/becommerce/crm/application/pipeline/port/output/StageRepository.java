package com.becommerce.crm.application.pipeline.port.output;

import com.becommerce.crm.domain.pipeline.Stage;

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
