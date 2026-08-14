package com.becommerce.crm.infrastructure.workflow.persistence;

import com.becommerce.crm.application.workflow.port.output.WorkflowRepository;
import com.becommerce.crm.domain.workflow.ConditionOperator;
import com.becommerce.crm.domain.workflow.TriggerEvent;
import com.becommerce.crm.domain.workflow.Workflow;
import com.becommerce.crm.domain.workflow.WorkflowAction;
import com.becommerce.crm.domain.workflow.WorkflowCondition;
import com.becommerce.crm.domain.workflow.ActionType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class WorkflowRepositoryImpl implements WorkflowRepository {

    private final WorkflowJpaRepository jpaRepository;
    private final WorkflowConditionJpaRepository conditionJpaRepository;
    private final WorkflowActionJpaRepository actionJpaRepository;

    public WorkflowRepositoryImpl(WorkflowJpaRepository jpaRepository,
                                  WorkflowConditionJpaRepository conditionJpaRepository,
                                  WorkflowActionJpaRepository actionJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.conditionJpaRepository = conditionJpaRepository;
        this.actionJpaRepository = actionJpaRepository;
    }

    @Override
    @Transactional
    public Workflow save(Workflow workflow) {
        WorkflowJpaEntity entity = toEntity(workflow);
        jpaRepository.save(entity);

        replaceChildren(workflow);
        return workflow;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Workflow> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Workflow> findByCompanyId(UUID companyId) {
        return jpaRepository.findByCompanyId(companyId).stream()
                .map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Workflow> findByCompanyIdAndTriggerAndActive(UUID companyId, TriggerEvent trigger, boolean active) {
        return jpaRepository.findByCompanyIdAndTriggerAndActive(companyId, trigger.name(), active).stream()
                .map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void delete(Workflow workflow) {
        conditionJpaRepository.deleteByWorkflowId(workflow.getId());
        actionJpaRepository.deleteByWorkflowId(workflow.getId());
        jpaRepository.deleteById(workflow.getId());
    }

    private void replaceChildren(Workflow workflow) {
        conditionJpaRepository.deleteByWorkflowId(workflow.getId());
        conditionJpaRepository.saveAll(workflow.getConditions().stream()
                .map(this::toConditionEntity).toList());

        actionJpaRepository.deleteByWorkflowId(workflow.getId());
        actionJpaRepository.saveAll(workflow.getActions().stream()
                .map(this::toActionEntity).toList());
    }

    private Workflow toDomain(WorkflowJpaEntity e) {
        Workflow workflow = Workflow.reconstitute(e.getId(), e.getCompanyId(), e.getName(), e.getDescription(),
                TriggerEvent.valueOf(e.getTrigger()), Boolean.TRUE.equals(e.getActive()),
                e.getCreatedAt(), e.getUpdatedAt());
        conditionJpaRepository.findByWorkflowId(e.getId()).stream()
                .sorted(Comparator.comparingInt(WorkflowConditionJpaEntity::getSortOrder))
                .forEach(c -> workflow.addCondition(WorkflowCondition.reconstitute(c.getId(), c.getCompanyId(),
                        c.getWorkflowId(), c.getField(), ConditionOperator.valueOf(c.getOperator()),
                        c.getValue(), c.getSortOrder())));
        actionJpaRepository.findByWorkflowId(e.getId()).stream()
                .sorted(Comparator.comparingInt(WorkflowActionJpaEntity::getSortOrder))
                .forEach(a -> workflow.addAction(WorkflowAction.reconstitute(a.getId(), a.getCompanyId(),
                        a.getWorkflowId(), ActionType.valueOf(a.getActionType()), a.getSortOrder(), a.getConfig())));
        return workflow;
    }

    private static WorkflowJpaEntity toEntity(Workflow w) {
        WorkflowJpaEntity e = new WorkflowJpaEntity();
        e.setId(w.getId());
        e.setCompanyId(w.getCompanyId());
        e.setName(w.getName());
        e.setDescription(w.getDescription());
        e.setTrigger(w.getTrigger().name());
        e.setActive(w.isActive());
        e.setCreatedAt(w.getCreatedAt());
        e.setUpdatedAt(w.getUpdatedAt());
        return e;
    }

    private WorkflowConditionJpaEntity toConditionEntity(WorkflowCondition c) {
        WorkflowConditionJpaEntity e = new WorkflowConditionJpaEntity();
        e.setId(c.getId());
        e.setCompanyId(c.getCompanyId());
        e.setWorkflowId(c.getWorkflowId());
        e.setField(c.getField());
        e.setOperator(c.getOperator().name());
        e.setValue(c.getValue());
        e.setSortOrder(c.getSortOrder());
        return e;
    }

    private WorkflowActionJpaEntity toActionEntity(WorkflowAction a) {
        WorkflowActionJpaEntity e = new WorkflowActionJpaEntity();
        e.setId(a.getId());
        e.setCompanyId(a.getCompanyId());
        e.setWorkflowId(a.getWorkflowId());
        e.setActionType(a.getActionType().name());
        e.setSortOrder(a.getSortOrder());
        e.setConfig(a.getConfig());
        return e;
    }
}