import { useQuery } from "@tanstack/react-query";
import { DashboardService } from "../services/dashboard.service";

export function useDashboardKpis() {
  return useQuery({
    queryKey: ["dashboard", "kpis"],
    queryFn: () => DashboardService.getKpis(),
  });
}

export function useRecentActivities() {
  return useQuery({
    queryKey: ["dashboard", "recent-activities"],
    queryFn: () => DashboardService.getRecentActivities(),
  });
}
