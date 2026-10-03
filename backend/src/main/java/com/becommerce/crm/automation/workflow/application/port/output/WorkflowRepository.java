package com.becommerce.crm.automation.workflow.application.port.output;

import com.becommerce.crm.automation.workflow.domain.TriggerEvent;
import com.becommerce.crm.automation.workflow.domain.Workflow;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkflowRepository {

    Workflow save(Workflow workflow);

    Optional<Workflow> findById(UUID id);

    List<Workflow> findByCompanyId(UUID companyId);

    List<Workflow> findByCompanyIdAndTriggerAndActive(UUID companyId, TriggerEvent trigger, boolean active);

    void delete(Workflow workflow);
}
