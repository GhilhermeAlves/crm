"use client";

import { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { format, startOfDay, endOfDay, setHours, setMinutes } from "date-fns";
import { ptBR } from "date-fns/locale";
import { AlertTriangle, CalendarIcon, ExternalLink, Plus } from "lucide-react";
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
  RECURRENCE_PRESETS,
  isValidBrazilianPhone,
  presetFromRecurrence,
  recurrenceFromPreset,
  type RecurrencePreset,
  type RecurrenceValue,
} from "../schemas/scheduling.schema";
import { RegistrationLinkActions } from "@/features/masterdata/contacts/registration/components/RegistrationLinkActions";
import { ContactCombobox } from "./ContactCombobox";
import { RecurrenceDialog, describeRecurrence } from "./RecurrenceDialog";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { useCatalog } from "@/features/masterdata/catalog/hooks/useCatalog";
import { useCreateTag } from "@/features/sales/tag/hooks/useTags";
import { ROUTES } from "@/lib/constants";
import { cn } from "@/lib/utils";
import type { Contact } from "@/features/masterdata/contacts/types/contact.types";

type PanelTab = "consulta" | "bloqueio";

const STATUS_LABELS: Record<(typeof INITIAL_STATUSES)[number], string> = {
  CONFIRMED: "Confirmado",
  SCHEDULED: "Pendente",
  CANCELED: "Cancelado",
};

const RECURRENCE_LABELS: Record<RecurrencePreset, string> = {
  none: "Não se repete",
  daily: "Todos os dias",
  weekly: "Semanal",
  monthly: "Mensal",
  yearly: "Anual",
  custom: "Personalizado…",
};

function contactName(c?: Contact): string {
  if (!c) return "";
  return [c.firstName, c.lastName].filter(Boolean).join(" ").trim();
}

function contactMobile(c?: Contact): string {
  return c?.mobile ?? c?.phone ?? "";
}

