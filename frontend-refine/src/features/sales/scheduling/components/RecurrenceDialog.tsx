"use client";

import { useEffect, useState } from "react";
import { Minus, Plus } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { RECURRENCE_FREQUENCIES, type RecurrenceValue } from "../schemas/scheduling.schema";

export const FREQUENCY_LABELS: Record<RecurrenceValue["frequency"], string> = {
  DAILY: "Dia",
  WEEKLY: "Semana",
  MONTHLY: "Mês",
  YEARLY: "Ano",
};

const FREQUENCY_PLURALS: Record<RecurrenceValue["frequency"], string> = {
  DAILY: "dias",
  WEEKLY: "semanas",
  MONTHLY: "meses",
  YEARLY: "anos",
};

/** Texto curto da regra, ex.: "A cada 2 semanas, 10 ocorrências". */
export function describeRecurrence(r: RecurrenceValue): string {
  const every =
    r.interval === 1
      ? `A cada ${FREQUENCY_LABELS[r.frequency].toLowerCase()}`
      : `A cada ${r.interval} ${FREQUENCY_PLURALS[r.frequency]}`;
  if (r.endType === "ON_DATE" && r.until) {
    const [y, m, d] = r.until.split("-");
    return `${every}, até ${d}/${m}/${y}`;
  }
  if (r.endType === "AFTER_COUNT" && r.count) return `${every}, ${r.count} ocorrências`;
  return every;
}

const DEFAULT_VALUE: RecurrenceValue = { frequency: "WEEKLY", interval: 1, endType: "NEVER" };

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  initialValue?: RecurrenceValue | null;
  onConfirm: (value: RecurrenceValue) => void;
}

export function RecurrenceDialog({ open, onOpenChange, initialValue, onConfirm }: Props) {
  const [frequency, setFrequency] = useState<RecurrenceValue["frequency"]>("WEEKLY");
  const [interval, setInterval] = useState(1);
  const [endType, setEndType] = useState<RecurrenceValue["endType"]>("NEVER");
  const [until, setUntil] = useState("");
  const [count, setCount] = useState(10);

  useEffect(() => {
    if (!open) return;
    const v = initialValue ?? DEFAULT_VALUE;
    setFrequency(v.frequency);
    setInterval(v.interval);
    setEndType(v.endType);
    setUntil(v.until ?? "");
    setCount(v.count ?? 10);
  }, [open, initialValue]);

  const invalid =
    (endType === "ON_DATE" && !until) ||
    (endType === "AFTER_COUNT" && (!Number.isInteger(count) || count < 2 || count > 366));

  const handleConfirm = () => {
    if (invalid) return;
    onConfirm({
      frequency,
      interval,
      endType,
      ...(endType === "ON_DATE" ? { until } : {}),
      ...(endType === "AFTER_COUNT" ? { count } : {}),
    });
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Configurar recorrência</DialogTitle>
        </DialogHeader>

        <div className="space-y-5">
          <div className="space-y-2">
            <Label>Repetir a cada</Label>
            <div className="flex items-center gap-2">
              <Button
                type="button"
                variant="outline"
                size="icon"
                aria-label="Diminuir intervalo"
                disabled={interval <= 1}
                onClick={() => setInterval((n) => Math.max(1, n - 1))}
              >
                <Minus className="h-4 w-4" />
              </Button>
              <span
                className="w-8 text-center text-sm font-medium"
                data-testid="recurrence-interval"
              >
                {interval}
              </span>
              <Button
                type="button"
                variant="outline"
                size="icon"
                aria-label="Aumentar intervalo"
                disabled={interval >= 99}
                onClick={() => setInterval((n) => Math.min(99, n + 1))}
              >
                <Plus className="h-4 w-4" />
              </Button>
              <Select
                value={frequency}
                onValueChange={(v) => setFrequency(v as RecurrenceValue["frequency"])}
              >
                <SelectTrigger className="w-[130px]" aria-label="Unidade da recorrência">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {RECURRENCE_FREQUENCIES.map((f) => (
                    <SelectItem key={f} value={f}>
                      {FREQUENCY_LABELS[f]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="space-y-2">
            <Label>Termina em</Label>
            <RadioGroup
              value={endType}
              onValueChange={(v) => setEndType(v as RecurrenceValue["endType"])}
              className="space-y-2"
            >
              <label className="flex items-center gap-2 text-sm">
                <RadioGroupItem value="NEVER" aria-label="Nunca" />
                Nunca
              </label>
              <div className="flex items-center gap-2 text-sm">
                <RadioGroupItem value="ON_DATE" aria-label="Em" />
                <span>Em</span>
                <Input
                  type="date"
                  aria-label="Data final da recorrência"
                  className="w-[170px]"
                  value={until}
                  disabled={endType !== "ON_DATE"}
                  onChange={(e) => setUntil(e.target.value)}
                />
              </div>
              <div className="flex items-center gap-2 text-sm">
                <RadioGroupItem value="AFTER_COUNT" aria-label="Após" />
                <span>Após</span>
                <Input
                  type="number"
                  min={2}
                  max={366}
                  aria-label="Número de ocorrências"
                  className="w-[90px]"
                  value={count}
                  disabled={endType !== "AFTER_COUNT"}
                  onChange={(e) => setCount(Number(e.target.value))}
                />
                <span>ocorrências</span>
              </div>
            </RadioGroup>
          </div>
        </div>

        <DialogFooter>
          <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
            Cancelar
          </Button>
          <Button type="button" disabled={invalid} onClick={handleConfirm}>
            Concluir
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
