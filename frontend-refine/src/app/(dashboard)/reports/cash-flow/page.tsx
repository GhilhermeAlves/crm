"use client";

import { CashFlowReport } from "@/features/finance/components/reports/CashFlowReport";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";

export default function ReportsCashFlowPage() {
  const { user } = useAuth();
  return <CashFlowReport companyId={user?.companyId ?? null} />;
}
