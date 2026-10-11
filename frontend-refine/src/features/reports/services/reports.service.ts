import api from "@/lib/api";

import type {
  CreateReportSuggestionRequest,
  ReportSuggestionResponse,
} from "../types/reports.types";

export const ReportsService = {
  async submitReportSuggestion(
    companyId: string,
    body: CreateReportSuggestionRequest,
  ): Promise<ReportSuggestionResponse> {
    const response = await api.post<ReportSuggestionResponse>(
      `/companies/${companyId}/report-suggestions`,
      body,
    );
    return response.data;
  },
};
