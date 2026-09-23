import type { Activity } from "@/features/activities/types/activity.types";
import type { Task } from "@/features/tasks/types/task.types";

export type AttentionOpportunity = {
  id: string;
  title: string;
  value: number;
  contactName: string | null;
  stageName: string;
  stageOrder: number;
  pipelineName: string;
  stale: boolean;
  daysInactive: number;
  suggestion: string;
  priorityScore: number;
};

export type OperationalDashboard = {
  greeting: string;
  opportunitiesNeedingAttention: number;
  staleOpportunities: number;
  tasksDueToday: number;
  openOpportunities: number;
  openValue: number;
  attentionOpportunities: AttentionOpportunity[];
  dueToday: Task[];
  recentActivities: Activity[];
};
