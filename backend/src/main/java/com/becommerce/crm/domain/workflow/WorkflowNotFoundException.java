package com.becommerce.crm.domain.workflow;

import java.util.UUID;

public class WorkflowNotFoundException extends RuntimeException {
    public WorkflowNotFoundException(UUID id) {
        super("Workflow não encontrado: " + id);
    }
}
