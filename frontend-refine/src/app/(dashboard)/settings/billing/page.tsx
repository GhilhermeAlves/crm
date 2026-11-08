"use client";

import { PageTitle } from "@/components/common/PageTitle";
import { BillingSettingsView } from "@/features/billing/components/BillingSettingsView";

export default function BillingSettingsPage() {
  return (
    <div className="space-y-6">
      <PageTitle>Plano e cobrança</PageTitle>
      <BillingSettingsView />
    </div>
  );
}
