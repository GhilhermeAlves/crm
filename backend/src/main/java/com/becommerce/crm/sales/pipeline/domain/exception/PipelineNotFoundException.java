package com.becommerce.crm.sales.pipeline.domain.exception;

import java.util.UUID;

public class PipelineNotFoundException extends RuntimeException {

    public PipelineNotFoundException(UUID id) {
        super("Pipeline não encontrado: " + id);
    }
}
