import api from "@/lib/api";
import type { AuditLog, AuditLogPageResponse, AuditLogSearchParams } from "../types/audit.types";

const AUDIT_PATH = "/audit";

function buildQueryString(params: AuditLogSearchParams): string {
  const searchParams = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      searchParams.append(key, String(value));
    }
  });
  const str = searchParams.toString();
  return str ? `?${str}` : "";
}

export const AuditService = {
  async search(params: AuditLogSearchParams): Promise<AuditLogPageResponse> {
    const queryString = buildQueryString(params);
    const response = await api.get<AuditLogPageResponse>(`${AUDIT_PATH}${queryString}`);
    return response.data;
  },

  async getById(id: string): Promise<AuditLog> {
    const response = await api.get<AuditLog>(`${AUDIT_PATH}/${id}`);
    return response.data;
  },
};
