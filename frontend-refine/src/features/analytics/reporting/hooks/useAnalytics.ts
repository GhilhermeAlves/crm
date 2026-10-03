import { useQuery } from "@tanstack/react-query";
import { AnalyticsService } from "../services/analytics.service";
import type { PeriodOption } from "../types/analytics.types";
import { resolvePeriodRange } from "../types/analytics.types";

export function useAnalyticsSummary(companyId: string | null, period: PeriodOption) {
  const range = resolvePeriodRange(period);
  return useQuery({
    queryKey: ["analytics", companyId, period, range.from, range.to],
    queryFn: () => AnalyticsService.summary(companyId as string, range),
    enabled: !!companyId,
  });
}
