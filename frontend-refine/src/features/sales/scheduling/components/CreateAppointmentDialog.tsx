"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
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

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  isLoading: boolean;
  onSubmit: (values: CreateAppointmentFormValues) => void;
  appointmentTypes: AppointmentType[];
  members: { id: string; name: string }[];
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
  defaultStart,
  defaultEnd,
  defaultHostId,
}: Props) {
  const form = useForm<CreateAppointmentFormValues>({
    resolver: zodResolver(createAppointmentSchema),
    defaultValues: {
      title: "",
      hostId: defaultHostId ?? "",
      startAt: defaultStart ?? "",
      endAt: defaultEnd ?? "",
    },
  });

  const handleSubmit = form.handleSubmit((values) => {
    onSubmit(values);
  });

  const selectedTypeId = form.watch("appointmentTypeId");
  const selectedType = appointmentTypes.find((t) => t.id === selectedTypeId);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>Novo Agendamento</DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
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

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="startAt">Início *</Label>
              <Input id="startAt" type="datetime-local" {...form.register("startAt")} />
              {form.formState.errors.startAt && (
                <p className="text-sm text-destructive">{form.formState.errors.startAt.message}</p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="endAt">Fim *</Label>
              <Input id="endAt" type="datetime-local" {...form.register("endAt")} />
              {form.formState.errors.endAt && (
                <p className="text-sm text-destructive">{form.formState.errors.endAt.message}</p>
              )}
            </div>
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

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancelar
            </Button>
            <Button type="submit" disabled={isLoading}>
              {isLoading ? "Criando…" : "Criar agendamento"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
