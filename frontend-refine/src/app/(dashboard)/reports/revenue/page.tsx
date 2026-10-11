"use client";

import { RevenueReport } from "@/features/finance/components/reports/RevenueReport";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";

export default function ReportsRevenuePage() {
  const { user } = useAuth();
  return <RevenueReport companyId={user?.companyId ?? null} />;
}
