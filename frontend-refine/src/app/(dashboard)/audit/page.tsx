"use client";

import { useState } from "react";
import { DEFAULT_PAGE_SIZE } from "@/types/page";
import { useRouter } from "next/navigation";

import { AuditTable } from "@/features/analytics/audit/components/AuditTable";
import { AuditFilters } from "@/features/analytics/audit/components/AuditFilters";
import { useAuditLogs } from "@/features/analytics/audit/hooks/useAudit";
import type { AuditLog, AuditLogSearchParams } from "@/features/analytics/audit/types/audit.types";

export default function AuditPage() {
  const router = useRouter();
  const [params, setParams] = useState<AuditLogSearchParams>({
    page: 1,
    pageSize: DEFAULT_PAGE_SIZE,
  });

  const { data, isLoading } = useAuditLogs(params);

  const handleRowClick = (log: AuditLog) => {
    router.push(`/audit/${log.id}`);
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Auditoria</h1>
        <p className="text-muted-foreground">Histórico de ações e eventos do sistema</p>
      </div>

      <AuditFilters params={params} onParamsChange={setParams} />

      <AuditTable
        data={data}
        isLoading={isLoading}
        pageSize={params.pageSize ?? DEFAULT_PAGE_SIZE}
        onPageChange={(page) => setParams({ ...params, page })}
        onPageSizeChange={(pageSize) => setParams({ ...params, pageSize, page: 1 })}
        onRowClick={handleRowClick}
      />
    </div>
  );
}