/** Seletor "Repetir": presets + "Personalizado" (abre o diálogo "Configurar recorrência"). */
function RecurrenceField({
  id,
  label,
  value,
  onChange,
}: {
  id: string;
  label: string;
  value: RecurrenceValue | null | undefined;
  onChange: (v: RecurrenceValue | null) => void;
}) {
  const [customOpen, setCustomOpen] = useState(false);
  const preset = presetFromRecurrence(value);
  return (
    <div className="space-y-2">
      <Label htmlFor={id}>{label}</Label>
      <Select
        value={preset}
        onValueChange={(v) => {
          if (v === "custom") setCustomOpen(true);
          else onChange(recurrenceFromPreset(v as Exclude<RecurrencePreset, "custom">));
        }}
      >
        <SelectTrigger id={id} aria-label={label}>
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          {RECURRENCE_PRESETS.map((opt) => (
            <SelectItem key={opt} value={opt}>
              {RECURRENCE_LABELS[opt]}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
      {preset === "custom" && value && (
        <button
          type="button"
          className="text-xs text-muted-foreground underline-offset-2 hover:underline"
          onClick={() => setCustomOpen(true)}
        >
          {describeRecurrence(value)}
        </button>
      )}
      <RecurrenceDialog
        open={customOpen}
        onOpenChange={setCustomOpen}
        initialValue={value}
        onConfirm={onChange}
      />
    </div>
  );
}

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
  defaultContactId?: string;
  /** Horário de trabalho no formato "HH:mm", para avisar quando a consulta fica fora dele. */
  workHoursStart?: string;
  workHoursEnd?: string;
  /** Cadeiras ativas e marcadores da empresa (Fase C). Sem itens, o campo não aparece. */
  chairs?: { id: string; name: string; color: string | null }[];
  tags?: { id: string; name: string; color: string | null }[];
}

const NO_CHAIR = "__none__";

/** Strings "HH:mm" comparam bem em ordem lexicográfica. */
export function isOutsideWorkHours(
  from: string,
  to: string,
  workStart?: string,
  workEnd?: string,
): boolean {
  if (!workStart || !workEnd) return false;
  return from < workStart || to > workEnd;
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
  defaultContactId,
  workHoursStart,
  workHoursEnd,
  chairs = [],
  tags = [],
}: Props) {
  const [tab, setTab] = useState<PanelTab>("consulta");
  const [allDay, setAllDay] = useState(false);

  const [date, setDate] = useState<Date | undefined>(undefined);
  const [timeFrom, setTimeFrom] = useState("08:00");
  const [timeTo, setTimeTo] = useState("09:00");

  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const { data: catalogPage } = useCatalog(companyId, {
    type: "SERVICE",
    active: true,
    pageSize: 200,
  });
  const procedures = catalogPage?.content ?? [];
  const createTag = useCreateTag(companyId);

  const [phone, setPhone] = useState("");
  const [initialPhone, setInitialPhone] = useState("");
  const [phoneError, setPhoneError] = useState<string | null>(null);
  const [newTagOpen, setNewTagOpen] = useState(false);
  const [newTagName, setNewTagName] = useState("");
  const [newTagColor, setNewTagColor] = useState("#3b82f6");

  const [blockDate, setBlockDate] = useState<Date | undefined>(undefined);
  const [blockTimeFrom, setBlockTimeFrom] = useState("08:00");
  const [blockTimeTo, setBlockTimeTo] = useState("09:00");

  const form = useForm<CreateAppointmentFormValues>({
    resolver: zodResolver(createAppointmentSchema),
    defaultValues: {
      title: "",
      hostId: defaultHostId ?? "",
      contactId: defaultContactId,
      startAt: defaultStart ?? "",
      endAt: defaultEnd ?? "",
      status: "SCHEDULED",
      recurrence: null,
      tagIds: [],
      catalogItemIds: [],
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
        hostId: defaultHostId ?? "",
        contactId: defaultContactId,
        startAt: defaultStart ?? "",
        endAt: defaultEnd ?? "",
        title: contactName(contacts.find((c) => c.id === defaultContactId)),
        status: "SCHEDULED",
        recurrence: null,
        tagIds: [],
        catalogItemIds: [],
      });
      blockForm.reset({
        hostId: defaultHostId ?? "",
        startAt: defaultStart ?? "",
        endAt: defaultEnd ?? "",
        reason: "",
        recurrence: null,
      });
      const initial = contactMobile(contacts.find((c) => c.id === defaultContactId));
      setInitialPhone(initial);
      setPhone(initial);
      setPhoneError(null);
      setNewTagOpen(false);
      setAllDay(false);
      setTab("consulta");
    }
    // contacts fica fora: recarregar a lista não deve apagar o que já foi digitado.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, defaultStart, defaultEnd, defaultHostId, defaultContactId, form, blockForm]);

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

  const handleContactChange = (id: string | undefined) => {
    form.setValue("contactId", id);
    const contact = contacts.find((c) => c.id === id);
    if (contact && !form.getValues("title")?.trim()) {
      form.setValue("title", contactName(contact), { shouldValidate: true });
    }
    const initial = contactMobile(contact);
    setInitialPhone(initial);
    setPhone(initial);
    setPhoneError(null);
  };

  const handleCreateTag = () => {
    const name = newTagName.trim();
    if (!name) return;
    createTag.mutate(
      { name, color: newTagColor },
      {
        onSuccess: (tag) => {
          form.setValue("tagIds", [...(form.getValues("tagIds") ?? []), tag.id]);
          setNewTagName("");
          setNewTagOpen(false);
        },
      },
    );
  };

  const handleSubmit = form.handleSubmit((values) => {
    const changedPhone = phone.trim() !== initialPhone.trim() ? phone.trim() : "";
    if (values.contactId && changedPhone && !isValidBrazilianPhone(changedPhone)) {
      setPhoneError("Celular inválido (ex: (11) 99999-9999)");
      return;
    }
    values.contactPhone = values.contactId && changedPhone ? changedPhone : undefined;
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

        {/* Tabs: Consulta / Bloqueio de Agenda (bloqueio só quando há onSubmitBlock, ex.: Agenda) */}
        {onSubmitBlock && (
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
        )}

        {tab === "consulta" || !onSubmitBlock ? (
          <form onSubmit={handleSubmit} className="flex flex-1 flex-col gap-4 pt-2">
            {/* Paciente */}
            <div className="space-y-2">
              <Label>Paciente</Label>
              <ContactCombobox
                contacts={contacts}
                value={form.watch("contactId")}
                onChange={handleContactChange}
                placeholder="Selecione ou crie um novo paciente"
              />
            </div>

            {/* Celular do(a) paciente: se mudar, o pai atualiza o contato antes de criar */}
            {form.watch("contactId") && (
              <div className="space-y-2">
                <Label htmlFor="contactPhone">
                  Celular do(a) paciente <span className="text-muted-foreground">(opcional)</span>
                </Label>
                <Input
                  id="contactPhone"
                  inputMode="tel"
                  placeholder="(11) 99999-9999"
                  value={phone}
                  onChange={(e) => {
                    setPhone(e.target.value);
                    setPhoneError(null);
                  }}
                />
                {phoneError && <p className="text-sm text-destructive">{phoneError}</p>}
                <RegistrationLinkActions
                  companyId={companyId}
                  contactId={form.watch("contactId") as string}
                  firstName={
                    contacts.find((c) => c.id === form.watch("contactId"))?.firstName ?? ""
                  }
                  phone={phone}
                />
              </div>
            )}

            {/* Status */}
            <div className="space-y-2">
              <Label>Status</Label>
              <RadioGroup
                value={form.watch("status") ?? "SCHEDULED"}
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

            {/* Cadeira */}
            {chairs.length > 0 && (
              <div className="space-y-2">
                <Label htmlFor="chairId">Cadeira</Label>
                <Select
                  value={form.watch("chairId") ?? NO_CHAIR}
                  onValueChange={(v) => form.setValue("chairId", v === NO_CHAIR ? undefined : v)}
                >
                  <SelectTrigger id="chairId">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value={NO_CHAIR}>Sem cadeira</SelectItem>
                    {chairs.map((c) => (
                      <SelectItem key={c.id} value={c.id}>
                        {c.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            )}

            {/* Marcadores */}
            <div className="space-y-2">
              <Label>Marcadores</Label>
              <div className="flex flex-wrap gap-1.5" role="group" aria-label="Marcadores">
                {tags.map((t) => {
                  const selected = (form.watch("tagIds") ?? []).includes(t.id);
                  return (
                    <button
                      key={t.id}
                      type="button"
                      aria-pressed={selected}
                      onClick={() => {
                        const current = form.getValues("tagIds") ?? [];
                        form.setValue(
                          "tagIds",
                          selected ? current.filter((id) => id !== t.id) : [...current, t.id],
                        );
                      }}
                      className={cn(
                        "flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-xs transition-colors",
                        selected
                          ? "border-primary bg-primary/10 font-medium"
                          : "text-muted-foreground hover:bg-muted",
                      )}
                    >
                      <span
                        className="h-2 w-2 rounded-full"
                        style={{ backgroundColor: t.color ?? "currentColor" }}
                      />
                      {t.name}
                    </button>
                  );
                })}
                <button
                  type="button"
                  onClick={() => setNewTagOpen((v) => !v)}
                  className="flex items-center gap-1 rounded-full border border-dashed px-2.5 py-1 text-xs text-muted-foreground transition-colors hover:bg-muted"
                >
                  <Plus className="h-3 w-3" />
                  Novo marcador
                </button>
              </div>
              {newTagOpen && (
                <div className="flex items-center gap-2">
                  <Input
                    aria-label="Nome do marcador"
                    placeholder="Nome do marcador"
                    value={newTagName}
                    onChange={(e) => setNewTagName(e.target.value)}
                  />
                  <input
                    type="color"
                    aria-label="Cor do marcador"
                    value={newTagColor}
                    onChange={(e) => setNewTagColor(e.target.value)}
                    className="h-9 w-10 shrink-0 cursor-pointer rounded-md border bg-background"
                  />
                  <Button
                    type="button"
                    size="sm"
                    disabled={!newTagName.trim() || createTag.isPending}
                    onClick={handleCreateTag}
                  >
                    Criar
                  </Button>
                </div>
              )}
            </div>

            {/* Procedimentos (serviços ativos do catálogo) */}
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <Label>Procedimentos</Label>
                <a
                  href={ROUTES.CATALOG}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline"
                >
                  <ExternalLink className="h-3 w-3" />
                  Adicionar procedimentos
                </a>
              </div>
              {procedures.length === 0 ? (
                <p className="text-xs text-muted-foreground">Nenhum serviço ativo no catálogo.</p>
              ) : (
                <div className="flex flex-wrap gap-1.5" role="group" aria-label="Procedimentos">
                  {procedures.map((p) => {
                    const selected = (form.watch("catalogItemIds") ?? []).includes(p.id);
                    return (
                      <button
                        key={p.id}
                        type="button"
                        aria-pressed={selected}
                        onClick={() => {
                          const current = form.getValues("catalogItemIds") ?? [];
                          form.setValue(
                            "catalogItemIds",
                            selected ? current.filter((id) => id !== p.id) : [...current, p.id],
                          );
                        }}
                        className={cn(
                          "rounded-full border px-2.5 py-1 text-xs transition-colors",
                          selected
                            ? "border-primary bg-primary/10 font-medium"
                            : "text-muted-foreground hover:bg-muted",
                        )}
                      >
                        {p.name}
                      </button>
                    );
                  })}
                </div>
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
            {!allDay && isOutsideWorkHours(timeFrom, timeTo, workHoursStart, workHoursEnd) && (
              <p
                role="status"
                className="flex items-center gap-2 rounded-md border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-amber-800 dark:border-amber-700 dark:bg-amber-950 dark:text-amber-200"
              >
                <AlertTriangle className="h-4 w-4 shrink-0" />
                Evento fora da faixa de horário de trabalho ({workHoursStart} – {workHoursEnd}).
              </p>
            )}
            {allDay && (
              <label className="flex items-center gap-2 text-sm">
                <Checkbox checked={allDay} onCheckedChange={(v) => setAllDay(!!v)} />
                Dia inteiro
              </label>
            )}

            {/* Repetir agendamento */}
            <RecurrenceField
              id="recurrence"
              label="Repetir agendamento"
              value={form.watch("recurrence")}
              onChange={(v) => form.setValue("recurrence", v)}
            />

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
            <RecurrenceField
              id="block-recurrence"
              label="Repetir bloqueio"
              value={blockForm.watch("recurrence")}
              onChange={(v) => blockForm.setValue("recurrence", v)}
            />

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
