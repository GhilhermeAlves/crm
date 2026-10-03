package com.becommerce.crm.automation.workflow.domain;

import java.util.UUID;

public class WorkflowNotFoundException extends RuntimeException {
    public WorkflowNotFoundException(UUID id) {
        super("Workflow não encontrado: " + id);
    }
}
