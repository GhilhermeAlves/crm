"use client";

import Link from "next/link";
import { useParams } from "next/navigation";

import { PageTitle } from "@/components/common/PageTitle";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { useAuthorization } from "@/features/identity/auth/hooks/useAuthorization";
import { RecentReportExports } from "@/features/reports/export/RecentReportExports";
import { ReportExportForm } from "@/features/reports/export/ReportExportForm";
import { findReportExport } from "@/features/reports/export/types";
import {
  useReportExports,
  useRequestReportExport,
} from "@/features/reports/export/useReportExports";
import { ROUTES } from "@/lib/constants";

export default function ReportExportPage() {
  const params = useParams<{ type: string }>();
  const definition = findReportExport(params.type);
  const { user } = useAuth();
  const { can } = useAuthorization();
  const companyId = user?.companyId ?? null;

  const exportsQuery = useReportExports(companyId);
  const requestExport = useRequestReportExport(companyId);

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

      {can(definition.permission) ? (
        <ReportExportForm
          key={definition.type}
          definition={definition}
          isSubmitting={requestExport.isPending}
          onSubmit={(request) => requestExport.mutate(request)}
        />
      ) : (
        <p className="text-sm text-muted-foreground">
          Você não tem permissão para gerar este relatório.
        </p>
      )}

      <RecentReportExports
        companyId={companyId}
        exports={exportsQuery.data ?? []}
        isLoading={exportsQuery.isLoading}
      />
    </div>
  );
}
