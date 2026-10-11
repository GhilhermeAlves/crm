"use client";

import { OverviewReport } from "@/features/finance/components/reports/OverviewReport";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";

export default function ReportsOverviewPage() {
  const { user } = useAuth();
  return <OverviewReport companyId={user?.companyId ?? null} />;
}
