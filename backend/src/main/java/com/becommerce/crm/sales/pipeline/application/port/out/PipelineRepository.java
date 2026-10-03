package com.becommerce.crm.sales.pipeline.application.port.out;

import com.becommerce.crm.sales.pipeline.domain.Pipeline;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PipelineRepository {

    Pipeline save(Pipeline pipeline);

    Optional<Pipeline> findById(UUID id);

    List<Pipeline> findByCompanyId(UUID companyId);

    void delete(Pipeline pipeline);
}
