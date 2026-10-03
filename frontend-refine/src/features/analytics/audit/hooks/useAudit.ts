import { useQuery } from "@tanstack/react-query";
import { AuditService } from "../services/audit.service";
import type { AuditLogSearchParams } from "../types/audit.types";

export function useAuditLogs(params: AuditLogSearchParams = {}) {
  return useQuery({
    queryKey: ["audit-logs", params],
    queryFn: () => AuditService.search(params),
  });
}

export function useAuditLog(id: string) {
  return useQuery({
    queryKey: ["audit-logs", id],
    queryFn: () => AuditService.getById(id),
    enabled: !!id,
  });
}
