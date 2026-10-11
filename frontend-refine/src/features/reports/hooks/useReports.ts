import { useMutation } from "@tanstack/react-query";

import { ReportsService } from "../services/reports.service";
import type { CreateReportSuggestionRequest } from "../types/reports.types";

export function useSubmitReportSuggestion(companyId: string | null) {
  return useMutation({
    mutationFn: (body: CreateReportSuggestionRequest) =>
      ReportsService.submitReportSuggestion(companyId as string, body),
  });
}
