export type OpportunityStatus = "OPEN" | "WON" | "LOST";

export type Pipeline = {
  id: string;
  companyId: string;
  name: string;
  description: string | null;
  active: boolean;
  stages: Stage[];
  createdAt: string;
  updatedAt: string;
};

export type Stage = {
  id: string;
  pipelineId: string;
  name: string;
  color: string | null;
  order: number;
  probability: number;
  createdAt: string;
  updatedAt: string | null;
};

export type Opportunity = {
  id: string;
  companyId: string;
  title: string;
  value: number;
  contactId: string;
  pipelineId: string;
  stageId: string;
  stageName: string | null;
  probability: number;
  assignedTo: string | null;
  expectedCloseDate: string | null;
  status: OpportunityStatus;
  wonAt: string | null;
  lostAt: string | null;
  lossReason: string | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
};

export type OpportunityHistory = {
  id: string;
  opportunityId: string;
  fromStageId: string | null;
  toStageId: string;
  changedBy: string | null;
  changedAt: string;
  note: string | null;
};

export type StageMetric = {
  stageId: string;
  stageName: string;
  count: number;
  value: number;
};

export type PipelineMetrics = {
  pipelineId: string;
  openCount: number;
  wonCount: number;
  lostCount: number;
  totalValue: number;
  wonValue: number;
  lostValue: number;
  winRate: number;
  averageCycleDays: number | null;
  forecast: number;
  byStage: StageMetric[];
};

export type CreatePipelineRequest = {
  name: string;
  description?: string;
};

export type CreateOpportunityRequest = {
  title: string;
  value: number;
  contactId: string;
  assignedTo?: string;
  expectedCloseDate?: string;
  notes?: string;
};

export type UpdateOpportunityRequest = {
  title?: string;
  value?: number;
  assignedTo?: string;
  expectedCloseDate?: string;
  notes?: string;
};

export type MoveDirection = "ADVANCE" | "REGRESS";
