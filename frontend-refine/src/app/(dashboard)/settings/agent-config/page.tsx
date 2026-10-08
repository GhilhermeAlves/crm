"use client";

import { useEffect, useMemo, useState, type ReactNode } from "react";
import { Save, Loader2, ArrowDownToLine } from "lucide-react";
import {
  useAgentConfig,
  useUpdateAgentConfig,
  useAiPermissions,
} from "@/features/automation/ai/hooks/useAi";
import type {
  AgentConfig,
  AgentConfigRequest,
  VoiceReplyMode,
} from "@/features/automation/ai/types/ai.types";
import { PageTitle } from "@/components/common/PageTitle";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Textarea } from "@/components/ui/textarea";
import { Skeleton } from "@/components/ui/skeleton";

/**
 * Configuração do Agente de IA, organizada pela arquitetura do agente:
 * Identidade · Comportamento · Ferramentas · Modelo · Conversação.
 *
 * Só ficam aqui as configurações PERMANENTES. Dados da clínica, do paciente,
 * agenda, histórico e memórias são carregados pelo CRM no momento de cada
 * resposta — não precisam (nem devem) ser escritos no texto do agente.
 *
 * O "prompt legado" continua funcionando enquanto Identidade e Comportamento
 * estiverem vazios. A empresa nunca vem do formulário (RLS FORCE no backend).
 */

/** Estado do formulário: listas de regras/instruções editadas como "uma por linha". */
type Draft = {
  aiEnabled: boolean;
  allowAutoReply: boolean;
  systemPrompt: string;
  name: string;
  description: string;
  persona: string;
  objective: string;
  tone: string;
  rules: string;
  instructions: string;
  model: string;
  temperature: string;
  maxTokens: string;
  cooldownMinutes: number;
  maxChars: number;
  voiceReplyMode: VoiceReplyMode;
  memoryEnabled: boolean;
  humanTransferEnabled: boolean;
};

function toDraft(config: AgentConfig): Draft {
  return {
    aiEnabled: config.aiEnabled,
    allowAutoReply: config.allowAutoReply,
    systemPrompt: config.systemPrompt ?? "",
    name: config.identity?.name ?? "",
    description: config.identity?.description ?? "",
    persona: config.identity?.persona ?? "",
    objective: config.behavior?.objective ?? "",
    tone: config.behavior?.tone ?? "",
    rules: (config.behavior?.rules ?? []).join("\n"),
    instructions: (config.behavior?.instructions ?? []).join("\n"),
    model: config.model ?? "",
    temperature: config.temperature == null ? "" : String(config.temperature),
    maxTokens: config.maxTokens == null ? "" : String(config.maxTokens),
    cooldownMinutes: config.cooldownMinutes,
    maxChars: config.maxChars,
    voiceReplyMode: config.voiceReplyMode ?? "MIRROR",
    memoryEnabled: config.conversation?.memory.enabled ?? false,
    humanTransferEnabled: config.tools?.humanTransferEnabled ?? false,
  };
}

const orNull = (value: string) => (value.trim() === "" ? null : value.trim());
const lines = (value: string) =>
  value
    .split("\n")
    .map((line) => line.replace(/^\s*[-•*]\s*/, "").trim())
    .filter(Boolean);

function toRequest(draft: Draft): AgentConfigRequest {
  return {
    aiEnabled: draft.aiEnabled,
    allowAutoReply: draft.allowAutoReply,
    systemPrompt: orNull(draft.systemPrompt),
    model: orNull(draft.model),
    temperature: draft.temperature === "" ? null : Number(draft.temperature),
    maxTokens: draft.maxTokens === "" ? null : Number(draft.maxTokens),
    cooldownMinutes: draft.cooldownMinutes,
    maxChars: draft.maxChars,
    voiceReplyMode: draft.voiceReplyMode,
    identity: {
      name: orNull(draft.name),
      description: orNull(draft.description),
      persona: orNull(draft.persona),
    },
    behavior: {
      objective: orNull(draft.objective),
      tone: orNull(draft.tone),
      rules: lines(draft.rules),
      instructions: lines(draft.instructions),
    },
    memoryEnabled: draft.memoryEnabled,
    humanTransferEnabled: draft.humanTransferEnabled,
  };
}

function hasStructuredProfile(draft: Draft) {
  return [
    draft.name,
    draft.description,
    draft.persona,
    draft.objective,
    draft.tone,
    draft.rules,
    draft.instructions,
  ].some((value) => value.trim() !== "");
}

function Section({
  title,
  description,
  children,
}: {
  title: string;
  description: string;
  children: ReactNode;
}) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
        <CardDescription>{description}</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">{children}</CardContent>
    </Card>
  );
}

