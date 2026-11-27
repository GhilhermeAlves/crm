export type ReportExportType =
  "MONTHLY_DRE" | "CASH_FLOW" | "TRANSACTIONS" | "AGENDA" | "BUDGETS" | "LEADS";
export type ExportFormat = "PDF" | "XLSX" | "CSV";

export const FORMAT_LABELS: Record<ExportFormat, string> = {
  PDF: "PDF",
  XLSX: "Excel",
  CSV: "CSV",
};
export type ReportExportStatus = "PENDING" | "READY" | "FAILED" | "EXPIRED" | "CANCELED";

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
  /** Linhas de dados do arquivo; preenchido quando READY. */
  rowCount?: number | null;
}

/** Item do catálogo vindo do backend (`GET /report-exports/catalog`), fonte única dos relatórios. */
export interface ReportCatalogEntry {
  type: ReportExportType;
  /** Segmento da URL: /reports/export/<slug>. */
  slug: string;
  label: string;
  description: string;
  domain: string;
  permission: string;
  formats: ExportFormat[];
  periodMode: "MONTH" | "RANGE";
  /** Janela máxima do período, em meses. */
  maxMonths: number;
}

export interface ReportExportDefinition {
  type: ReportExportType;
  slug: string;
  label: string;
  description: string;
  permission: string;
  formats: ExportFormat[];
  /** DRE é gerada por mês fechado; os demais por intervalo de datas. */
  period: "month" | "range";
  maxMonths: number;
}

export function toReportExportDefinition(entry: ReportCatalogEntry): ReportExportDefinition {
  return {
    type: entry.type,
    slug: entry.slug,
    label: entry.label,
    description: entry.description,
    permission: entry.permission,
    formats: entry.formats,
    period: entry.periodMode === "MONTH" ? "month" : "range",
    maxMonths: entry.maxMonths,
  };
}

/** Procura no catálogo (já filtrado por permissão) o relatório do slug da URL. */
export function findReportExport(
  catalog: ReportCatalogEntry[] | undefined,
  slug: string,
): ReportExportDefinition | undefined {
  const entry = catalog?.find((item) => item.slug === slug);
  return entry ? toReportExportDefinition(entry) : undefined;
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
