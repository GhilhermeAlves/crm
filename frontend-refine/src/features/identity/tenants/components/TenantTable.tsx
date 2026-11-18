"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { Building2, Eye, History, Pencil, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { DataTable } from "@/components/ui/data-table";
import { ROUTES } from "@/lib/constants";
import { useAuthorization } from "@/features/identity/auth/hooks/useAuthorization";
import type { Tenant } from "../types/tenant.types";
import { TenantStatusBadge } from "./TenantStatusBadge";
import { TenantPlanBadge } from "./TenantPlanBadge";
import { TenantAuditDialog } from "./TenantAuditDialog";
import { useBillingStatuses } from "@/features/billing/hooks";
import { BillingStatusBadge } from "@/features/billing/components/BillingStatusBadge";

type TenantTableProps = {
  tenants: Tenant[];
  onDelete: (tenant: Tenant) => void;
};

export function TenantTable({ tenants, onDelete }: TenantTableProps) {
  const { can, isPlatformAdmin } = useAuthorization();
  const statuses = useBillingStatuses(isPlatformAdmin);
  const billingByCompany = useMemo(
    () => new Map((statuses.data ?? []).map((s) => [s.companyId, s.status])),
    [statuses.data],
  );
  const [auditTenant, setAuditTenant] = useState<Tenant | null>(null);

  const formatDate = useMemo(
    () => (dateStr: string) => {
      return new Date(dateStr).toLocaleDateString("pt-BR", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
      });
    },
    [],
  );

  return (
    <>
      <DataTable
        tableId="tenants"
        data={tenants ?? []}
        columns={[
          {
            id: "empresa",
            header: "Empresa",
            headerClassName: "w-[300px]",
            cell: (tenant) => (
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-muted">
                  {tenant.logoUrl ? (
                    <img
                      src={tenant.logoUrl}
                      alt={tenant.tradingName}
                      className="h-10 w-10 rounded-lg object-cover"
                    />
                  ) : (
                    <Building2 className="h-5 w-5 text-muted-foreground" />
                  )}
                </div>
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium">{tenant.tradingName}</p>
                  <p className="truncate text-xs text-muted-foreground">{tenant.cnpj}</p>
                </div>
              </div>
            ),
          },
          {
            id: "plano",
            header: "Plano",
            cell: (tenant) => <TenantPlanBadge plan={tenant.plan} />,
          },
          {
            id: "status",
            header: "Status",
            cell: (tenant) => <TenantStatusBadge status={tenant.status} />,
          },
          {
            id: "cobranca",
            header: "Cobrança",
            cell: (tenant) => (
              <>
                {billingByCompany.get(tenant.id) ? (
                  <BillingStatusBadge status={billingByCompany.get(tenant.id)!} />
                ) : (
                  "—"
                )}
              </>
            ),
          },
          {
            id: "usuarios",
            header: "Usuários",
            headerClassName: "text-center",
            className: "text-center",
            cell: (tenant) => <span className="text-sm">{tenant.maxUsers}</span>,
          },
          {
            id: "contatos",
            header: "Contatos",
            headerClassName: "text-center",
            className: "text-center",
            cell: (tenant) => <span className="text-sm">{tenant.maxContacts}</span>,
          },
          {
            id: "criado-em",
            header: "Criado em",
            cell: (tenant) => (
              <span className="text-sm text-muted-foreground">{formatDate(tenant.createdAt)}</span>
            ),
          },
          {
            id: "acoes",
            header: "Ações",
            hideable: false,
            headerClassName: "text-right",
            cell: (tenant) => (
              <div className="flex justify-end gap-1">
                <Button variant="ghost" size="icon" className="h-8 w-8" asChild>
                  <Link href={`${ROUTES.TENANTS}/${tenant.id}`}>
                    <Eye className="h-4 w-4" />
                  </Link>
                </Button>
                <Button variant="ghost" size="icon" className="h-8 w-8" asChild>
                  <Link href={`${ROUTES.TENANTS}/${tenant.id}/edit`}>
                    <Pencil className="h-4 w-4" />
                  </Link>
                </Button>
                {can("audit:read") && (
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-8 w-8"
                    title="Ver auditoria"
                    onClick={() => setAuditTenant(tenant)}
                  >
                    <History className="h-4 w-4" />
                  </Button>
                )}
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-8 w-8 text-destructive hover:text-destructive"
                  onClick={() => onDelete(tenant)}
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>
            ),
          },
        ]}
        keyExtractor={(tenant) => tenant.id}
      />

      <TenantAuditDialog
        tenant={auditTenant}
        open={!!auditTenant}
        onOpenChange={(open) => {
          if (!open) setAuditTenant(null);
        }}
      />
    </>
  );
}
