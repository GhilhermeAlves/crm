import { describe, it, expect } from "vitest";
import {
  workflowFormToPayload,
  workflowToFormValues,
  workflowFormSchema,
  type WorkflowFormValues,
} from "./workflow.schema";
import type { Workflow } from "../types/workflow.types";

describe("workflowFormToPayload", () => {
  it("converte um formulário válido em payload", () => {
    const values: WorkflowFormValues = {
      name: "Follow-up de proposta",
      description: "Automação de follow-up",
      trigger: "OPPORTUNITY_STAGE_CHANGED",
      conditions: [{ field: "opportunity.stage", operator: "EQUALS", value: "Negociação" }],
      actions: [
        {
          actionType: "CREATE_TASK",
          title: "Fazer follow-up",
          description: "",
          priority: "MEDIUM",
          dueInDays: "2",
          activityType: "OTHER",
        },
        {
          actionType: "CREATE_ACTIVITY",
          title: "Registrar contato",
          description: "",
          priority: "MEDIUM",
          dueInDays: "",
          activityType: "CALL",
        },
      ],
    };

    const payload = workflowFormToPayload(values);

    expect(payload.name).toBe("Follow-up de proposta");
    expect(payload.trigger).toBe("OPPORTUNITY_STAGE_CHANGED");
    expect(payload.conditions).toHaveLength(1);
    expect(payload.conditions[0]).toMatchObject({
      sortOrder: 0,
      field: "opportunity.stage",
    });
    expect(payload.actions).toHaveLength(2);
    expect(payload.actions[0]).toMatchObject({
      actionType: "CREATE_TASK",
      sortOrder: 0,
      config: { title: "Fazer follow-up", priority: "MEDIUM", dueInDays: 2 },
    });
    expect(payload.actions[0].config.description).toBeUndefined();
    expect(payload.actions[1]).toMatchObject({
      actionType: "CREATE_ACTIVITY",
      sortOrder: 1,
      config: { subject: "Registrar contato", type: "CALL" },
    });
  });

  it("ignora condições vazias e dueInDays zero", () => {
    const values: WorkflowFormValues = {
      name: "Só criar atividade",
      trigger: "TASK_CREATED",
      conditions: [{ field: "", operator: "EQUALS", value: "" }],
      actions: [
        {
          actionType: "CREATE_TASK",
          title: "Tarefa",
          priority: "LOW",
          dueInDays: "0",
          activityType: "OTHER",
        },
      ],
    };

    const payload = workflowFormToPayload(values);

    expect(payload.conditions).toHaveLength(0);
    expect(payload.actions[0].config.dueInDays).toBeUndefined();
    expect(payload.actions[0].config.priority).toBe("LOW");
    expect(payload.description).toBeUndefined();
  });

  it("rejeita formulário sem ações", () => {
    const result = workflowFormSchema.safeParse({
      name: "Inválido",
      trigger: "ACTIVITY_CREATED",
      conditions: [],
      actions: [],
    });

    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toContain("ação");
    }
  });
});

describe("workflowToFormValues", () => {
  it("converte um workflow em valores de formulário", () => {
    const workflow: Workflow = {
      id: "wf-1",
      companyId: "co-1",
      name: "Follow-up",
      description: "Desc",
      trigger: "OPPORTUNITY_CREATED",
      active: true,
      conditions: [
        {
          id: "c1",
          field: "opportunity.value",
          operator: "GREATER_THAN",
          value: "5000",
          sortOrder: 0,
        },
      ],
      actions: [
        {
          id: "a1",
          actionType: "CREATE_TASK",
          sortOrder: 0,
          config: '{"title":"Entrar em contato","priority":"HIGH","dueInDays":1}',
        },
      ],
      createdAt: "2026-01-01T00:00:00",
      updatedAt: "2026-01-01T00:00:00",
    };

    const values = workflowToFormValues(workflow);

    expect(values.name).toBe("Follow-up");
    expect(values.trigger).toBe("OPPORTUNITY_CREATED");
    expect(values.conditions[0]).toMatchObject({
      field: "opportunity.value",
      value: "5000",
    });
    expect(values.actions[0]).toMatchObject({
      actionType: "CREATE_TASK",
      title: "Entrar em contato",
      priority: "HIGH",
      dueInDays: "1",
    });
  });

  it("interpreta cron como assunto da atividade", () => {
    const workflow: Workflow = {
      id: "wf-2",
      companyId: "co-1",
      name: "Atividade",
      description: null,
      trigger: "ACTIVITY_CREATED",
      active: false,
      conditions: [],
      actions: [
        {
          id: "a1",
          actionType: "CREATE_ACTIVITY",
          sortOrder: 0,
          config: '{"subject":"Reagendar"}',
        },
      ],
      createdAt: "2026-01-01T00:00:00",
      updatedAt: "2026-01-01T00:00:00",
    };

    const values = workflowToFormValues(workflow);
    expect(values.actions[0].actionType).toBe("CREATE_ACTIVITY");
    expect(values.actions[0].title).toBe("Reagendar");
    expect(values.actions[0].activityType).toBe("OTHER");
  });
});
