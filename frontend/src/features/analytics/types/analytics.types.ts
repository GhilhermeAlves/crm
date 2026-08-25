export type AnalyticsMetrics = {
  contactsCreated: number;
  leadsCreated: number;
  leadsConverted: number;
  opportunitiesCreated: number;
  opportunitiesWon: number;
  wonValue: number;
  pipelineOpenValue: number;
  activitiesCreated: number;
  tasksCreated: number;
  tasksCompleted: number;
  tasksOverdue: number;
  campaignsExecuted: number;
  campaignMessagesSent: number;
  campaignMessagesFailed: number;
  omnichannelMessagesIn: number;
  omnichannelMessagesOut: number;
  workflowRunsMatched: number;
  workflowRunsSuccess: number;
  workflowRunsFailed: number;
};

export type AnalyticsDailyPoint = {
  date: string;
  leads: number;
  opportunities: number;
  messagesSent: number;
};

export type AnalyticsSummary = {
  from: string;
  to: string;
  current: AnalyticsMetrics;
  previous: AnalyticsMetrics;
  series: AnalyticsDailyPoint[];
};

export const PERIOD_OPTIONS = [
  { value: "7d", label: "Últimos 7 dias", days: 7 },
  { value: "30d", label: "Últimos 30 dias", days: 30 },
  { value: "month", label: "Mês atual" },
  { value: "prevMonth", label: "Mês anterior" },
] as const;

export type PeriodOption = (typeof PERIOD_OPTIONS)[number]["value"];

export function resolvePeriodRange(period: PeriodOption): { from: string; to: string } {
  const now = new Date();
  const iso = (d: Date) => d.toISOString().slice(0, 10);
  if (period === "7d") {
    const from = new Date(now);
    from.setDate(from.getDate() - 6);
    return { from: iso(from), to: iso(now) };
  }
  if (period === "30d") {
    const from = new Date(now);
    from.setDate(from.getDate() - 29);
    return { from: iso(from), to: iso(now) };
  }
  if (period === "prevMonth") {
    const first = new Date(now.getFullYear(), now.getMonth() - 1, 1);
    const last = new Date(now.getFullYear(), now.getMonth(), 0);
    return { from: iso(first), to: iso(last) };
  }
  const first = new Date(now.getFullYear(), now.getMonth(), 1);
  return { from: iso(first), to: iso(now) };
}

/** Variação percentual com proteção contra divisão por zero. */
export function delta(
  current: number,
  previous: number,
): {
  abs: number;
  pct: number | null;
} {
  const abs = current - previous;
  if (previous === 0) {
    return { abs, pct: null };
  }
  return { abs, pct: Math.round((abs / previous) * 1000) / 10 };
}
