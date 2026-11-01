"use client";

import { useParams } from "next/navigation";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { useCampaign } from "@/features/automation/campaigns/hooks/useCampaigns";
import { CampaignWizard } from "@/features/automation/campaigns/components/CampaignWizard";
import { isCampaignEditable } from "@/features/automation/campaigns/types/campaign.types";

export default function EditCampaignPage() {
  const params = useParams<{ id: string }>();
  const { user } = useAuth();
  const { data: campaign, isLoading, isError } = useCampaign(user?.companyId ?? null, params.id);

  if (isLoading) {
    return <div className="py-12 text-center text-muted-foreground">Carregando...</div>;
  }
  if (isError || !campaign) {
    return (
      <div className="py-12 text-center text-muted-foreground">
        Campanha não encontrada ou sem acesso.
      </div>
    );
  }
  if (!isCampaignEditable(campaign.status)) {
    return (
      <div className="py-12 text-center text-muted-foreground">
        Só campanhas em rascunho ou agendadas podem ser editadas.
      </div>
    );
  }
  return <CampaignWizard campaign={campaign} />;
}
