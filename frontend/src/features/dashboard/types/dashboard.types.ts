export type DepartmentCount = {
  department: string;
  count: number;
};

export type DashboardKpis = {
  totalUsers: number;
  activeUsers: number;
  inactiveUsers: number;
  newUsersThisMonth: number;
  auditEventsThisMonth: number;
  totalAuditEvents: number;
  usersByDepartment: DepartmentCount[];
};

export type RecentActivity = {
  id: string;
  userName: string | null;
  action: string;
  module: string;
  description: string | null;
  createdAt: string;
};
