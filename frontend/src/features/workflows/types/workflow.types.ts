export type WorkflowTrigger =
  | "OPPORTUNITY_CREATED"
  | "OPPORTUNITY_STAGE_CHANGED"
  | "OPPORTUNITY_WON"
  | "OPPORTUNITY_LOST"
  | "TASK_CREATED"
  | "TASK_COMPLETED"
  | "ACTIVITY_CREATED";

export type ConditionOperator =
  "EQUALS" | "NOT_EQUALS" | "GREATER_THAN" | "LESS_THAN" | "GREATER_OR_EQUAL" | "LESS_OR_EQUAL";

export type WorkflowActionType = "CREATE_TASK" | "CREATE_ACTIVITY";

export type WorkflowExecutionStatus = "PROCESSING" | "SUCCESS" | "FAILED" | "SKIPPED";

export type WorkflowCondition = {
  id: string | null;
  field: string;
  operator: ConditionOperator;
  value: string;
  sortOrder: number;
};

export type WorkflowAction = {
  id: string | null;
  actionType: WorkflowActionType;
  sortOrder: number;
  config: string;
};

export type Workflow = {
  id: string;
  companyId: string;
  name: string;
  description: string | null;
  trigger: WorkflowTrigger;
  active: boolean;
  conditions: WorkflowCondition[];
  actions: WorkflowAction[];
  createdAt: string;
  updatedAt: string;
};

export type WorkflowExecution = {
  id: string;
  workflowId: string;
  actionType: WorkflowActionType;
  eventType: string;
  entityId: string;
  status: WorkflowExecutionStatus;
  resultText: string | null;
  errorMessage: string | null;
  createdAt: string;
};

export type WorkflowConditionInput = {
  id?: string | null;
  field: string;
  operator: ConditionOperator;
  value: string;
  sortOrder: number;
};

export type WorkflowActionInput = {
  id?: string | null;
  actionType: WorkflowActionType;
  sortOrder: number;
  config: Record<string, unknown>;
};

export type CreateWorkflowRequest = {
  name: string;
  description?: string;
  trigger: WorkflowTrigger;
  conditions: WorkflowConditionInput[];
  actions: WorkflowActionInput[];
};

export type UpdateWorkflowRequest = CreateWorkflowRequest;

export const WORKFLOW_TRIGGER_LABELS: Record<WorkflowTrigger, string> = {
  OPPORTUNITY_CREATED: "Oportunidade criada",
  OPPORTUNITY_STAGE_CHANGED: "Etapa da oportunidade alterada",
  OPPORTUNITY_WON: "Oportunidade ganha",
  OPPORTUNITY_LOST: "Oportunidade perdida",
  TASK_CREATED: "Tarefa criada",
  TASK_COMPLETED: "Tarefa concluída",
  ACTIVITY_CREATED: "Atividade criada",
};

export const CONDITION_OPERATOR_LABELS: Record<ConditionOperator, string> = {
  EQUALS: "Igual a",
  NOT_EQUALS: "Diferente de",
  GREATER_THAN: "Maior que",
  LESS_THAN: "Menor que",
  GREATER_OR_EQUAL: "Maior ou igual",
  LESS_OR_EQUAL: "Menor ou igual",
};

export const WORKFLOW_ACTION_LABELS: Record<WorkflowActionType, string> = {
  CREATE_TASK: "Criar tarefa",
  CREATE_ACTIVITY: "Criar atividade",
};

export const WORKFLOW_EXECUTION_STATUS_LABELS: Record<WorkflowExecutionStatus, string> = {
  PROCESSING: "Processando",
  SUCCESS: "Sucesso",
  FAILED: "Falhou",
  SKIPPED: "Ignorada",
};

export type ConditionFieldOption = { value: string; label: string };

export const CONDITION_FIELDS: Record<WorkflowTrigger, ConditionFieldOption[]> = {
  OPPORTUNITY_CREATED: [
    { value: "opportunity.stage", label: "Etapa da oportunidade" },
    { value: "opportunity.value", label: "Valor da oportunidade" },
  ],
  OPPORTUNITY_STAGE_CHANGED: [
    { value: "opportunity.stage", label: "Etapa da oportunidade" },
    { value: "opportunity.value", label: "Valor da oportunidade" },
  ],
  OPPORTUNITY_WON: [
    { value: "opportunity.stage", label: "Etapa da oportunidade" },
    { value: "opportunity.value", label: "Valor da oportunidade" },
  ],
  OPPORTUNITY_LOST: [
    { value: "opportunity.stage", label: "Etapa da oportunidade" },
    { value: "opportunity.value", label: "Valor da oportunidade" },
  ],
  TASK_CREATED: [{ value: "task.priority", label: "Prioridade da tarefa" }],
  TASK_COMPLETED: [{ value: "task.priority", label: "Prioridade da tarefa" }],
  ACTIVITY_CREATED: [{ value: "activity.type", label: "Tipo da atividade" }],
};
