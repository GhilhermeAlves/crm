"use client";

import { useRouter } from "next/navigation";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useCreateLead } from "@/features/leads/hooks/useLeads";
import { LeadForm } from "@/features/leads/components/LeadForm";
import { PageTitle } from "@/components/common/PageTitle";
import type { LeadFormValues } from "@/features/leads/schemas/lead.schema";
import { ROUTES } from "@/lib/constants";

export default function NewLeadPage() {
  const router = useRouter();
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const createLead = useCreateLead(companyId);

  const handleSubmit = (data: LeadFormValues) => {
    if (!companyId) return;
    createLead.mutate(
      {
        contactId: data.contactId,
        status: data.status,
        source: data.source,
        score: Number(data.score),
        classification: data.classification || undefined,
        assignedTo: data.assignedTo || undefined,
        notes: data.notes || undefined,
      },
      { onSuccess: () => router.push(ROUTES.LEADS) },
    );
  };

  return (
    <div className="space-y-6">
      <PageTitle>Novo Lead</PageTitle>
      <LeadForm
        mode="create"
        companyId={companyId}
        onSubmit={handleSubmit}
        onCancel={() => router.back()}
        isLoading={createLead.isPending}
      />
    </div>
  );
}
