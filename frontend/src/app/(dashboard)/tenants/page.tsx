"use client";

import { useState, useCallback, useMemo } from "react";
import Link from "next/link";
import { Plus, Building2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { PageTitle } from "@/components/common/PageTitle";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorCard } from "@/components/common/ErrorCard";
import { SkeletonTable } from "@/components/feedback/SkeletonTable";
import { ROUTES } from "@/lib/constants";
import { useTenants } from "@/features/tenants/hooks/useTenants";
import type { Tenant } from "@/features/tenants/types/tenant.types";
import { TenantTable } from "@/features/tenants/components/TenantTable";
import { TenantFilters } from "@/features/tenants/components/TenantFilters";
import { DeleteTenantDialog } from "@/features/tenants/components/DeleteTenantDialog";

export default function TenantsPage() {
  const [search, setSearch] = useState("");
  const [deleteTarget, setDeleteTarget] = useState<Tenant | null>(null);

  const { data: tenants, isLoading, error, refetch } = useTenants();

  const filteredTenants = useMemo(() => {
    if (!tenants) return [];
    if (!search) return tenants;
    const term = search.toLowerCase();
    return tenants.filter(
      (t) =>
        t.tradingName.toLowerCase().includes(term) ||
        t.legalName.toLowerCase().includes(term) ||
        t.cnpj.includes(term) ||
        t.email.toLowerCase().includes(term),
    );
  }, [tenants, search]);

  const handleSearchChange = useCallback((value: string) => {
    setSearch(value);
  }, []);

  if (error) {
    return (
      <div className="space-y-6">
        <PageTitle>Empresas</PageTitle>
        <ErrorCard
          message="Não foi possível carregar as empresas. Verifique se o backend está disponível."
          onRetry={() => refetch()}
        />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <PageTitle>Empresas</PageTitle>
        <Button asChild>
          <Link href={ROUTES.TENANTS_NEW}>
            <Plus className="mr-1 h-4 w-4" />
            Nova Empresa
          </Link>
        </Button>
      </div>

      <TenantFilters
        search={search}
        onSearchChange={handleSearchChange}
        onRefresh={() => refetch()}
      />

      {isLoading ? (
        <SkeletonTable rows={5} columns={6} />
      ) : filteredTenants.length === 0 ? (
        <EmptyState
          icon={<Building2 className="h-8 w-8 text-muted-foreground" />}
          title={
            search ? "Nenhuma empresa encontrada" : "Nenhuma empresa cadastrada"
          }
          description={
            search
              ? "Tente ajustar sua pesquisa."
              : "Comece cadastrando a primeira empresa do sistema."
          }
          action={
            !search ? (
              <Button asChild>
                <Link href={ROUTES.TENANTS_NEW}>
                  <Plus className="mr-1 h-4 w-4" />
                  Nova Empresa
                </Link>
              </Button>
            ) : undefined
          }
        />
      ) : (
        <TenantTable tenants={filteredTenants} onDelete={setDeleteTarget} />
      )}

      {deleteTarget && (
        <DeleteTenantDialog
          open={!!deleteTarget}
          onOpenChange={(open: boolean) => {
            if (!open) setDeleteTarget(null);
          }}
          tenantId={deleteTarget.id}
          tenantName={deleteTarget.tradingName}
        />
      )}
    </div>
  );
}
