"use client";

import { useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useAuthorization } from "@/features/auth/hooks/useAuthorization";
import { useLeads, useDeleteLead } from "@/features/leads/hooks/useLeads";
import { LeadTable } from "@/features/leads/components/LeadTable";
import { LeadFilters } from "@/features/leads/components/LeadFilters";
import { DeleteLeadDialog } from "@/features/leads/components/DeleteLeadDialog";
import { PageTitle } from "@/components/common/PageTitle";
import { Button } from "@/components/ui/button";
import { Plus } from "lucide-react";
import type { Lead, LeadSource, LeadStatus, LeadClassification } from "@/features/leads/types/lead.types";
import { ROUTES } from "@/lib/constants";

export default function LeadsPage() {
  const router = useRouter();
  const { user } = useAuth();
  const { can } = useAuthorization();
  const companyId = user?.companyId ?? null;

  const [status, setStatus] = useState("all");
  const [source, setSource] = useState("all");
  const [classification, setClassification] = useState("all");
  const [page, setPage] = useState(0);
  const [deleteLead, setDeleteLead] = useState<Lead | null>(null);

  const { data, isLoading, refetch } = useLeads(companyId, {
    page,
    pageSize: 10,
    status: status !== "all" ? (status as LeadStatus) : undefined,
    source: source !== "all" ? (source as LeadSource) : undefined,
    classification: classification !== "all" ? (classification as LeadClassification) : undefined,
    sortBy: "createdAt",
    sortDirection: "desc",
  });

  const deleteLeadMutation = useDeleteLead(companyId);
  const canCreate = can("lead:create");
  const canDelete = can("lead:delete");

  const handleDelete = useCallback(() => {
    if (deleteLead) {
      deleteLeadMutation.mutate(deleteLead.id, {
        onSuccess: () => setDeleteLead(null),
      });
    }
  }, [deleteLead, deleteLeadMutation]);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <PageTitle>Leads</PageTitle>
        {canCreate && (
          <Button onClick={() => router.push(ROUTES.LEADS_NEW)}>
            <Plus className="mr-2 h-4 w-4" />
            Novo Lead
          </Button>
        )}
      </div>

      <LeadFilters
        status={status}
        source={source}
        classification={classification}
        onStatusChange={(val) => {
          setStatus(val);
          setPage(0);
        }}
        onSourceChange={(val) => {
          setSource(val);
          setPage(0);
        }}
        onClassificationChange={(val) => {
          setClassification(val);
          setPage(0);
        }}
        onRefresh={() => refetch()}
      />

      <LeadTable
        leads={data?.content || []}
        isLoading={isLoading}
        onDelete={canDelete ? setDeleteLead : undefined}
      />

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-muted-foreground">
            Mostrando {data.content.length} de {data.totalElements} leads
          </p>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
            >
              Anterior
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setPage((p) => Math.min(data.totalPages - 1, p + 1))}
              disabled={page >= data.totalPages - 1}
            >
              Próximo
            </Button>
          </div>
        </div>
      )}

      <DeleteLeadDialog
        lead={deleteLead}
        open={!!deleteLead}
        onOpenChange={(open) => !open && setDeleteLead(null)}
        onConfirm={handleDelete}
        isLoading={deleteLeadMutation.isPending}
      />
    </div>
  );
}