function ToggleRow({
  id,
  label,
  hint,
  checked,
  onChange,
}: {
  id: string;
  label: string;
  hint: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
}) {
  return (
    <div className="flex items-center justify-between gap-4">
      <div>
        <Label htmlFor={id} className="text-base">
          {label}
        </Label>
        <p className="text-sm text-muted-foreground">{hint}</p>
      </div>
      <Switch id={id} checked={checked} onCheckedChange={onChange} />
    </div>
  );
}

export default function AgentConfigPage() {
  const { canManageAgentConfig } = useAiPermissions();
  const configQuery = useAgentConfig(canManageAgentConfig);
  const updateConfig = useUpdateAgentConfig();

  const config = configQuery.data;
  const initial = useMemo(() => (config ? toDraft(config) : null), [config]);
  const [draft, setDraft] = useState<Draft | null>(null);

  useEffect(() => {
    if (initial) {
      setDraft(initial);
    }
  }, [initial]);

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

  if (configQuery.isLoading || !draft || !initial) {
    return (
      <div className="space-y-6">
        <PageTitle>Agente de IA</PageTitle>
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  const hasChanges = JSON.stringify(draft) !== JSON.stringify(initial);
  const structured = hasStructuredProfile(draft);
  const set = <K extends keyof Draft>(key: K, value: Draft[K]) =>
    setDraft((d) => (d ? { ...d, [key]: value } : d));

  function copyLegacyToPersona() {
    setDraft((d) =>
      d ? { ...d, persona: d.persona.trim() === "" ? d.systemPrompt : d.persona } : d,
    );
  }

  function handleSave() {
    if (draft) {
      updateConfig.mutate(toRequest(draft));
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <PageTitle>Agente de IA</PageTitle>
        <p className="text-sm text-muted-foreground">
          Defina quem o agente é e como ele se comporta. Dados da clínica, do paciente, agenda e
          histórico são buscados no CRM a cada resposta — não precisam ser escritos aqui.
        </p>
      </div>

      <Section
        title="Ativação"
        description="A IA só responde no WhatsApp quando o agente estiver ligado e a auto-resposta permitida."
      >
        <ToggleRow
          id="aiEnabled"
          label="Agente de IA ativo"
          hint="Liga/desliga o agente autônomo desta empresa."
          checked={draft.aiEnabled}
          onChange={(checked) => set("aiEnabled", checked)}
        />
        <ToggleRow
          id="allowAutoReply"
          label="Permitir auto-resposta no WhatsApp"
          hint="Sem esta permissão, o agente não responde sozinho a mensagens recebidas."
          checked={draft.allowAutoReply}
          onChange={(checked) => set("allowAutoReply", checked)}
        />
      </Section>

      <Section title="Identidade" description="Quem o agente é.">
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="space-y-2">
            <Label htmlFor="name">Nome</Label>
            <Input
              id="name"
              maxLength={120}
              value={draft.name}
              onChange={(e) => set("name", e.target.value)}
              placeholder="Ex.: Ana Laura"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="description">Descrição</Label>
            <Input
              id="description"
              maxLength={500}
              value={draft.description}
              onChange={(e) => set("description", e.target.value)}
              placeholder="Ex.: Assistente virtual da clínica odontológica"
            />
          </div>
        </div>
        <div className="space-y-2">
          <Label htmlFor="persona">Persona</Label>
          <Textarea
            id="persona"
            rows={4}
            maxLength={6000}
            value={draft.persona}
            onChange={(e) => set("persona", e.target.value)}
            placeholder="Ex.: Você é Ana Laura, assistente virtual da clínica. Fala de forma simpática e próxima."
          />
        </div>
      </Section>

      <Section
        title="Comportamento"
        description="Como o agente age: objetivo, tom, regras e instruções."
      >
        <div className="space-y-2">
          <Label htmlFor="objective">Objetivo</Label>
          <Textarea
            id="objective"
            rows={2}
            maxLength={2000}
            value={draft.objective}
            onChange={(e) => set("objective", e.target.value)}
            placeholder="Ex.: Atender pacientes e auxiliar no agendamento."
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="tone">Tom de voz</Label>
          <Input
            id="tone"
            maxLength={500}
            value={draft.tone}
            onChange={(e) => set("tone", e.target.value)}
            placeholder="Ex.: Profissional, acolhedor e objetivo"
          />
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="space-y-2">
            <Label htmlFor="rules">Regras (uma por linha)</Label>
            <Textarea
              id="rules"
              rows={5}
              value={draft.rules}
              onChange={(e) => set("rules", e.target.value)}
              placeholder={"Não inventar informações.\nTransferir para humano quando necessário."}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="instructions">Instruções (uma por linha)</Label>
            <Textarea
              id="instructions"
              rows={5}
              value={draft.instructions}
              onChange={(e) => set("instructions", e.target.value)}
              placeholder={
                "Identificar o paciente antes de agendar.\nConfirmar data e horário antes de finalizar."
              }
            />
          </div>
        </div>
      </Section>

      {(draft.systemPrompt.trim() !== "" || !structured) && (
        <Section
          title="Prompt legado"
          description={
            structured
              ? "Não está mais em uso: Identidade e Comportamento têm prioridade. Você pode apagá-lo quando quiser."
              : "Em uso enquanto Identidade e Comportamento estiverem vazios. Para migrar, copie para a Persona e separe as regras e instruções."
          }
        >
          <Textarea
            id="systemPrompt"
            rows={5}
            value={draft.systemPrompt}
            onChange={(e) => set("systemPrompt", e.target.value)}
            placeholder="Ex.: Você responde como Léo, assistente do setor comercial."
          />
          {draft.systemPrompt.trim() !== "" && draft.persona.trim() === "" && (
            <Button type="button" variant="outline" onClick={copyLegacyToPersona}>
              <ArrowDownToLine className="h-4 w-4" />
              Copiar para a Persona
            </Button>
          )}
        </Section>
      )}

      <Section
        title="Ferramentas"
        description="Ações reais que o agente pode executar no CRM durante a conversa."
      >
        <div>
          <p className="text-base font-medium">Agenda</p>
          <p className="text-sm text-muted-foreground">
            Consultar horários livres e agendar. Fica disponível automaticamente quando há tipos de
            consulta ativos com profissional responsável.
          </p>
        </div>
        <ToggleRow
          id="humanTransferEnabled"
          label="Transferência para humano"
          hint="O agente pode passar a conversa para a equipe; a IA para de responder nela até alguém devolver ao automático."
          checked={draft.humanTransferEnabled}
          onChange={(checked) => set("humanTransferEnabled", checked)}
        />
      </Section>

      <Section
        title="Modelo"
        description="Parâmetros de geração. Campos vazios usam o padrão do provedor."
      >
        <div className="grid gap-4 sm:grid-cols-3">
          <div className="space-y-2">
            <Label htmlFor="model">Modelo</Label>
            <Input
              id="model"
              value={draft.model}
              onChange={(e) => set("model", e.target.value)}
              placeholder="Ex.: gpt-4o"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="temperature">Temperatura</Label>
            <Input
              id="temperature"
              type="number"
              step="0.1"
              min={0}
              max={2}
              value={draft.temperature}
              onChange={(e) => set("temperature", e.target.value)}
              placeholder="Padrão do provedor"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="maxTokens">Máximo de tokens</Label>
            <Input
              id="maxTokens"
              type="number"
              min={1}
              value={draft.maxTokens}
              onChange={(e) => set("maxTokens", e.target.value)}
              placeholder="Padrão do provedor"
            />
          </div>
        </div>
      </Section>

      <Section title="Conversação" description="Ritmo, tamanho e formato das respostas, e memória.">
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="space-y-2">
            <Label htmlFor="cooldownMinutes">Cooldown (minutos)</Label>
            <Input
              id="cooldownMinutes"
              type="number"
              min={0}
              max={1440}
              value={draft.cooldownMinutes}
              onChange={(e) => set("cooldownMinutes", Number(e.target.value))}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="maxChars">Limite máx. de caracteres da resposta</Label>
            <Input
              id="maxChars"
              type="number"
              min={1}
              max={5000}
              value={draft.maxChars}
              onChange={(e) => set("maxChars", Number(e.target.value))}
            />
          </div>
        </div>

        <div className="space-y-2">
          <Label htmlFor="voiceReplyMode">Responder em áudio</Label>
          <select
            id="voiceReplyMode"
            className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
            value={draft.voiceReplyMode}
            onChange={(e) => set("voiceReplyMode", e.target.value as VoiceReplyMode)}
          >
            <option value="MIRROR">Quando o paciente mandar áudio (recomendado)</option>
            <option value="NEVER">Nunca — sempre texto</option>
            <option value="ALWAYS">Sempre em áudio</option>
          </select>
          <p className="text-xs text-muted-foreground">
            Respostas em áudio custam mais (cerca de R$ 0,04 cada). Datas, horários e valores também
            seguem por escrito.
          </p>
        </div>

        <ToggleRow
          id="memoryEnabled"
          label="Memória"
          hint="O agente guarda informações duradouras de pacientes cadastrados (ex.: prefere atendimento pela manhã) e as usa nos próximos atendimentos. Agenda e status continuam vindo do CRM."
          checked={draft.memoryEnabled}
          onChange={(checked) => set("memoryEnabled", checked)}
        />
      </Section>

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
