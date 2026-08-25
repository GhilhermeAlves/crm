"use client";

import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/features/auth/hooks/useAuth";
import {
  useCreateCampaign,
  useAttachChannel,
  useTemplates,
} from "@/features/campaigns/hooks/useCampaigns";
import { useChannels } from "@/features/omnichannel/hooks/useOmnichannel";
import { PageTitle } from "@/components/common/PageTitle";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { ROUTES } from "@/lib/constants";
import type { AudienceType, Campaign } from "@/features/campaigns/types/campaign.types";

const STEPS = ["Informações", "Público", "Canal", "Mensagem", "Agendamento", "Revisão"];

type WizardState = {
  name: string;
  description: string;
  audienceType: AudienceType;
  providerChannelId: string;
  templateId: string;
  newTemplateName: string;
  newTemplateBody: string;
  mode: "now" | "scheduled";
  scheduledAt: string;
};

const initialState: WizardState = {
  name: "",
  description: "",
  audienceType: "CONTACTS",
  providerChannelId: "",
  templateId: "",
  newTemplateName: "",
  newTemplateBody: "",
  mode: "now",
  scheduledAt: "",
};

export default function NewCampaignPage() {
  const router = useRouter();
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;

  const [step, setStep] = useState(0);
  const [state, setState] = useState<WizardState>(initialState);
  const [createdCampaign, setCreatedCampaign] = useState<Campaign | null>(null);
  const [error, setError] = useState<string | null>(null);

  const createMutation = useCreateCampaign(companyId);
  const attachChannelMutation = useAttachChannel(companyId);
  const { data: templatesData } = useTemplates(companyId);
  const { data: channelsData } = useChannels();

  const activeChannels = useMemo(
    () => (channelsData || []).filter((c) => c.status === "ACTIVE"),
    [channelsData],
  );
  const templates = templatesData?.content ?? [];

  const update = (partial: Partial<WizardState>) => setState((s) => ({ ...s, ...partial }));

  const validateStep = (): string | null => {
    switch (step) {
      case 0:
        if (!state.name.trim()) return "Informe o nome da campanha.";
        return null;
      case 1:
        if (!["CONTACTS", "LEADS"].includes(state.audienceType)) return "Selecione o público.";
        return null;
      case 2:
        if (!state.providerChannelId) return "Selecione um canal ativo.";
        return null;
      case 3:
        if (state.templateId === "__new__") {
          if (!state.newTemplateName.trim()) return "Informe o nome do novo template.";
          if (!state.newTemplateBody.trim()) return "Informe o conteúdo da mensagem.";
        } else if (!state.templateId) {
          return "Selecione um template.";
        }
        return null;
      case 4:
        if (state.mode === "scheduled" && !state.scheduledAt) {
          return "Informe a data e hora do agendamento.";
        }
        if (state.mode === "scheduled" && new Date(state.scheduledAt).getTime() <= Date.now()) {
          return "A data de agendamento deve estar no futuro.";
        }
        return null;
      default:
        return null;
    }
  };

  const handleFinish = async () => {
    setError(null);
    try {
      let campaign = createdCampaign;
      if (!campaign) {
        campaign = await createMutation.mutateAsync({
          name: state.name,
          description: state.description || undefined,
          audienceType: state.audienceType,
        });
        setCreatedCampaign(campaign);
      }

      let templateId = state.templateId;
      if (templateId === "__new__") {
        const template = await import("@/features/campaigns/services/campaign.service").then((m) =>
          m.TemplateService.create(companyId as string, {
            name: state.newTemplateName,
            channelType: "WHATSAPP",
            body: state.newTemplateBody,
          }),
        );
        templateId = template.id;
      }

      await attachChannelMutation.mutateAsync({
        id: campaign.id,
        data: {
          channelType: "WHATSAPP",
          providerChannelId: state.providerChannelId,
          templateId,
        },
      });

      if (state.mode === "scheduled") {
        const { CampaignService } = await import("@/features/campaigns/services/campaign.service");
        await CampaignService.schedule(companyId as string, campaign.id, {
          scheduledAt: new Date(state.scheduledAt).toISOString(),
        });
      } else {
        const { CampaignService } = await import("@/features/campaigns/services/campaign.service");
        await CampaignService.executeNow(companyId as string, campaign.id);
      }

      router.push(`${ROUTES.CAMPAIGNS}/${campaign.id}`);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Erro ao criar campanha");
    }
  };

  const next = async () => {
    const validationError = validateStep();
    if (validationError) {
      setError(validationError);
      return;
    }
    setError(null);

    // Persiste a campanha base ao sair da etapa Informações
    if (step === 0 && !createdCampaign) {
      try {
        const campaign = await createMutation.mutateAsync({
          name: state.name,
          description: state.description || undefined,
          audienceType: state.audienceType,
        });
        setCreatedCampaign(campaign);
      } catch {
        setError("Erro ao criar campanha. Verifique os dados.");
        return;
      }
    }
    setStep((s) => Math.min(s + 1, STEPS.length - 1));
  };

  const back = () => {
    setError(null);
    setStep((s) => Math.max(0, s - 1));
  };

  const busy = createMutation.isPending || attachChannelMutation.isPending;

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <PageTitle>Nova Campanha</PageTitle>

      <ol className="flex flex-wrap gap-2 text-sm" aria-label="Etapas">
        {STEPS.map((label, i) => (
          <li
            key={label}
            className={`rounded-full px-3 py-1 ${
              i === step
                ? "bg-primary text-primary-foreground"
                : i < step
                  ? "bg-muted text-muted-foreground"
                  : "border text-muted-foreground"
            }`}
          >
            {i + 1}. {label}
          </li>
        ))}
      </ol>

      <div className="space-y-4 rounded-md border p-6">
        {step === 0 && (
          <>
            <div className="space-y-2">
              <Label htmlFor="name">Nome</Label>
              <Input
                id="name"
                value={state.name}
                onChange={(e) => update({ name: e.target.value })}
                placeholder="Ex.: Black Friday — Clientes Ativos"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="description">Descrição</Label>
              <Textarea
                id="description"
                value={state.description}
                onChange={(e) => update({ description: e.target.value })}
                placeholder="Objetivo da campanha (opcional)"
              />
            </div>
          </>
        )}

        {step === 1 && (
          <div className="space-y-3" role="radiogroup" aria-label="Público da campanha">
            <label className="flex items-center space-x-2 text-sm">
              <input
                type="radio"
                name="audienceType"
                value="CONTACTS"
                checked={state.audienceType === "CONTACTS"}
                onChange={() => update({ audienceType: "CONTACTS" })}
              />
              <span>Contatos ativos com telefone</span>
            </label>
            <label className="flex items-center space-x-2 text-sm">
              <input
                type="radio"
                name="audienceType"
                value="LEADS"
                checked={state.audienceType === "LEADS"}
                onChange={() => update({ audienceType: "LEADS" })}
              />
              <span>Leads (via contato vinculado)</span>
            </label>
          </div>
        )}

        {step === 2 && (
          <div className="space-y-2">
            <Label>Canal WhatsApp</Label>
            <Select
              value={state.providerChannelId}
              onValueChange={(v) => update({ providerChannelId: v })}
            >
              <SelectTrigger aria-label="Selecionar canal">
                <SelectValue placeholder="Selecione um canal ativo" />
              </SelectTrigger>
              <SelectContent>
                {activeChannels.map((channel) => (
                  <SelectItem key={channel.id} value={channel.id}>
                    {channel.name} ({channel.externalId ?? "sem número"})
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            {activeChannels.length === 0 && (
              <p className="text-sm text-muted-foreground">
                Nenhum canal ativo. Cadastre um canal em Comunicação → Canais.
              </p>
            )}
          </div>
        )}

        {step === 3 && (
          <div className="space-y-4">
            <div className="space-y-2">
              <Label>Template</Label>
              <Select value={state.templateId} onValueChange={(v) => update({ templateId: v })}>
                <SelectTrigger aria-label="Selecionar template">
                  <SelectValue placeholder="Selecione ou crie um template" />
                </SelectTrigger>
                <SelectContent>
                  {templates.map((t) => (
                    <SelectItem key={t.id} value={t.id}>
                      {t.name} (v{t.version})
                    </SelectItem>
                  ))}
                  <SelectItem value="__new__">+ Criar novo template</SelectItem>
                </SelectContent>
              </Select>
            </div>
            {state.templateId === "__new__" && (
              <>
                <div className="space-y-2">
                  <Label htmlFor="tpl-name">Nome do template</Label>
                  <Input
                    id="tpl-name"
                    value={state.newTemplateName}
                    onChange={(e) => update({ newTemplateName: e.target.value })}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="tpl-body">Mensagem (variáveis: {"{{nome}}"})</Label>
                  <Textarea
                    id="tpl-body"
                    rows={4}
                    value={state.newTemplateBody}
                    onChange={(e) => update({ newTemplateBody: e.target.value })}
                    placeholder="Olá {{primeiroNome}}, temos novidades para você!"
                  />
                </div>
              </>
            )}
          </div>
        )}

        {step === 4 && (
          <div className="space-y-4">
            <div className="space-y-3" role="radiogroup" aria-label="Modo de execução">
              <Label>Quando executar?</Label>
              <label className="flex items-center space-x-2 text-sm">
                <input
                  type="radio"
                  name="executionMode"
                  value="now"
                  checked={state.mode === "now"}
                  onChange={() => update({ mode: "now" })}
                />
                <span>Executar agora</span>
              </label>
              <label className="flex items-center space-x-2 text-sm">
                <input
                  type="radio"
                  name="executionMode"
                  value="scheduled"
                  checked={state.mode === "scheduled"}
                  onChange={() => update({ mode: "scheduled" })}
                />
                <span>Agendar</span>
              </label>
            </div>
            {state.mode === "scheduled" && (
              <div className="space-y-2">
                <Label htmlFor="schedule-at">Data e hora</Label>
                <Input
                  id="schedule-at"
                  type="datetime-local"
                  value={state.scheduledAt}
                  onChange={(e) => update({ scheduledAt: e.target.value })}
                />
              </div>
            )}
          </div>
        )}

        {step === 5 && createdCampaign && (
          <dl className="space-y-2 text-sm">
            <div>
              <dt className="inline font-medium">Nome: </dt>
              <dd className="inline">{state.name}</dd>
            </div>
            <div>
              <dt className="inline font-medium">Público: </dt>
              <dd className="inline">{state.audienceType === "CONTACTS" ? "Contatos" : "Leads"}</dd>
            </div>
            <div>
              <dt className="inline font-medium">Canal: </dt>
              <dd className="inline">
                {activeChannels.find((c) => c.id === state.providerChannelId)?.name}
              </dd>
            </div>
            <div>
              <dt className="inline font-medium">Template: </dt>
              <dd className="inline">
                {state.templateId === "__new__"
                  ? state.newTemplateName
                  : templates.find((t) => t.id === state.templateId)?.name}
              </dd>
            </div>
            <div>
              <dt className="inline font-medium">Execução: </dt>
              <dd className="inline">
                {state.mode === "now"
                  ? "Imediata"
                  : `Agendada para ${new Date(state.scheduledAt).toLocaleString("pt-BR")}`}
              </dd>
            </div>
          </dl>
        )}

        {error && <p className="text-sm text-destructive">{error}</p>}
      </div>

      <div className="flex justify-between">
        <Button variant="outline" onClick={back} disabled={step === 0 || busy}>
          Voltar
        </Button>
        {step < STEPS.length - 1 ? (
          <Button onClick={next} disabled={busy}>
            {busy ? "Salvando..." : "Próximo"}
          </Button>
        ) : (
          <Button onClick={handleFinish} disabled={busy}>
            {busy ? "Processando..." : "Confirmar e iniciar"}
          </Button>
        )}
      </div>
    </div>
  );
}
