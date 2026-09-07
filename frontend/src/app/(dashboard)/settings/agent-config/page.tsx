"use client";

import { useEffect, useState } from "react";
import { Save, Loader2 } from "lucide-react";
import { useAgentConfig, useUpdateAgentConfig, useAiPermissions } from "@/features/ai/hooks/useAi";
import type { AgentConfigRequest } from "@/features/ai/types/ai.types";
import { PageTitle } from "@/components/common/PageTitle";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Textarea } from "@/components/ui/textarea";
import { Skeleton } from "@/components/ui/skeleton";

/**
 * Sprint 3 — Configuração do Agente de IA (AgentConfig administrativo).
 *
 * Permite ao ADMIN ligar/desligar o agente autônomo (auto-resposta) e definir
 * o prompt do sistema, modelo de geração e limites (cooldown/maxChars).
 * O vinculamento é com a EMPRESA ATIVA — a empresa nunca vem do formulário,
 * o backend resolve a partir do usuário autenticado (RLS FORCE).
 */
export default function AgentConfigPage() {
  const { canManageAgentConfig } = useAiPermissions();
  const configQuery = useAgentConfig(canManageAgentConfig);
  const updateConfig = useUpdateAgentConfig();

  const [draft, setDraft] = useState<AgentConfigRequest | null>(null);

  const config = configQuery.data;

  useEffect(() => {
    if (config && draft === null) {
      setDraft({
        aiEnabled: config.aiEnabled,
        allowAutoReply: config.allowAutoReply,
        systemPrompt: config.systemPrompt,
        model: config.model,
        temperature: config.temperature,
        maxTokens: config.maxTokens,
        cooldownMinutes: config.cooldownMinutes,
        maxChars: config.maxChars,
      });
    }
  }, [config, draft]);

  if (!canManageAgentConfig) {
    return (
      <div className="space-y-6">
        <PageTitle>Agente de IA</PageTitle>
        <Skeleton className="h-64 w-full" />
        <p className="text-sm text-muted-foreground">
          Você não tem permissão para configurar o agente de IA desta empresa.
        </p>
      </div>
    );
  }

  if (configQuery.isLoading || !draft) {
    return (
      <div className="space-y-6">
        <PageTitle>Agente de IA</PageTitle>
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  const hasChanges =
    draft.aiEnabled !== config?.aiEnabled ||
    draft.allowAutoReply !== config?.allowAutoReply ||
    draft.systemPrompt !== config?.systemPrompt ||
    draft.model !== config?.model ||
    draft.temperature !== config?.temperature ||
    draft.maxTokens !== config?.maxTokens ||
    draft.cooldownMinutes !== config?.cooldownMinutes ||
    draft.maxChars !== config?.maxChars;

  function handleSave() {
    if (draft) {
      updateConfig.mutate(draft);
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <PageTitle>Agente de IA</PageTitle>
        <p className="text-sm text-muted-foreground">
          Controle o atendimento autônomo desta empresa. A IA só responde no WhatsApp quando o
          agente estiver ligado e a permissão de auto-resposta estiver ativa.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Comportamento</CardTitle>
          <CardDescription>
            Quando o agente está ativo, mensagens recebidas sem resposta anterior por um humano
            podem ser atendidas automaticamente (sujeito a cooldown e limite de caracteres).
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="flex items-center justify-between gap-4">
            <div>
              <Label htmlFor="aiEnabled" className="text-base">
                Agente de IA ativo
              </Label>
              <p className="text-sm text-muted-foreground">
                Liga/desliga o agente autônomo desta empresa.
              </p>
            </div>
            <Switch
              id="aiEnabled"
              checked={draft.aiEnabled}
              onCheckedChange={(checked) => setDraft((d) => (d ? { ...d, aiEnabled: checked } : d))}
            />
          </div>

          <div className="flex items-center justify-between gap-4">
            <div>
              <Label htmlFor="allowAutoReply" className="text-base">
                Permitir auto-resposta no WhatsApp
              </Label>
              <p className="text-sm text-muted-foreground">
                Sem esta permissão, o agente não responde sozinho a mensagens recebidas.
              </p>
            </div>
            <Switch
              id="allowAutoReply"
              checked={draft.allowAutoReply}
              onCheckedChange={(checked) =>
                setDraft((d) => (d ? { ...d, allowAutoReply: checked } : d))
              }
            />
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Persona e geração</CardTitle>
          <CardDescription>
            Como o agente se comporta e quais parâmetros de geração usar. Campos opcionais
            (modelo/temperatura/máx. tokens) usam o padrão do provedor quando vazios.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="systemPrompt">Prompt do sistema</Label>
            <Textarea
              id="systemPrompt"
              value={draft.systemPrompt ?? ""}
              onChange={(event) => {
                const value = event.target.value;
                setDraft((d) =>
                  d ? { ...d, systemPrompt: value.trim() === "" ? null : value } : d,
                );
              }}
              placeholder="Ex.: Você responde como Léo, assistente do setor comercial."
              rows={5}
            />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="model">Modelo</Label>
              <Input
                id="model"
                value={draft.model ?? ""}
                onChange={(event) => {
                  const value = event.target.value;
                  setDraft((d) => (d ? { ...d, model: value.trim() === "" ? null : value } : d));
                }}
                placeholder="Ex.: gpt-4o"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="maxTokens">Máximo de tokens (opcional)</Label>
              <Input
                id="maxTokens"
                type="number"
                min={1}
                value={draft.maxTokens ?? ""}
                onChange={(event) => {
                  const value = event.target.value;
                  setDraft((d) =>
                    d ? { ...d, maxTokens: value === "" ? null : Number(value) } : d,
                  );
                }}
                placeholder="Padrão do provedor"
              />
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="temperature">Temperatura (opcional)</Label>
              <Input
                id="temperature"
                type="number"
                step="0.1"
                min={0}
                max={2}
                value={draft.temperature ?? ""}
                onChange={(event) => {
                  const value = event.target.value;
                  setDraft((d) =>
                    d ? { ...d, temperature: value === "" ? null : Number(value) } : d,
                  );
                }}
                placeholder="Padrão do provedor"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="cooldownMinutes">Cooldown (minutos)</Label>
              <Input
                id="cooldownMinutes"
                type="number"
                min={0}
                max={1440}
                value={draft.cooldownMinutes}
                onChange={(event) =>
                  setDraft((d) => (d ? { ...d, cooldownMinutes: Number(event.target.value) } : d))
                }
              />
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="maxChars">Limite máx. de caracteres da resposta</Label>
            <Input
              id="maxChars"
              type="number"
              min={1}
              max={5000}
              value={draft.maxChars}
              onChange={(event) =>
                setDraft((d) => (d ? { ...d, maxChars: Number(event.target.value) } : d))
              }
            />
          </div>
        </CardContent>
      </Card>

      <div className="flex justify-end">
        <Button onClick={handleSave} disabled={updateConfig.isPending || !hasChanges}>
          {updateConfig.isPending ? (
            <>
              <Loader2 className="h-4 w-4 animate-spin" />
              Salvando...
            </>
          ) : (
            <>
              <Save className="h-4 w-4" />
              Salvar configuração
            </>
          )}
        </Button>
      </div>
    </div>
  );
}
