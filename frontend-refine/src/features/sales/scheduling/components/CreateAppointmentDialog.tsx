"use client";

import { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { format, startOfDay, endOfDay, setHours, setMinutes } from "date-fns";
import { ptBR } from "date-fns/locale";
import { CalendarIcon } from "lucide-react";
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetFooter } from "@/components/ui/sheet";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Checkbox } from "@/components/ui/checkbox";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Calendar } from "@/components/ui/calendar";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  createAppointmentSchema,
  createBlockSchema,
  type CreateAppointmentFormValues,
  type CreateBlockFormValues,
  INITIAL_STATUSES,
  RECURRENCE_OPTIONS,
} from "../schemas/scheduling.schema";
import { ContactCombobox } from "./ContactCombobox";
import { cn } from "@/lib/utils";
import type { Contact } from "@/features/masterdata/contacts/types/contact.types";

type PanelTab = "consulta" | "bloqueio";

const STATUS_LABELS: Record<(typeof INITIAL_STATUSES)[number], string> = {
  CONFIRMED: "Confirmado",
  SCHEDULED: "Pendente",
  CANCELED: "Cancelado",
};

const RECURRENCE_LABELS: Record<(typeof RECURRENCE_OPTIONS)[number], string> = {
  none: "Não se repete",
  daily: "Todos os dias",
  weekly: "Semanal",
  monthly: "Mensal",
  yearly: "Anual",
};

function generateTimeOptions() {
  const options: string[] = [];
  for (let h = 0; h < 24; h++) {
    for (let m = 0; m < 60; m += 5) {
      options.push(`${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}`);
    }
  }
  return options;
}

const TIME_OPTIONS = generateTimeOptions();

function extractDate(value: string): Date | undefined {
  if (!value) return undefined;
  const d = new Date(value);
  return isNaN(d.getTime()) ? undefined : d;
}

