"use client";

import { useParams, useRouter } from "next/navigation";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useLead, useUpdateLead } from "@/features/leads/hooks/useLeads";
import { LeadForm } from "@/features/leads/components/LeadForm";
import { PageTitle } from "@/components/common/PageTitle";
import { SkeletonForm } from "@/components/feedback/SkeletonForm";
import type { LeadFormValues } from "@/features/leads/schemas/lead.schema";
import { ROUTES } from "@/lib/constants";

export default function EditLeadPage() {
  const params = useParams();
  const router = useRouter();
  const id = params.id as string;
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;

  const { data: lead, isLoading } = useLead(companyId, id);
  const updateLead = useUpdateLead(companyId);

  const handleSubmit = (data: LeadFormValues) => {
    if (!companyId) return;
    updateLead.mutate(
      {
        id,
        data: {
          status: data.status,
          score: Number(data.score),
          classification: data.classification || undefined,
          assignedTo: data.assignedTo || undefined,
          notes: data.notes || undefined,
        },
      },
      { onSuccess: () => router.push(`${ROUTES.LEADS}/${id}`) },
    );
  };

  if (isLoading) {
    return (
      <div className="space-y-6">
        <SkeletonForm />
      </div>
    );
  }

  if (!lead) {
    return (
      <div className="text-center py-12">
        <p className="text-muted-foreground">Lead não encontrado.</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <PageTitle>Editar Lead</PageTitle>
      <LeadForm
        lead={lead}
        mode="edit"
        onSubmit={handleSubmit}
        onCancel={() => router.back()}
        isLoading={updateLead.isPending}
      />
    </div>
  );
}
