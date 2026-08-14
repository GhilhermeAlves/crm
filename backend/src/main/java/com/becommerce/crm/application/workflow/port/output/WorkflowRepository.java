package com.becommerce.crm.application.workflow.port.output;

import com.becommerce.crm.domain.workflow.TriggerEvent;
import com.becommerce.crm.domain.workflow.Workflow;

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
