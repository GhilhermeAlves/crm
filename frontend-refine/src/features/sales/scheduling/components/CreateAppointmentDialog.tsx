"use client";

import { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { format, startOfDay, endOfDay } from "date-fns";
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetFooter } from "@/components/ui/sheet";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  createAppointmentSchema,
  type CreateAppointmentFormValues,
  LOCATION_KINDS,
} from "../schemas/scheduling.schema";
import { LOCATION_KIND_LABELS, type AppointmentType } from "../types/scheduling.types";
import { ContactCombobox } from "./ContactCombobox";
import type { Contact } from "@/features/masterdata/contacts/types/contact.types";

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  isLoading: boolean;
  onSubmit: (values: CreateAppointmentFormValues) => void;
  appointmentTypes: AppointmentType[];
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
  appointmentTypes,
  members,
  contacts,
  defaultStart,
  defaultEnd,
  defaultHostId,
}: Props) {
  const [allDay, setAllDay] = useState(false);

  const form = useForm<CreateAppointmentFormValues>({
    resolver: zodResolver(createAppointmentSchema),
    defaultValues: {
      title: "",
      hostId: defaultHostId ?? "",
      startAt: defaultStart ?? "",
      endAt: defaultEnd ?? "",
    },
  });

  useEffect(() => {
    if (open) {
      form.reset({
        title: "",
        hostId: defaultHostId ?? "",
        startAt: defaultStart ?? "",
        endAt: defaultEnd ?? "",
      });
      setAllDay(false);
    }
  }, [open, defaultStart, defaultEnd, defaultHostId, form]);

  const handleSubmit = form.handleSubmit((values) => {
    if (allDay && values.startAt) {
      const day = new Date(values.startAt);
      values.startAt = format(startOfDay(day), "yyyy-MM-dd'T'HH:mm");
      values.endAt = format(endOfDay(day), "yyyy-MM-dd'T'HH:mm");
    }
    onSubmit(values);
  });

  const selectedTypeId = form.watch("appointmentTypeId");

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="flex flex-col overflow-y-auto sm:max-w-md">
        <SheetHeader>
          <SheetTitle>Novo Agendamento</SheetTitle>
        </SheetHeader>

        <form onSubmit={handleSubmit} className="flex flex-1 flex-col gap-4 pt-4">
          <div className="space-y-2">
            <Label htmlFor="appointmentTypeId">Tipo de agendamento</Label>
            <Select
              value={selectedTypeId ?? ""}
              onValueChange={(v) => {
                form.setValue("appointmentTypeId", v || undefined);
                const type = appointmentTypes.find((t) => t.id === v);
                if (type) {
                  form.setValue("title", type.name);
                  if (type.locationKind) form.setValue("locationKind", type.locationKind);
                }
              }}
            >
              <SelectTrigger id="appointmentTypeId">
                <SelectValue placeholder="Opcional" />
              </SelectTrigger>
              <SelectContent>
                {appointmentTypes.map((t) => (
                  <SelectItem key={t.id} value={t.id}>
                    {t.name} ({t.durationMinutes}min)
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label>Contato</Label>
            <ContactCombobox
              contacts={contacts}
              value={form.watch("contactId")}
              onChange={(id) => form.setValue("contactId", id)}
              placeholder="Selecione ou busque um contato"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="title">Título *</Label>
            <Input id="title" {...form.register("title")} />
            {form.formState.errors.title && (
              <p className="text-sm text-destructive">{form.formState.errors.title.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="hostId">Responsável *</Label>
            <Select value={form.watch("hostId")} onValueChange={(v) => form.setValue("hostId", v)}>
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

          <div className="space-y-3">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="startAt">{allDay ? "Data *" : "Início *"}</Label>
                <Input
                  id="startAt"
                  type={allDay ? "date" : "datetime-local"}
                  {...form.register("startAt")}
                />
                {form.formState.errors.startAt && (
                  <p className="text-sm text-destructive">
                    {form.formState.errors.startAt.message}
                  </p>
                )}
              </div>
              {!allDay && (
                <div className="space-y-2">
                  <Label htmlFor="endAt">Fim *</Label>
                  <Input id="endAt" type="datetime-local" {...form.register("endAt")} />
                  {form.formState.errors.endAt && (
                    <p className="text-sm text-destructive">
                      {form.formState.errors.endAt.message}
                    </p>
                  )}
                </div>
              )}
            </div>
            <label className="flex items-center gap-2 text-sm">
              <Checkbox checked={allDay} onCheckedChange={(v) => setAllDay(!!v)} />
              Dia inteiro
            </label>
          </div>

          <div className="space-y-2">
            <Label htmlFor="locationKind">Local</Label>
            <Select
              value={form.watch("locationKind") ?? ""}
              onValueChange={(v) =>
                form.setValue("locationKind", v as CreateAppointmentFormValues["locationKind"])
              }
            >
              <SelectTrigger id="locationKind">
                <SelectValue placeholder="Opcional" />
              </SelectTrigger>
              <SelectContent>
                {LOCATION_KINDS.map((k) => (
                  <SelectItem key={k} value={k}>
                    {LOCATION_KIND_LABELS[k]}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label htmlFor="notes">Observações</Label>
            <Textarea id="notes" {...form.register("notes")} rows={3} />
          </div>

          <SheetFooter className="mt-auto border-t pt-4">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancelar
            </Button>
            <Button type="submit" disabled={isLoading}>
              {isLoading ? "Criando…" : "Criar agendamento"}
            </Button>
          </SheetFooter>
        </form>
      </SheetContent>
    </Sheet>
  );
}
