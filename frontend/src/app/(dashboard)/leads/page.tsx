"use client";

import { useState, useCallback, useMemo } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useAuthorization } from "@/features/auth/hooks/useAuthorization";
import { useLeads, useDeleteLead, useUpdateLead } from "@/features/leads/hooks/useLeads";
import { useContacts } from "@/features/contacts/hooks/useContacts";
import { LeadTable } from "@/features/leads/components/LeadTable";
import { LeadFilters } from "@/features/leads/components/LeadFilters";
import { DeleteLeadDialog } from "@/features/leads/components/DeleteLeadDialog";
import { ConvertLeadDialog } from "@/features/leads/components/ConvertLeadDialog";
import { PageTitle } from "@/components/common/PageTitle";
import { SearchInput } from "@/components/common/SearchInput";
import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/common/EmptyState";
import { Card, CardContent } from "@/components/ui/card";
import { Plus, SearchX } from "lucide-react";
import type {
  Lead,
  LeadSource,
  LeadStatus,
  LeadClassification,
} from "@/features/leads/types/lead.types";
import { ROUTES } from "@/lib/constants";

export default function LeadsPage() {
  const router = useRouter();
  const { user } = useAuth();
  const { can } = useAuthorization();
  const companyId = user?.companyId ?? null;

  const [status, setStatus] = useState("all");
  const [source, setSource] = useState("all");
  const [classification, setClassification] = useState("all");
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [deleteLead, setDeleteLead] = useState<Lead | null>(null);
  const [convertLead, setConvertLead] = useState<Lead | null>(null);

  const { data, isLoading, refetch } = useLeads(companyId, {
    page,
    pageSize: 10,
    status: status !== "all" ? (status as LeadStatus) : undefined,
    source: source !== "all" ? (source as LeadSource) : undefined,
    classification: classification !== "all" ? (classification as LeadClassification) : undefined,
    sortBy: "createdAt",
    sortDirection: "desc",
  });

  const { data: contactsData } = useContacts(companyId);

  const contactsMap = useMemo(() => {
    const map: Record<
      string,
      { firstName: string; lastName?: string; email: string | null; phone: string | null }
    > = {};
    (contactsData ?? []).forEach((c) => {
      map[c.id] = { firstName: c.firstName, lastName: c.lastName, email: c.email, phone: c.phone };
    });
    return map;
  }, [contactsData]);

  const deleteLeadMutation = useDeleteLead(companyId);
  const updateLeadMutation = useUpdateLead(companyId);
  const canCreate = can("lead:create");
  const canDelete = can("lead:delete");
  const canUpdate = can("lead:update");

  const rawLeads = data?.content ?? [];

  const filteredLeads = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return rawLeads;
    return rawLeads.filter((lead) => {
      const contact = lead.contactId ? contactsMap[lead.contactId] : undefined;
      const name = contact
        ? `${contact.firstName}${contact.lastName ? ` ${contact.lastName}` : ""}`
        : "";
      return (
        name.toLowerCase().includes(q) ||
        (contact?.email ?? "").toLowerCase().includes(q) ||
        (contact?.phone ?? "").toLowerCase().includes(q)
      );
    });
  }, [rawLeads, search, contactsMap]);

  const hasActiveFilters =
    status !== "all" || source !== "all" || classification !== "all" || search.trim() !== "";

  const clearFilters = useCallback(() => {
    setStatus("all");
    setSource("all");
    setClassification("all");
    setSearch("");
    setPage(0);
  }, []);

  const handleDelete = useCallback(() => {
    if (deleteLead) {
      deleteLeadMutation.mutate(deleteLead.id, {
        onSuccess: () => setDeleteLead(null),
      });
    }
  }, [deleteLead, deleteLeadMutation]);

  const handleConvert = useCallback(() => {
    if (!convertLead) return;
    updateLeadMutation.mutate(
      { id: convertLead.id, data: { status: "CONVERTED" } },
      {
        onSuccess: () => setConvertLead(null),
      },
    );
  }, [convertLead, updateLeadMutation]);

  const convertPending =
    updateLeadMutation.isPending && updateLeadMutation.variables?.id === convertLead?.id;

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <PageTitle>Leads</PageTitle>
          <p className="text-sm text-muted-foreground">
            Gerencie seus leads, acompanhe oportunidades de conversão e mantenha o relacionamento
            com potenciais clientes.
          </p>
        </div>
        {canCreate && (
          <Button onClick={() => router.push(ROUTES.LEADS_NEW)}>
            <Plus className="mr-2 h-4 w-4" />
            Novo Lead
          </Button>
        )}
      </div>

      <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <div className="w-full max-w-sm">
          <SearchInput
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
            placeholder="Pesquisar por nome, e-mail ou telefone..."
          />
        </div>
        <div className="flex flex-wrap items-center gap-2">
          {hasActiveFilters && (
            <Button variant="ghost" size="sm" onClick={clearFilters}>
              <SearchX className="mr-1 h-4 w-4" />
              Limpar filtros
            </Button>
          )}
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
        </div>
      </div>

      {isLoading ? (
        <LeadTable leads={[]} isLoading />
      ) : filteredLeads.length === 0 ? (
        hasActiveFilters ? (
          <Card>
            <CardContent>
              <EmptyState
                icon={<SearchX className="h-8 w-8" />}
                title="Nenhum resultado"
                description="Não encontramos leads para a pesquisa ou filtros aplicados."
                action={
                  <Button variant="outline" size="sm" onClick={clearFilters}>
                    Limpar filtros
                  </Button>
                }
              />
            </CardContent>
          </Card>
        ) : (
          <LeadTable leads={[]} />
        )
      ) : (
        <LeadTable
          leads={filteredLeads}
          contacts={contactsMap}
          onDelete={canDelete ? setDeleteLead : undefined}
          onConvert={canUpdate ? setConvertLead : undefined}
          canConvert={canUpdate}
        />
      )}

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-muted-foreground">
            Mostrando {filteredLeads.length} de {data.totalElements} leads
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

      <ConvertLeadDialog
        lead={convertLead}
        open={!!convertLead}
        onOpenChange={(open) => !open && setConvertLead(null)}
        onConfirm={handleConvert}
        isLoading={convertPending}
      />
    </div>
  );
}
