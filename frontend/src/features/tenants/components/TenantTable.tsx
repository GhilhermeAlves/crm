"use client";

import { useMemo } from "react";
import Link from "next/link";
import { Building2, Eye, Pencil, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { ROUTES } from "@/lib/constants";
import type { Tenant } from "../types/tenant.types";
import { TenantStatusBadge } from "./TenantStatusBadge";
import { TenantPlanBadge } from "./TenantPlanBadge";

type TenantTableProps = {
  tenants: Tenant[];
  onDelete: (tenant: Tenant) => void;
};

export function TenantTable({ tenants, onDelete }: TenantTableProps) {
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
    <div className="rounded-lg border">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="w-[300px]">Empresa</TableHead>
            <TableHead>Plano</TableHead>
            <TableHead>Status</TableHead>
            <TableHead className="text-center">Usuários</TableHead>
            <TableHead className="text-center">Contatos</TableHead>
            <TableHead>Criado em</TableHead>
            <TableHead className="text-right">Ações</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {tenants.map((tenant) => (
            <TableRow key={tenant.id}>
              <TableCell>
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
              </TableCell>
              <TableCell>
                <TenantPlanBadge plan={tenant.plan} />
              </TableCell>
              <TableCell>
                <TenantStatusBadge status={tenant.status} />
              </TableCell>
              <TableCell className="text-center">
                <span className="text-sm">{tenant.maxUsers}</span>
              </TableCell>
              <TableCell className="text-center">
                <span className="text-sm">{tenant.maxContacts}</span>
              </TableCell>
              <TableCell>
                <span className="text-sm text-muted-foreground">
                  {formatDate(tenant.createdAt)}
                </span>
              </TableCell>
              <TableCell>
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
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-8 w-8 text-destructive hover:text-destructive"
                    onClick={() => onDelete(tenant)}
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
