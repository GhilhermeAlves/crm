"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { ClipboardList } from "lucide-react";
import { AuditTable } from "@/features/audit/components/AuditTable";
import { AuditFilters } from "@/features/audit/components/AuditFilters";
import { useAuditLogs } from "@/features/audit/hooks/useAudit";
import type {
  AuditLog,
  AuditLogSearchParams,
} from "@/features/audit/types/audit.types";

export default function AuditPage() {
  const router = useRouter();
  const [params, setParams] = useState<AuditLogSearchParams>({
    page: 1,
    pageSize: 20,
  });

  const { data, isLoading } = useAuditLogs(params);

  const handleRowClick = (log: AuditLog) => {
    router.push(`/audit/${log.id}`);
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold flex items-center gap-2">
          <ClipboardList className="h-6 w-6" />
          Auditoria
        </h1>
        <p className="text-muted-foreground">
          Histórico de ações e eventos do sistema
        </p>
      </div>

      <AuditFilters params={params} onParamsChange={setParams} />

      <AuditTable
        data={data}
        isLoading={isLoading}
        onPageChange={(page) => setParams({ ...params, page })}
        onRowClick={handleRowClick}
      />
    </div>
  );
}