function extractTime(value: string): string {
  if (!value) return "08:00";
  const d = new Date(value);
  if (isNaN(d.getTime())) return "08:00";
  return `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

function buildDateTime(date: Date, time: string): string {
  const [h, m] = time.split(":").map(Number);
  const dt = setMinutes(setHours(date, h), m);
  return format(dt, "yyyy-MM-dd'T'HH:mm");
}

function DatePicker({
  value,
  onChange,
  placeholder = "Selecione a data",
}: {
  value?: Date;
  onChange: (d: Date) => void;
  placeholder?: string;
}) {
  return (
    <Popover>
      <PopoverTrigger asChild>
        <Button
          variant="outline"
          className={cn(
            "w-full justify-start text-left font-normal",
            !value && "text-muted-foreground",
          )}
        >
          <CalendarIcon className="mr-2 h-4 w-4" />
          {value ? format(value, "dd/MM/yyyy", { locale: ptBR }) : placeholder}
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-auto p-0" align="start">
        <Calendar mode="single" selected={value} onSelect={(d) => d && onChange(d)} autoFocus />
      </PopoverContent>
    </Popover>
  );
}

function TimeSelect({ value, onChange }: { value: string; onChange: (v: string) => void }) {
  return (
    <Select value={value} onValueChange={onChange}>
      <SelectTrigger className="w-[90px]">
        <SelectValue />
      </SelectTrigger>
      <SelectContent className="max-h-60">
        {TIME_OPTIONS.map((t) => (
          <SelectItem key={t} value={t}>
            {t}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  isLoading: boolean;
  onSubmit: (values: CreateAppointmentFormValues) => void;
  onSubmitBlock?: (values: CreateBlockFormValues) => void;
  isLoadingBlock?: boolean;
  appointmentTypes: { id: string; name: string; durationMinutes: number }[];
  members: { id: string; name: string }[];
  contacts: Contact[];
  defaultStart?: string;
  defaultEnd?: string;
  defaultHostId?: string;
}

export function CreateAppointmentDialog({
  open,
  onOpenChange,
  isLoading,
  onSubmit,
  onSubmitBlock,
  isLoadingBlock,
  members,
  contacts,
  defaultStart,
  defaultEnd,
  defaultHostId,
}: Props) {
  const [tab, setTab] = useState<PanelTab>("consulta");
  const [allDay, setAllDay] = useState(false);

  const [date, setDate] = useState<Date | undefined>(undefined);
  const [timeFrom, setTimeFrom] = useState("08:00");
  const [timeTo, setTimeTo] = useState("09:00");

  const [blockDate, setBlockDate] = useState<Date | undefined>(undefined);
  const [blockTimeFrom, setBlockTimeFrom] = useState("08:00");
  const [blockTimeTo, setBlockTimeTo] = useState("09:00");

  const form = useForm<CreateAppointmentFormValues>({
    resolver: zodResolver(createAppointmentSchema),
    defaultValues: {
      title: "",
      hostId: defaultHostId ?? "",
      startAt: defaultStart ?? "",
      endAt: defaultEnd ?? "",
      status: "CONFIRMED",
      recurrence: "none",
    },
  });

  const blockForm = useForm<CreateBlockFormValues>({
    resolver: zodResolver(createBlockSchema),
    defaultValues: {
      hostId: defaultHostId ?? "",
      startAt: defaultStart ?? "",
      endAt: defaultEnd ?? "",
      reason: "",
    },
  });

  useEffect(() => {
    if (open) {
      const startDate = extractDate(defaultStart ?? "");
      const startTime = extractTime(defaultStart ?? "");
      const endTime = extractTime(defaultEnd ?? "");

      setDate(startDate);
      setTimeFrom(startTime);
      setTimeTo(endTime);
      setBlockDate(startDate);
      setBlockTimeFrom(startTime);
      setBlockTimeTo(endTime);

      form.reset({
        title: "",
        hostId: defaultHostId ?? "",
        startAt: defaultStart ?? "",
        endAt: defaultEnd ?? "",
        status: "CONFIRMED",
        recurrence: "none",
      });
      blockForm.reset({
        hostId: defaultHostId ?? "",
        startAt: defaultStart ?? "",
        endAt: defaultEnd ?? "",
        reason: "",
      });
      setAllDay(false);
      setTab("consulta");
    }
  }, [open, defaultStart, defaultEnd, defaultHostId, form, blockForm]);

  useEffect(() => {
    if (date) {
      form.setValue("startAt", buildDateTime(date, timeFrom));
      form.setValue("endAt", buildDateTime(date, timeTo));
    }
  }, [date, timeFrom, timeTo, form]);

  useEffect(() => {
    if (blockDate) {
      blockForm.setValue("startAt", buildDateTime(blockDate, blockTimeFrom));
      blockForm.setValue("endAt", buildDateTime(blockDate, blockTimeTo));
    }
  }, [blockDate, blockTimeFrom, blockTimeTo, blockForm]);

  const handleSubmit = form.handleSubmit((values) => {
    if (allDay && date) {
      values.startAt = format(startOfDay(date), "yyyy-MM-dd'T'HH:mm");
      values.endAt = format(endOfDay(date), "yyyy-MM-dd'T'HH:mm");
    }
    onSubmit(values);
  });

  const handleBlockSubmit = blockForm.handleSubmit((values) => {
    onSubmitBlock?.(values);
  });

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="flex flex-col overflow-y-auto sm:max-w-lg">
        <SheetHeader>
          <SheetTitle>Novo agendamento</SheetTitle>
        </SheetHeader>

        {/* Tabs: Consulta / Bloqueio de Agenda */}
        <div className="flex rounded-md border">
          <button
            type="button"
            className={cn(
              "flex-1 px-3 py-1.5 text-sm font-medium transition-colors",
              tab === "consulta"
                ? "bg-primary text-primary-foreground"
                : "text-muted-foreground hover:text-foreground",
            )}
            onClick={() => setTab("consulta")}
          >
            Consulta
          </button>
          <button
            type="button"
            className={cn(
              "flex-1 px-3 py-1.5 text-sm font-medium transition-colors",
              tab === "bloqueio"
                ? "bg-primary text-primary-foreground"
                : "text-muted-foreground hover:text-foreground",
            )}
            onClick={() => setTab("bloqueio")}
          >
            Bloqueio de Agenda
          </button>
        </div>

        {tab === "consulta" ? (
          <form onSubmit={handleSubmit} className="flex flex-1 flex-col gap-4 pt-2">
            {/* Paciente */}
            <div className="space-y-2">
              <Label>Paciente</Label>
              <ContactCombobox
                contacts={contacts}
                value={form.watch("contactId")}
                onChange={(id) => form.setValue("contactId", id)}
                placeholder="Selecione ou crie um novo paciente"
              />
            </div>

            {/* Status */}
            <div className="space-y-2">
              <Label>Status</Label>
              <RadioGroup
                value={form.watch("status") ?? "CONFIRMED"}
                onValueChange={(v) =>
                  form.setValue("status", v as CreateAppointmentFormValues["status"])
                }
                className="flex gap-4"
              >
                {INITIAL_STATUSES.map((s) => (
                  <label key={s} className="flex items-center gap-1.5 text-sm">
                    <RadioGroupItem value={s} />
                    {STATUS_LABELS[s]}
                  </label>
                ))}
              </RadioGroup>
            </div>

            {/* Título */}
            <div className="space-y-2">
              <Label htmlFor="title">Título</Label>
              <Input
                id="title"
                placeholder="Digite o título da consulta"
                {...form.register("title")}
              />
              {form.formState.errors.title && (
                <p className="text-sm text-destructive">{form.formState.errors.title.message}</p>
              )}
            </div>

            {/* Responsável */}
            <div className="space-y-2">
              <Label htmlFor="hostId">Responsável</Label>
              <Select
                value={form.watch("hostId")}
                onValueChange={(v) => form.setValue("hostId", v)}
              >
                <SelectTrigger id="hostId">
                  <SelectValue placeholder="Selecione" />
                </SelectTrigger>
                <SelectContent>
                  {members.map((m) => (
                    <SelectItem key={m.id} value={m.id}>
                      {m.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {form.formState.errors.hostId && (
                <p className="text-sm text-destructive">{form.formState.errors.hostId.message}</p>
              )}
            </div>

            {/* Data */}
            <div className="space-y-2">
              <Label>Data</Label>
              <DatePicker value={date} onChange={setDate} />
            </div>

            {/* De / Até / Dia inteiro */}
            {!allDay && (
              <div className="flex items-end gap-4">
                <div className="space-y-2">
                  <Label>De</Label>
                  <TimeSelect value={timeFrom} onChange={setTimeFrom} />
                </div>
                <div className="space-y-2">
                  <Label>Até</Label>
                  <TimeSelect value={timeTo} onChange={setTimeTo} />
                </div>
                <label className="flex items-center gap-2 pb-2 text-sm">
                  <Checkbox checked={allDay} onCheckedChange={(v) => setAllDay(!!v)} />
                  Dia inteiro
                </label>
              </div>
            )}
            {allDay && (
              <label className="flex items-center gap-2 text-sm">
                <Checkbox checked={allDay} onCheckedChange={(v) => setAllDay(!!v)} />
                Dia inteiro
              </label>
            )}

            {/* Repetir agendamento */}
            <div className="space-y-2">
              <Label htmlFor="recurrence">Repetir agendamento</Label>
              <Select
                value={form.watch("recurrence") ?? "none"}
                onValueChange={(v) =>
                  form.setValue("recurrence", v as CreateAppointmentFormValues["recurrence"])
                }
              >
                <SelectTrigger id="recurrence">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {RECURRENCE_OPTIONS.map((opt) => (
                    <SelectItem key={opt} value={opt}>
                      {RECURRENCE_LABELS[opt]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Observações */}
            <div className="space-y-2">
              <Label htmlFor="notes">
                Observações <span className="text-muted-foreground">Opcional</span>
              </Label>
              <Textarea
                id="notes"
                placeholder="Digite aqui uma observação"
                {...form.register("notes")}
                rows={3}
              />
            </div>

            <SheetFooter className="mt-auto border-t pt-4">
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                Cancelar
              </Button>
              <Button type="submit" disabled={isLoading}>
                {isLoading ? "Criando…" : "Confirmar"}
              </Button>
            </SheetFooter>
          </form>
        ) : (
          /* ── Bloqueio de Agenda tab ── */
          <form onSubmit={handleBlockSubmit} className="flex flex-1 flex-col gap-4 pt-2">
            {/* Título do bloqueio */}
            <div className="space-y-2">
              <Label htmlFor="block-title">Título</Label>
              <Input id="block-title" placeholder="Digite o título do bloqueio de agenda" />
            </div>

            {/* Responsável */}
            <div className="space-y-2">
              <Label htmlFor="block-hostId">Responsável</Label>
              <Select
                value={blockForm.watch("hostId")}
                onValueChange={(v) => blockForm.setValue("hostId", v)}
              >
                <SelectTrigger id="block-hostId">
                  <SelectValue placeholder="Selecione" />
                </SelectTrigger>
                <SelectContent>
                  {members.map((m) => (
                    <SelectItem key={m.id} value={m.id}>
                      {m.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {blockForm.formState.errors.hostId && (
                <p className="text-sm text-destructive">
                  {blockForm.formState.errors.hostId.message}
                </p>
              )}
            </div>

            {/* Data */}
            <div className="space-y-2">
              <Label>Data</Label>
              <DatePicker value={blockDate} onChange={setBlockDate} />
            </div>

            {/* De / Até / Dia inteiro */}
            <div className="flex items-end gap-4">
              <div className="space-y-2">
                <Label>De</Label>
                <TimeSelect value={blockTimeFrom} onChange={setBlockTimeFrom} />
              </div>
              <div className="space-y-2">
                <Label>Até</Label>
                <TimeSelect value={blockTimeTo} onChange={setBlockTimeTo} />
              </div>
              <label className="flex items-center gap-2 pb-2 text-sm">
                <Checkbox checked={false} disabled />
                Dia inteiro
              </label>
            </div>

            {/* Repetir bloqueio */}
            <div className="space-y-2">
              <Label>Repetir bloqueio</Label>
              <Select defaultValue="none">
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {RECURRENCE_OPTIONS.map((opt) => (
                    <SelectItem key={opt} value={opt}>
                      {RECURRENCE_LABELS[opt]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Observações */}
            <div className="space-y-2">
              <Label>
                Observações <span className="text-muted-foreground">Opcional</span>
              </Label>
              <Textarea
                placeholder="Digite aqui uma observação"
                {...blockForm.register("reason")}
                rows={3}
              />
            </div>

            <SheetFooter className="mt-auto border-t pt-4">
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                Cancelar
              </Button>
              <Button type="submit" disabled={isLoadingBlock ?? false}>
                {isLoadingBlock ? "Salvando…" : "Confirmar"}
              </Button>
            </SheetFooter>
          </form>
        )}
      </SheetContent>
    </Sheet>
  );
}
