export type AuditAction =
  | "CREATE"
  | "READ"
  | "UPDATE"
  | "DELETE"
  | "LOGIN"
  | "LOGOUT"
  | "EXPORT"
  | "IMPORT"
  | "APPROVE"
  | "REJECT"
  | "ASSIGN"
  | "UNASSIGN"
  | "RESET_PASSWORD"
  | "CHANGE_PASSWORD"
  | "GENERATE_REPORT"
  | "CUSTOM";

export type AuditModule =
  | "AUTH"
  | "TENANTS"
  | "USERS"
  | "ROLES"
  | "PERMISSIONS"
  | "CUSTOMERS"
  | "CONTACTS"
  | "LEADS"
  | "PIPELINE"
  | "TASKS"
  | "CALENDAR"
  | "FINANCE"
  | "REPORTS"
  | "SETTINGS"
  | "AUDIT"
  | "SYSTEM";

export type AuditStatus = "SUCCESS" | "FAILED" | "ERROR";

export type AuditLog = {
  id: string;
  companyId: string;
  userId: string | null;
  userName: string | null;
  userEmail: string | null;
  action: AuditAction;
  module: AuditModule;
  entityName: string | null;
  entityId: string | null;
  description: string | null;
  oldValues: Record<string, unknown> | null;
  newValues: Record<string, unknown> | null;
  ipAddress: string | null;
  userAgent: string | null;
  requestMethod: string | null;
  requestUri: string | null;
  status: AuditStatus;
  success: boolean;
  createdAt: string;
};

export type AuditLogPageResponse = {
  content: AuditLog[];
  page: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
};

export type AuditLogSearchParams = {
  page?: number;
  pageSize?: number;
  module?: string;
  action?: string;
  status?: string;
  userId?: string;
  entityId?: string;
  entityName?: string;
  search?: string;
  startDate?: string;
  endDate?: string;
};
