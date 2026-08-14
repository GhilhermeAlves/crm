package com.becommerce.crm.domain.workflow;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Regra de workflow (Item 1): pertence a uma Company (multi-tenant), possui um
 * trigger, um conjunto de condições (todas precisam ser verdadeiras) e um
 * conjunto de ações (executadas em ordem). Núcleo simples e extensível da
 * Sprint 14 — sem BPMN/scripts/linguagem própria.
 */
public class Workflow {

    private final UUID id;
    private final UUID companyId;
    private String name;
    private String description;
    private TriggerEvent trigger;
    private boolean active;
    private final List<WorkflowCondition> conditions = new ArrayList<>();
    private final List<WorkflowAction> actions = new ArrayList<>();
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Workflow(UUID id, UUID companyId, String name, String description, TriggerEvent trigger,
                     boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.name = name;
        this.description = description;
        this.trigger = trigger;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Workflow create(UUID companyId, String name, String description, TriggerEvent trigger) {
        if (name == null || name.isBlank()) {
            throw new WorkflowValidationException("Nome do workflow é obrigatório.");
        }
        if (trigger == null) {
            throw new WorkflowValidationException("O trigger do workflow é obrigatório.");
        }
        LocalDateTime now = LocalDateTime.now();
        return new Workflow(UUID.randomUUID(), companyId, name.trim(), description, trigger, false, now, now);
    }

    public static Workflow reconstitute(UUID id, UUID companyId, String name, String description,
                                        TriggerEvent trigger, boolean active,
                                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Workflow(id, companyId, name, description, trigger, active, createdAt, updatedAt);
    }

    public void update(String name, String description, TriggerEvent trigger) {
        if (name == null || name.isBlank()) {
            throw new WorkflowValidationException("Nome do workflow é obrigatório.");
        }
        if (trigger == null) {
            throw new WorkflowValidationException("O trigger do workflow é obrigatório.");
        }
        this.name = name.trim();
        this.description = description;
        this.trigger = trigger;
        touch();
    }

    public void activate() {
        this.active = true;
        touch();
    }

    public void deactivate() {
        this.active = false;
        touch();
    }

    public void addCondition(WorkflowCondition condition) {
        this.conditions.add(condition);
    }

    public void addAction(WorkflowAction action) {
        this.actions.add(action);
    }

    public void replaceConditions(List<WorkflowCondition> newConditions) {
        this.conditions.clear();
        this.conditions.addAll(newConditions);
    }

    public void replaceActions(List<WorkflowAction> newActions) {
        this.actions.clear();
        this.actions.addAll(newActions);
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    public List<WorkflowCondition> getConditions() { return List.copyOf(conditions); }
    public List<WorkflowAction> getActions() { return List.copyOf(actions); }
    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public TriggerEvent getTrigger() { return trigger; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}