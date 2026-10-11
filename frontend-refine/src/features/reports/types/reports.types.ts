export type ReportSuggestionCategory = "CLINICAL_RECORD" | "INVENTORY" | "FINANCE" | "OTHER";

export const REPORT_SUGGESTION_CATEGORY_LABELS: Record<ReportSuggestionCategory, string> = {
  CLINICAL_RECORD: "Ficha clínica",
  INVENTORY: "Estoque",
  FINANCE: "Financeiro",
  OTHER: "Outros",
};

export const REPORT_SUGGESTION_CATEGORIES = Object.keys(
  REPORT_SUGGESTION_CATEGORY_LABELS,
) as ReportSuggestionCategory[];

export type CreateReportSuggestionRequest = {
  categories: ReportSuggestionCategory[];
  notes?: string;
};

export type ReportSuggestionResponse = {
  id: string;
};
