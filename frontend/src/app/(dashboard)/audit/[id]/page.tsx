"use client";

import { useParams, useRouter } from "next/navigation";
import { useAuditLog } from "@/features/audit/hooks/useAudit";
import { AuditDetailCard } from "@/features/audit/components/AuditDetailCard";
import { ArrowLeft } from "lucide-react";
import { Button } from "@/components/ui/button";

export default function AuditDetailPage() {
  const params = useParams();
  const id = params.id as string;
  const { data: log, isLoading, error } = useAuditLog(id);

  if (isLoading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <p className="text-muted-foreground">Carregando detalhes do log...</p>
      </div>
    );
  }

  if (error || !log) {
    return (
      <div className="space-y-4">
        <Button variant="ghost" size="sm" onClick={() => window.history.back()}>
          <ArrowLeft className="mr-1 h-4 w-4" />
          Voltar
        </Button>
        <div className="flex h-64 items-center justify-center">
          <p className="text-muted-foreground">Log de auditoria não encontrado.</p>
        </div>
      </div>
    );
  }

  return <AuditDetailCard log={log} />;
}
