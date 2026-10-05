"use client";

import { useState } from "react";
import { Switch } from "@/components/ui/switch";
import { Label } from "@/components/ui/label";
import { Bell, Mail } from "lucide-react";

type NotificationEvent = {
  id: string;
  label: string;
  description: string;
  defaultOn: boolean;
};

const NOTIFICATION_EVENTS: NotificationEvent[] = [
  {
    id: "on-booking",
    label: "No momento da marcação do horário pela empresa",
    description: "",
    defaultOn: true,
  },
  {
    id: "24h-before",
    label: "24 horas antes da consulta (com botões de confirmação para o usuário)",
    description: "",
    defaultOn: true,
  },
  {
    id: "1h-before",
    label: "1 hora antes do horário marcado",
    description: "",
    defaultOn: false,
  },
  {
    id: "on-cancel",
    label: "Quando a empresa cancelar a consulta",
    description: "",
    defaultOn: true,
  },
  {
    id: "on-reschedule",
    label: "Quando a empresa remarcar a consulta",
    description: "",
    defaultOn: true,
  },
];

export default function NotificationsPage() {
  const [settings, setSettings] = useState<Record<string, boolean>>(() => {
    const initial: Record<string, boolean> = {};
    NOTIFICATION_EVENTS.forEach((e) => {
      initial[e.id] = e.defaultOn;
    });
    return initial;
  });

  const toggle = (id: string) => {
    setSettings((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <h2 className="text-xl font-semibold">Central de notificações</h2>

      <div className="rounded-lg border border-blue-200 bg-blue-50 p-3 text-sm text-blue-800 dark:border-blue-900 dark:bg-blue-950/30 dark:text-blue-300">
        Controle e confira as notificações enviadas para seus contatos em cada um
        dos eventos abaixo.
      </div>

      <section className="space-y-4 rounded-lg border bg-card p-5">
        <h3 className="text-sm font-semibold text-primary">Agendamento</h3>

        <div className="space-y-3">
          {NOTIFICATION_EVENTS.map((event) => (
            <div
              key={event.id}
              className="flex items-center justify-between rounded-md p-2 transition-colors hover:bg-muted/50"
            >
              <div className="flex items-center gap-3">
                <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-muted">
                  {event.id.includes("before") ? (
                    <Bell className="h-4 w-4 text-muted-foreground" />
                  ) : (
                    <Mail className="h-4 w-4 text-muted-foreground" />
                  )}
                </div>
                <Label className="cursor-pointer text-sm font-medium">
                  {event.label}
                </Label>
              </div>
              <div className="flex items-center gap-2">
                <span className="text-xs text-muted-foreground">
                  Notificação ativa?
                </span>
                <Switch
                  checked={settings[event.id] ?? false}
                  onCheckedChange={() => toggle(event.id)}
                />
              </div>
            </div>
          ))}
        </div>
      </section>

      <section className="space-y-4 rounded-lg border bg-card p-5">
        <h3 className="text-sm font-semibold">Erro no envio de notificações</h3>
        <p className="text-xs text-muted-foreground">
          Gerencie aqui as notificações que não foram enviadas aos clientes
          devido a imprevistos do provedor.
        </p>

        <div className="flex flex-col items-center gap-2 rounded-lg border border-dashed p-8 text-center">
          <Mail className="h-8 w-8 text-muted-foreground/50" />
          <p className="text-sm font-medium text-muted-foreground">
            Sem notificações por aqui
          </p>
          <p className="text-xs text-muted-foreground">
            Que ótimo! Isso significa que todas as notificações foram enviadas
            corretamente aos seus clientes.
          </p>
        </div>
      </section>
    </div>
  );
}
