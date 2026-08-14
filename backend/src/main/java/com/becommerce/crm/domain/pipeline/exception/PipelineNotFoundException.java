package com.becommerce.crm.domain.pipeline.exception;

import java.util.UUID;

public class PipelineNotFoundException extends RuntimeException {

    public PipelineNotFoundException(UUID id) {
        super("Pipeline não encontrado: " + id);
    }
}
