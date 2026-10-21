"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type { AgendaSettings } from "../services/scheduling.service";

const DURATION_OPTIONS = [10, 15, 20, 30, 40, 45, 50, 60, 90, 120];

interface Props {
  settings: AgendaSettings;
  canConfigure: boolean;
  isSaving: boolean;
  onSave: (settings: AgendaSettings) => void;
}

/** Duração padrão da consulta e horário de trabalho exibido na agenda. */
export function AgendaSettingsForm({ settings, canConfigure, isSaving, onSave }: Props) {
  const [draft, setDraft] = useState<Pick<
    AgendaSettings,
    "defaultDurationMinutes" | "workHoursStart" | "workHoursEnd"
  > | null>(null);
  const values = draft ?? settings;
  const invalidHours = values.workHoursStart >= values.workHoursEnd;
  const durations = DURATION_OPTIONS.includes(values.defaultDurationMinutes)
    ? DURATION_OPTIONS
    : [...DURATION_OPTIONS, values.defaultDurationMinutes].sort((a, b) => a - b);

  const update = (patch: Partial<typeof values>) => setDraft({ ...values, ...patch });

  return (
    <section className="space-y-4 rounded-lg border bg-card p-5">
      <div>
        <h3 className="text-sm font-semibold">Agenda</h3>
        <p className="text-xs text-muted-foreground">
          A duração padrão é usada ao clicar num horário vazio da agenda. O horário de trabalho
          define a faixa exibida e o aviso de consulta fora do horário.
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <div className="space-y-2">
          <Label htmlFor="default-duration">Duração padrão</Label>
          <Select
            value={String(values.defaultDurationMinutes)}
            onValueChange={(v) => update({ defaultDurationMinutes: Number(v) })}
            disabled={!canConfigure}
          >
            <SelectTrigger id="default-duration">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {durations.map((m) => (
                <SelectItem key={m} value={String(m)}>
                  {m} minutos
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <div className="space-y-2">
          <Label htmlFor="work-hours-start">Início do expediente</Label>
          <Input
            id="work-hours-start"
            type="time"
            value={values.workHoursStart}
            onChange={(e) => update({ workHoursStart: e.target.value })}
            disabled={!canConfigure}
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="work-hours-end">Fim do expediente</Label>
          <Input
            id="work-hours-end"
            type="time"
            value={values.workHoursEnd}
            onChange={(e) => update({ workHoursEnd: e.target.value })}
            disabled={!canConfigure}
          />
        </div>
      </div>

      {invalidHours && (
        <p className="text-sm text-destructive">O início do expediente deve ser antes do fim.</p>
      )}

      {canConfigure && (
        <div className="flex justify-end">
          <Button
            type="button"
            disabled={!draft || invalidHours || isSaving}
            onClick={() => {
              onSave({ ...settings, ...values });
              setDraft(null);
            }}
          >
            Salvar
          </Button>
        </div>
      )}
    </section>
  );
}
