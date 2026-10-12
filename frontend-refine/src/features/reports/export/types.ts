export type ReportExportType = "MONTHLY_DRE" | "CASH_FLOW" | "TRANSACTIONS" | "AGENDA";
export type ExportFormat = "PDF" | "XLSX";
export type ReportExportStatus = "PENDING" | "READY" | "FAILED";

export interface ReportExportRequest {
  type: ReportExportType;
  format: ExportFormat;
  /** DRE: mês `yyyy-MM`. */
  month?: string;
  /** Demais relatórios: `yyyy-MM-dd`. */
  from?: string;
  to?: string;
}

export interface ReportExport {
  id: string;
  type: ReportExportType;
  typeLabel: string;
  format: ExportFormat;
  periodFrom: string;
  periodTo: string;
  status: ReportExportStatus;
  fileName: string | null;
  errorMessage: string | null;
  createdAt: string;
  finishedAt: string | null;
}

export interface ReportExportDefinition {
  type: ReportExportType;
  /** Segmento da URL: /reports/export/<slug>. */
  slug: string;
  label: string;
  description: string;
  permission: string;
  /** DRE é gerada por mês fechado; os demais por intervalo de datas. */
  period: "month" | "range";
}

export const REPORT_EXPORTS: ReportExportDefinition[] = [
  {
    type: "MONTHLY_DRE",
    slug: "monthly-dre",
    label: "Contábil mensal (DRE)",
    description:
      "Receitas, deduções e despesas do mês agrupadas por conta, com o resultado do mês (regime de caixa). Ideal para enviar ao contador.",
    permission: "finance:read",
    period: "month",
  },
  {
    type: "CASH_FLOW",
    slug: "cash-flow",
    label: "Fluxo de caixa",
    description:
      "Entradas e saídas pagas dia a dia, com saldo do dia e saldo acumulado do período.",
    permission: "finance:read",
    period: "range",
  },
  {
    type: "TRANSACTIONS",
    slug: "transactions",
    label: "Extrato de lançamentos",
    description:
      "Todos os lançamentos com vencimento no período: paciente, categoria, forma de pagamento, valor e status.",
    permission: "finance:read",
    period: "range",
  },
  {
    type: "AGENDA",
    slug: "agenda",
    label: "Agenda / atendimentos",
    description:
      "Consultas do período com resumo por profissional (agendadas, realizadas, faltas e canceladas) e a lista detalhada.",
    permission: "appointment:read",
    period: "range",
  },
];

export function findReportExport(slug: string): ReportExportDefinition | undefined {
  return REPORT_EXPORTS.find((definition) => definition.slug === slug);
}

/** Metadados gravados pelo backend na notificação de relatório pronto. */
export interface ReportExportNotificationMetadata {
  kind: "REPORT_EXPORT";
  exportId: string;
  fileName: string;
  format: ExportFormat;
}

export function parseReportExportMetadata(
  metadata: string | null | undefined,
): ReportExportNotificationMetadata | null {
  if (!metadata) return null;
  try {
    const parsed = JSON.parse(metadata) as Partial<ReportExportNotificationMetadata>;
    if (parsed.kind !== "REPORT_EXPORT" || !parsed.exportId || !parsed.fileName) return null;
    return parsed as ReportExportNotificationMetadata;
  } catch {
    return null;
  }
}
