"use client";

import { useState } from "react";
import { Plus, Trash2 } from "lucide-react";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { useSchedulingPermissions } from "@/features/sales/scheduling/schemas/scheduling.schema";
import {
  useCreateHoliday,
  useDeleteHoliday,
  useHolidays,
  useUpdateHoliday,
} from "@/features/sales/holiday/hooks/useHolidays";
import type { CompanyHoliday } from "@/features/sales/holiday/types/holiday.types";
import { formatDateOnly } from "@/features/sales/recall/utils/recall-utils";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Switch } from "@/components/ui/switch";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";

export default function HolidaysSettingsPage() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const { canConfigure } = useSchedulingPermissions();
  const { data: holidays = [], isLoading } = useHolidays(companyId);
  const createHoliday = useCreateHoliday(companyId);
  const updateHoliday = useUpdateHoliday(companyId);
  const deleteHoliday = useDeleteHoliday(companyId);

  const [date, setDate] = useState("");
  const [name, setName] = useState("");
  const [recurringYearly, setRecurringYearly] = useState(false);
  const [toDelete, setToDelete] = useState<CompanyHoliday | null>(null);

  const sorted = [...holidays].sort((a, b) => a.date.localeCompare(b.date));
  const canAdd = !!date && !!name.trim();

  const add = () => {
    if (!canAdd) return;
    createHoliday.mutate(
      { date, name: name.trim(), recurringYearly, blocksSchedule: true },
      {
        onSuccess: () => {
          setDate("");
          setName("");
          setRecurringYearly(false);
        },
      },
    );
  };

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <h2 className="text-xl font-semibold">Feriados da clínica</h2>
        <p className="text-sm text-muted-foreground">
          Recessos e feriados próprios, além dos nacionais que a agenda já mostra. Com
          &quot;Bloqueia agenda&quot; ligado, o dia fica sem horários livres para agendamento online
          e pelo WhatsApp.
        </p>
      </div>

      {canConfigure && (
        <div className="flex flex-wrap items-center gap-2 rounded-lg border bg-card p-3">
          <Input
            type="date"
            value={date}
            onChange={(e) => setDate(e.target.value)}
            aria-label="Data do feriado"
            className="w-40"
          />
          <Input
            value={name}
            onChange={(e) => setName(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && add()}
            placeholder="Nome (ex.: Recesso de fim de ano)"
            aria-label="Nome do feriado"
            className="min-w-48 flex-1"
          />
          <label className="flex items-center gap-2 text-sm">
            <Checkbox checked={recurringYearly} onCheckedChange={(v) => setRecurringYearly(!!v)} />
            Repete todo ano
          </label>
          <Button onClick={add} disabled={!canAdd || createHoliday.isPending}>
            <Plus className="h-4 w-4" />
            Adicionar
          </Button>
        </div>
      )}

      <div className="divide-y rounded-lg border bg-card">
        {isLoading && <p className="p-4 text-sm text-muted-foreground">Carregando…</p>}
        {!isLoading && sorted.length === 0 && (
          <p className="p-4 text-sm text-muted-foreground">Nenhum feriado da clínica cadastrado.</p>
        )}
        {sorted.map((holiday) => (
          <div key={holiday.id} className="flex flex-wrap items-center gap-3 p-3 text-sm">
            <span className="w-24 tabular-nums text-muted-foreground">
              {holiday.recurringYearly
                ? formatDateOnly(holiday.date).slice(0, 5)
                : formatDateOnly(holiday.date)}
            </span>
            <span className="min-w-32 flex-1 font-medium">{holiday.name}</span>
            {holiday.recurringYearly && (
              <span className="rounded-full bg-muted px-2 py-0.5 text-xs text-muted-foreground">
                Todo ano
              </span>
            )}
            <label className="flex items-center gap-2 text-xs text-muted-foreground">
              Bloqueia agenda
              <Switch
                checked={holiday.blocksSchedule}
                disabled={!canConfigure}
                aria-label={`${holiday.name} bloqueia agenda`}
                onCheckedChange={(blocksSchedule) =>
                  updateHoliday.mutate({ id: holiday.id, data: { blocksSchedule } })
                }
              />
            </label>
            {canConfigure && (
              <Button
                variant="ghost"
                size="icon"
                className="h-8 w-8"
                aria-label={`Excluir ${holiday.name}`}
                onClick={() => setToDelete(holiday)}
              >
                <Trash2 className="h-4 w-4" />
              </Button>
            )}
          </div>
        ))}
      </div>

      <ConfirmDialog
        open={!!toDelete}
        onOpenChange={(open) => !open && setToDelete(null)}
        title="Excluir feriado?"
        description="O dia volta a ter horários livres na agenda."
        confirmLabel="Excluir"
        variant="destructive"
        onConfirm={() => {
          if (toDelete) deleteHoliday.mutate(toDelete.id);
          setToDelete(null);
        }}
      />
    </div>
  );
}
