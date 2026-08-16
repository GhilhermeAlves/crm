import { useQuery } from "@tanstack/react-query";
import { DashboardService } from "../services/dashboard.service";
import { useAuthorization } from "@/features/auth/hooks/useAuthorization";

export function useOperationalDashboard(companyId: string | null) {
  const { can } = useAuthorization();
  const canView = can("dashboard:operational");
  return useQuery({
    queryKey: ["operational-dashboard", companyId],
    queryFn: () => DashboardService.operational(companyId as string),
    enabled: !!companyId && canView,
  });
}
