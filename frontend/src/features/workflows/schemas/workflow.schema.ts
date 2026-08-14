import { z } from "zod";
import { useAuthorization } from "@/features/auth/hooks/useAuthorization";
import type {
  ConditionOperator,
  CreateWorkflowRequest,
  UpdateWorkflowRequest,
  Workflow,
  WorkflowActionType,
  WorkflowConditionInput,
} from "../types/workflow.types";

export const WORKFLOW_TRIGGERS = [
  "OPPORTUNITY_CREATED",
  "OPPORTUNITY_STAGE_CHANGED",
  "OPPORTUNITY_WON",
  "OPPORTUNITY_LOST",
  "TASK_CREATED",
  "TASK_COMPLETED",
  "ACTIVITY_CREATED",
] as const;

export const CONDITION_OPERATORS = [
  "EQUALS",
  "NOT_EQUALS",
  "GREATER_THAN",
  "LESS_THAN",
  "GREATER_OR_EQUAL",
  "LESS_OR_EQUAL",
] as const;

export const WORKFLOW_ACTION_TYPES = ["CREATE_TASK", "CREATE_ACTIVITY"] as const;

export const TASK_PRIORITIES = ["LOW", "MEDIUM", "HIGH"] as const;

export const ACTIVITY_TYPES = [
  "CALL",
  "MEETING",
  "EMAIL",
  "MESSAGE",
  "NOTE",
  "PROPOSAL",
  "FOLLOW_UP",
  "OTHER",
] as const;

export const CONDITION_OPERATOR_LABELS: Record<string, string> = {
  EQUALS: "Igual a",
  NOT_EQUALS: "Diferente de",
  GREATER_THAN: "Maior que",
  LESS_THAN: "Menor que",
  GREATER_OR_EQUAL: "Maior ou igual",
  LESS_OR_EQUAL: "Menor ou igual",
};

export const TASK_PRIORITY_LABELS: Record<string, string> = {
  LOW: "Baixa",
  MEDIUM: "Média",
  HIGH: "Alta",
};

export const ACTIVITY_TYPE_LABELS: Record<string, string> = {
  CALL: "Ligação",
  MEETING: "Reunião",
  EMAIL: "E-mail",
  MESSAGE: "Mensagem",
  NOTE: "Anotação",
  PROPOSAL: "Proposta",
  FOLLOW_UP: "Follow-up",
  OTHER: "Outro",
};

const conditionSchema = z.object({
  field: z.string().min(1, "Campo é obrigatório"),
  operator: z.enum(CONDITION_OPERATORS),
  value: z.string().min(1, "Valor é obrigatório"),
});

const actionSchema = z.object({
  actionType: z.enum(WORKFLOW_ACTION_TYPES),
  title: z.string().optional(),
  description: z.string().optional(),
  priority: z.enum(TASK_PRIORITIES).optional(),
  dueInDays: z.string().optional(),
  activityType: z.enum(ACTIVITY_TYPES).optional(),
});

export const workflowFormSchema = z.object({
  name: z
    .string()
    .min(1, "Nome é obrigatório")
    .max(120, "Nome deve ter no máximo 120 caracteres"),
  description: z.string().max(2000, "Descrição muito longa").optional(),
  trigger: z.enum(WORKFLOW_TRIGGERS),
  conditions: z.array(conditionSchema),
  actions: z.array(actionSchema).min(1, "Adicione pelo menos uma ação"),
});

export type WorkflowFormValues = z.infer<typeof workflowFormSchema>;

export function workflowFormToPayload(
  values: WorkflowFormValues
): CreateWorkflowRequest | UpdateWorkflowRequest {
  const conditions: WorkflowConditionInput[] = values.conditions
    .filter((c) => c.field && c.value)
    .map((c, i) => ({
      id: null,
      field: c.field,
      operator: c.operator as ConditionOperator,
      value: c.value,
      sortOrder: i,
    }));

  const actions = values.actions.map((a, i) => {
    const config: Record<string, unknown> = {};
    if (a.actionType === "CREATE_TASK") {
      if (a.title) config.title = a.title;
      if (a.description) config.description = a.description;
      if (a.priority) config.priority = a.priority;
      const due = Number(a.dueInDays);
      if (a.dueInDays && !Number.isNaN(due) && due > 0) config.dueInDays = due;
    } else {
      if (a.title) config.subject = a.title;
      if (a.description) config.description = a.description;
      if (a.activityType) config.type = a.activityType;
    }
    return {
      id: null,
      actionType: a.actionType as WorkflowActionType,
      sortOrder: i,
      config,
    };
  });

  const payload: CreateWorkflowRequest = {
    name: values.name,
    trigger: values.trigger,
    conditions,
    actions,
  };
  if (values.description) payload.description = values.description;
  return payload;
}

function parseConfig(config: string): Record<string, unknown> {
  if (!config) return {};
  try {
    return JSON.parse(config) as Record<string, unknown>;
  } catch {
    return {};
  }
}

export function workflowToFormValues(workflow: Workflow): WorkflowFormValues {
  return {
    name: workflow.name,
    description: workflow.description ?? "",
    trigger: workflow.trigger,
    conditions: workflow.conditions.map((c) => ({
      field: c.field,
      operator: c.operator,
      value: c.value,
    })),
    actions: workflow.actions.map((a) => {
      const cfg = parseConfig(a.config);
      return {
        actionType: a.actionType,
        title: (cfg.title ?? cfg.subject ?? "") as string,
        description: (cfg.description ?? "") as string,
        priority: (cfg.priority as (typeof TASK_PRIORITIES)[number] | undefined) ?? undefined,
        dueInDays: cfg.dueInDays != null ? String(cfg.dueInDays) : "",
        activityType:
          (cfg.type as (typeof ACTIVITY_TYPES)[number] | undefined) ?? "OTHER",
      };
    }),
  };
}

export function useWorkflowPermissions() {
  const { can } = useAuthorization();
  return {
    canCreate: can("workflow:create"),
    canUpdate: can("workflow:update"),
    canDelete: can("workflow:delete"),
    canRead: can("workflow:read"),
  };
}