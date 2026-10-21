"use client";

import { Switch } from "@/components/ui/switch";
import { Label } from "@/components/ui/label";
import { Bell, Mail } from "lucide-react";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import {
  useAgendaSettings,
  useSaveAgendaSettings,
} from "@/features/sales/scheduling/hooks/useScheduling";
import { useSchedulingPermissions } from "@/features/sales/scheduling/schemas/scheduling.schema";
import type { AgendaSettings } from "@/features/sales/scheduling/services/scheduling.service";

type NotificationKey =
  | "notifyOnBooking"
  | "notify24hBefore"
  | "notify1hBefore"
  | "notifyOnCancel"
  | "notifyOnReschedule";

const NOTIFICATION_EVENTS: { key: NotificationKey; label: string; reminder: boolean }[] = [
  {
    key: "notifyOnBooking",
    label: "No momento da marcação do horário pela empresa",
    reminder: false,
  },
  {
    key: "notify24hBefore",
    label: "24 horas antes da consulta (com botões de confirmação para o usuário)",
    reminder: true,
  },
  { key: "notify1hBefore", label: "1 hora antes do horário marcado", reminder: true },
  { key: "notifyOnCancel", label: "Quando a empresa cancelar a consulta", reminder: false },
  { key: "notifyOnReschedule", label: "Quando a empresa remarcar a consulta", reminder: false },
];

export default function NotificationsPage() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const perms = useSchedulingPermissions();
  const { data: settings } = useAgendaSettings(companyId);
  const save = useSaveAgendaSettings(companyId);

  const toggle = (current: AgendaSettings, key: NotificationKey) =>
    save.mutate({ ...current, [key]: !current[key] });

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <h2 className="text-xl font-semibold">Central de notificações</h2>

      <div className="rounded-lg border border-blue-200 bg-blue-50 p-3 text-sm text-blue-800 dark:border-blue-900 dark:bg-blue-950/30 dark:text-blue-300">
        Escolha em quais eventos seus contatos devem ser notificados. As escolhas ficam salvas para
        toda a equipe; o envio automático pelo WhatsApp será ativado em uma próxima etapa.
      </div>

      <section className="space-y-4 rounded-lg border bg-card p-5">
        <h3 className="text-sm font-semibold text-primary">Agendamento</h3>

        <div className="space-y-3">
          {NOTIFICATION_EVENTS.map((event) => {
            const id = `notify-${event.key}`;
            return (
              <div
                key={event.key}
                className="flex items-center justify-between rounded-md p-2 transition-colors hover:bg-muted/50"
              >
                <div className="flex items-center gap-3">
                  <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-muted">
                    {event.reminder ? (
                      <Bell className="h-4 w-4 text-muted-foreground" />
                    ) : (
                      <Mail className="h-4 w-4 text-muted-foreground" />
                    )}
                  </div>
                  <Label htmlFor={id} className="cursor-pointer text-sm font-medium">
                    {event.label}
                  </Label>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-xs text-muted-foreground">Notificação ativa?</span>
                  <Switch
                    id={id}
                    checked={settings?.[event.key] ?? false}
                    disabled={!settings || !perms.canConfigure || save.isPending}
                    onCheckedChange={() => settings && toggle(settings, event.key)}
                  />
                </div>
              </div>
            );
          })}
        </div>
      </section>
    </div>
  );
}
