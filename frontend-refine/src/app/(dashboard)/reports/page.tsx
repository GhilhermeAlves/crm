"use client";

import { useState } from "react";

import { PageTitle } from "@/components/common/PageTitle";
import { ReportCatalogGrid } from "@/features/reports/components/ReportCatalogGrid";
import { ReportsHeadlineCards } from "@/features/reports/components/ReportsHeadlineCards";
import { SuggestReportDialog } from "@/features/reports/components/SuggestReportDialog";
import { useHeadline } from "@/features/finance/hooks/useFinance";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { useAuthorization } from "@/features/identity/auth/hooks/useAuthorization";

export default function ReportsPage() {
  const { user } = useAuth();
  const { can } = useAuthorization();
  const companyId = user?.companyId ?? null;

  const [suggestOpen, setSuggestOpen] = useState(false);
  const headline = useHeadline(companyId);

  return (
    <div className="space-y-6">
      <div className="space-y-1">
        <PageTitle>Relatórios da clínica</PageTitle>
        <p className="text-sm text-muted-foreground">
          Acompanhe os principais números e acesse os relatórios da sua clínica.
        </p>
      </div>

      <ReportsHeadlineCards
        revenueThisMonthCents={headline.data?.revenueThisMonthCents ?? 0}
        attendancesThisWeek={headline.data?.attendancesThisWeek ?? 0}
        budgetsCreatedThisMonth={headline.data?.budgetsCreatedThisMonth ?? 0}
        isLoading={headline.isLoading}
      />

      <ReportCatalogGrid can={can} onSuggest={() => setSuggestOpen(true)} />

      <SuggestReportDialog open={suggestOpen} onOpenChange={setSuggestOpen} companyId={companyId} />
    </div>
  );
}
