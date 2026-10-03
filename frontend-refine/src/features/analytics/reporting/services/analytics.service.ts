import api from "@/lib/api";
import type { AnalyticsSummary } from "../types/analytics.types";

const BASE = "/companies";

export const AnalyticsService = {
  async summary(
    companyId: string,
    params: { from?: string; to?: string },
  ): Promise<AnalyticsSummary> {
    const response = await api.get<AnalyticsSummary>(`${BASE}/${companyId}/analytics/summary`, {
      params,
    });
    return response.data;
  },
};
