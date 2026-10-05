"use client";

import { useEffect, useState } from "react";
import { MessageSquare } from "lucide-react";

import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import {
  useBirthdayMessage,
  useSaveBirthdayMessage,
} from "@/features/sales/scheduling/hooks/useScheduling";

import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Textarea } from "@/components/ui/textarea";

const VARIABLES = [
  { key: "{{Nome contato}}", label: "Nome contato" },
  { key: "{{Nome empresa}}", label: "Nome empresa" },
];

const DEFAULT_TEMPLATE = `{{Nome contato}}, parabéns pelo seu dia! 🎂

A equipe {{Nome empresa}} deseja um ano cheio de saúde e sorrisos.

Conta com a gente sempre! 😊 {{Nome contato}}`;

export default function BirthdayMessagePage() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const { data: saved } = useBirthdayMessage(companyId);
  const saveMessage = useSaveBirthdayMessage(companyId);

  const [enabled, setEnabled] = useState(false);
  const [template, setTemplate] = useState(DEFAULT_TEMPLATE);

  useEffect(() => {
    if (saved) {
      setEnabled(saved.enabled);
      setTemplate(saved.template);
    }
  }, [saved]);

  const handleSave = () => {
    if (!template.trim()) return;
    saveMessage.mutate({ enabled, template });
  };

  const insertVariable = (variable: string) => {
    setTemplate((prev) => prev + variable);
  };

  const previewText = template
    .replace(/\{\{Nome contato\}\}/g, "Maria")
    .replace(/\{\{Nome empresa\}\}/g, "Minha Empresa");

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <h2 className="text-xl font-semibold">Mensagem de aniversário</h2>

      <div className="rounded-lg border bg-card p-4">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-green-100 dark:bg-green-950/50">
            <MessageSquare className="h-5 w-5 text-green-600" />
          </div>
          <div className="flex-1">
            <p className="text-sm font-medium">
              Conecte o WhatsApp da empresa para o envio automático!
            </p>
            <p className="text-xs text-muted-foreground">
              Te ajudo a não esquecer a data especial dos seus contatos.
            </p>
          </div>
          <Button variant="outline" size="sm">
            Conectar
          </Button>
        </div>
      </div>

      <div className="flex items-center justify-between rounded-lg border bg-card p-4">
        <div>
          <p className="text-sm font-medium text-muted-foreground">
            Enviar mensagem de aniversário automaticamente
          </p>
          <p className="text-xs text-muted-foreground">
            Ativa para habilitar a edição e o envio pelo telefone da empresa.
          </p>
        </div>
        <Switch checked={enabled} onCheckedChange={setEnabled} />
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_300px]">
        <section className="space-y-4">
          <Label className="text-sm font-medium text-muted-foreground">
            Modelo de mensagem
          </Label>

          <Textarea
            value={template}
            onChange={(e) => setTemplate(e.target.value)}
            rows={6}
            className="resize-none bg-amber-50/50 font-mono text-sm dark:bg-amber-950/10"
            disabled={!enabled}
          />

          <div className="flex items-center gap-2">
            <span className="text-xs text-muted-foreground">
              Adicione personalização ao texto:
            </span>
            {VARIABLES.map((v) => (
              <Button
                key={v.key}
                variant="outline"
                size="sm"
                className="h-7 rounded-full text-xs"
                onClick={() => insertVariable(v.key)}
                disabled={!enabled}
              >
                {v.label}
              </Button>
            ))}
          </div>
        </section>

        <section className="space-y-2">
          <Label className="text-sm font-medium text-muted-foreground">
            Pré-visualização
          </Label>

          <div className="overflow-hidden rounded-2xl border bg-gradient-to-b from-[#e5ddd5] to-[#d1c7b7] p-4 dark:from-[#1a1a1a] dark:to-[#0d0d0d]">
            <div className="ml-auto max-w-[220px] rounded-lg bg-[#dcf8c6] p-3 text-xs leading-relaxed text-gray-800 shadow-sm dark:bg-[#005c4b] dark:text-gray-100">
              {previewText.split("\n").map((line, i) => (
                <span key={i}>
                  {line}
                  {i < previewText.split("\n").length - 1 && <br />}
                </span>
              ))}
            </div>
          </div>
        </section>
      </div>

      <div className="flex justify-end">
        <Button onClick={handleSave} disabled={saveMessage.isPending || !template.trim()}>
          {saveMessage.isPending ? "Salvando..." : "Salvar"}
        </Button>
      </div>
    </div>
  );
}
