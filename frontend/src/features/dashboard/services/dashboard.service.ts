import api from "@/lib/api";
import type { DashboardKpis, RecentActivity } from "../types/dashboard.types";

const DASHBOARD_PATH = "/dashboard";

export const DashboardService = {
  async getKpis(): Promise<DashboardKpis> {
    const response = await api.get<DashboardKpis>(`${DASHBOARD_PATH}/kpis`);
    return response.data;
  },

  async getRecentActivities(): Promise<RecentActivity[]> {
    const response = await api.get<RecentActivity[]>(`${DASHBOARD_PATH}/recent-activities`);
    return response.data;
  },
};
