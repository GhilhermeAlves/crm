"use client";

import { useState } from "react";
import { toast } from "sonner";
import { Copy } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Switch } from "@/components/ui/switch";
import {
  StatusBadge,
  LeadStatusBadgePreset,
  DealStageBadgePreset,
  UserStatusBadgePreset,
  ChannelStatusBadgePreset,
} from "@/components/ui/status-badge";

export function FeedbackSection() {
  const [badgeAppearance, setBadgeAppearance] = useState<"subtle" | "solid" | "outline">("subtle");
  const [badgeWithDot, setBadgeWithDot] = useState(true);
  const [badgePulse, setBadgePulse] = useState(false);

  const handleCopy = (text: string, label: string) => {
    navigator.clipboard.writeText(text);
    toast.success(`${label} copiado!`, {
      description: text,
      duration: 2000,
    });
  };

  return (
    <section className="space-y-8" aria-labelledby="design-system-feedback-title">
      <h2 id="design-system-feedback-title" className="text-xl font-bold tracking-tight">
        Feedback Semântico & Status
      </h2>

      {/* Badges de Status Semânticos (Design System Core) */}
      <Card>
        <CardHeader className="flex flex-col gap-4 pb-2 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <CardTitle className="flex items-center gap-2 text-base">
              <span>StatusBadge Polimórfico</span>
              <Badge variant="outline" className="border-crm-primary/30 text-xs text-crm-primary">
                Unificado v1.0
              </Badge>
            </CardTitle>
            <CardDescription>
              Substitui os 12 componentes isolados de badges do sistema com suporte a temas,
              indicador de ponto, animação de pulso e presets automáticos por entidade.
            </CardDescription>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <Button
              variant="ghost"
              size="sm"
              className="gap-1 text-xs text-muted-foreground"
              onClick={() =>
                handleCopy(
                  `<StatusBadge intent="success" withDot appearance="${badgeAppearance}">Fechado / Ganho</StatusBadge>`,
                  "Código JSX",
                )
              }
            >
              <Copy className="h-3 w-3" /> Copiar JSX
            </Button>
          </div>
        </CardHeader>

        <CardContent className="space-y-6 pt-4">
          {/* Controles da Badge */}
          <div className="flex flex-wrap items-center justify-between gap-4 rounded-lg border bg-muted/20 p-3">
            <div className="flex items-center gap-2">
              <span className="text-xs font-semibold text-muted-foreground">Estilo Visual:</span>
              {(["subtle", "solid", "outline"] as const).map((app) => (
                <Button
                  key={app}
                  size="sm"
                  variant={badgeAppearance === app ? "default" : "outline"}
                  className="h-7 px-2.5 text-xs capitalize"
                  onClick={() => setBadgeAppearance(app)}
                >
                  {app}
                </Button>
              ))}
            </div>

            <div className="flex flex-wrap items-center gap-4">
              <div className="flex items-center space-x-2">
                <Switch id="badge-dot" checked={badgeWithDot} onCheckedChange={setBadgeWithDot} />
                <label htmlFor="badge-dot" className="cursor-pointer text-xs font-medium">
                  Ponto Indicador (withDot)
                </label>
              </div>

              <div className="flex items-center space-x-2">
                <Switch id="badge-pulse" checked={badgePulse} onCheckedChange={setBadgePulse} />
                <label htmlFor="badge-pulse" className="cursor-pointer text-xs font-medium">
                  Pulsar (pulseDot)
                </label>
              </div>
            </div>
          </div>

          {/* 1. Intenções Fundamentais */}
          <div className="space-y-2">
            <h4 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              1. Intenções Semânticas Universais
            </h4>
            <div className="flex flex-wrap items-center gap-2.5">
              <StatusBadge
                intent="success"
                appearance={badgeAppearance}
                withDot={badgeWithDot}
                pulseDot={badgePulse}
              >
                Success (Sucesso)
              </StatusBadge>

              <StatusBadge
                intent="warning"
                appearance={badgeAppearance}
                withDot={badgeWithDot}
                pulseDot={badgePulse}
              >
                Warning (Atenção)
              </StatusBadge>

              <StatusBadge
                intent="danger"
                appearance={badgeAppearance}
                withDot={badgeWithDot}
                pulseDot={badgePulse}
              >
                Danger (Perigo)
              </StatusBadge>

              <StatusBadge
                intent="info"
                appearance={badgeAppearance}
                withDot={badgeWithDot}
                pulseDot={badgePulse}
              >
                Info (Informação)
              </StatusBadge>

              <StatusBadge
                intent="indigo"
                appearance={badgeAppearance}
                withDot={badgeWithDot}
                pulseDot={badgePulse}
              >
                Indigo (Descoberta)
              </StatusBadge>

              <StatusBadge
                intent="purple"
                appearance={badgeAppearance}
                withDot={badgeWithDot}
                pulseDot={badgePulse}
              >
                Purple (Negociação)
              </StatusBadge>

              <StatusBadge
                intent="neutral"
                appearance={badgeAppearance}
                withDot={badgeWithDot}
                pulseDot={badgePulse}
              >
                Neutral (Padrão)
              </StatusBadge>
            </div>
          </div>

          {/* 2. Presets de Entidades de Negócio */}
          <div className="space-y-4 border-t pt-4">
            <h4 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              2. Presets Integrados de Negócio (Zero Boilerplate)
            </h4>

            {/* Pipeline Stages */}
            <div className="space-y-1.5">
              <span className="text-xs font-medium text-foreground">Pipeline & Deals:</span>
              <div className="flex flex-wrap items-center gap-2">
                {["Novo", "Descoberta", "Proposta", "Negociação", "Fechado/Ganho", "Perdido"].map(
                  (st) => (
                    <DealStageBadgePreset
                      key={st}
                      stage={st}
                      appearance={badgeAppearance}
                      withDot={badgeWithDot}
                      pulseDot={badgePulse && st === "Fechado/Ganho"}
                    />
                  ),
                )}
              </div>
            </div>

            {/* Lead Statuses */}
            <div className="space-y-1.5">
              <span className="text-xs font-medium text-foreground">Módulo de Leads:</span>
              <div className="flex flex-wrap items-center gap-2">
                {["NEW", "CONTACTED", "QUALIFIED", "UNQUALIFIED", "CONVERTED", "LOST"].map((st) => (
                  <LeadStatusBadgePreset
                    key={st}
                    status={st}
                    appearance={badgeAppearance}
                    withDot={badgeWithDot}
                  />
                ))}
              </div>
            </div>

            {/* User Statuses */}
            <div className="space-y-1.5">
              <span className="text-xs font-medium text-foreground">Usuários do Sistema:</span>
              <div className="flex flex-wrap items-center gap-2">
                {["active", "pending", "locked", "inactive"].map((st) => (
                  <UserStatusBadgePreset
                    key={st}
                    status={st}
                    appearance={badgeAppearance}
                    withDot={badgeWithDot}
                    pulseDot={badgePulse && st === "active"}
                  />
                ))}
              </div>
            </div>

            {/* Omnichannel Channels */}
            <div className="space-y-1.5">
              <span className="text-xs font-medium text-foreground">
                Canais Omnichannel / WhatsApp:
              </span>
              <div className="flex flex-wrap items-center gap-2">
                {["ACTIVE", "CONNECTING", "INACTIVE", "ERROR"].map((st) => (
                  <ChannelStatusBadgePreset
                    key={st}
                    status={st}
                    appearance={badgeAppearance}
                    withDot={badgeWithDot}
                    pulseDot={st === "ACTIVE" || st === "CONNECTING"}
                  />
                ))}
              </div>
            </div>
          </div>
        </CardContent>
      </Card>
    </section>
  );
}
