"use client";

import { AgendaReport } from "@/features/finance/components/reports/AgendaReport";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";

export default function ReportsAgendaPage() {
  const { user } = useAuth();
  return <AgendaReport companyId={user?.companyId ?? null} />;
}
