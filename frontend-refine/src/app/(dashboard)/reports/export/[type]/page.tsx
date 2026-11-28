"use client";

import { useEffect } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";

import { PageTitle } from "@/components/common/PageTitle";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { RecentReportExports } from "@/features/reports/export/RecentReportExports";
import { ReportExportForm } from "@/features/reports/export/ReportExportForm";
import { findReportExport } from "@/features/reports/export/types";
import {
  useCancelReportExport,
  useReportCatalog,
  useReportExports,
  useRequestReportExport,
} from "@/features/reports/export/useReportExports";
import { ROUTES } from "@/lib/constants";

export default function ReportExportPage() {
  const params = useParams<{ type: string }>();
  const router = useRouter();
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  // O catálogo já vem filtrado pelas permissões do usuário (CRM_ADMIN vê todos).
  const catalogQuery = useReportCatalog(companyId);
  const definition = findReportExport(catalogQuery.data, params.type);
  const notAllowed = catalogQuery.isSuccess && !definition;

  const exportsQuery = useReportExports(companyId);
  const requestExport = useRequestReportExport(companyId);
  const cancelExport = useCancelReportExport(companyId);

  // Relatório inexistente ou sem permissão: volta para a lista de Relatórios.
  useEffect(() => {
    if (notAllowed) router.replace(ROUTES.REPORTS);
  }, [notAllowed, router]);

  if (!definition && !catalogQuery.isError) {
    return <p className="text-sm text-muted-foreground">Carregando…</p>;
  }

  if (!definition) {
    return (
      <div className="space-y-2">
        <PageTitle>Relatório não encontrado</PageTitle>
        <Link
          href={ROUTES.REPORTS}
          className="text-sm text-primary underline-offset-4 hover:underline"
        >
          Voltar para Relatórios
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="space-y-1">
        <Link
          href={ROUTES.REPORTS}
          className="text-xs font-medium text-primary underline-offset-4 hover:underline"
        >
          ← Relatórios
        </Link>
        <PageTitle>{definition.label}</PageTitle>
        <p className="max-w-2xl text-sm text-muted-foreground">{definition.description}</p>
      </div>

      <ReportExportForm
        key={definition.type}
        definition={definition}
        isSubmitting={requestExport.isPending}
        onSubmit={(request) => requestExport.mutate(request)}
      />

      <RecentReportExports
        companyId={companyId}
        exports={exportsQuery.data ?? []}
        isLoading={exportsQuery.isLoading}
        onCancel={(exportId) => cancelExport.mutate(exportId)}
        cancelingId={cancelExport.isPending ? (cancelExport.variables ?? null) : null}
      />
    </div>
  );
}
