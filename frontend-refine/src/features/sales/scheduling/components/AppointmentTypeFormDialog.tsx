"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetFooter,
} from "@/components/ui/sheet";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Switch } from "@/components/ui/switch";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  createAppointmentTypeSchema,
  type CreateAppointmentTypeFormValues,
  LOCATION_KINDS,
  ASSIGNMENT_MODES,
} from "../schemas/scheduling.schema";
import {
  LOCATION_KIND_LABELS,
  type AppointmentType,
} from "../types/scheduling.types";

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  isLoading: boolean;
  onSubmit: (values: CreateAppointmentTypeFormValues) => void;
  members: { id: string; name: string }[];
  editingType?: AppointmentType | null;
}

const ASSIGNMENT_MODE_LABELS: Record<string, string> = {
  ROUND_ROBIN: "Rodízio automático",
  CHOOSE_HOST: "Cliente escolhe",
};

export function AppointmentTypeFormDialog({
  open,
  onOpenChange,
  isLoading,
  onSubmit,
  members,
  editingType,
}: Props) {
  const form = useForm<CreateAppointmentTypeFormValues>({
    resolver: zodResolver(createAppointmentTypeSchema),
    defaultValues: {
      name: "",
      durationMinutes: 30,
      bufferBeforeMinutes: 0,
      bufferAfterMinutes: 0,
      minNoticeHours: 1,
      maxDaysAhead: 60,
      slotIntervalMinutes: 30,
      publicBookingEnabled: false,
      hostIds: [],
    },
  });

  useEffect(() => {
    if (editingType) {
      form.reset({
        name: editingType.name,
        slug: editingType.slug,
        description: editingType.description ?? undefined,
        durationMinutes: editingType.durationMinutes,
        bufferBeforeMinutes: editingType.bufferBeforeMinutes,
        bufferAfterMinutes: editingType.bufferAfterMinutes,
        minNoticeHours: editingType.minNoticeHours,
        maxDaysAhead: editingType.maxDaysAhead,
        slotIntervalMinutes: editingType.slotIntervalMinutes,
        color: editingType.color ?? undefined,
        locationKind: editingType.locationKind ?? undefined,
        locationDetail: editingType.locationDetail ?? undefined,
        assignmentMode: editingType.assignmentMode ?? undefined,
        publicBookingEnabled: editingType.publicBookingEnabled,
        hostIds: editingType.hostIds,
      });
    } else {
      form.reset({
        name: "",
        durationMinutes: 30,
        bufferBeforeMinutes: 0,
        bufferAfterMinutes: 0,
        minNoticeHours: 1,
        maxDaysAhead: 60,
        slotIntervalMinutes: 30,
        publicBookingEnabled: false,
        hostIds: [],
      });
    }
  }, [editingType, form]);

  const selectedHostIds = form.watch("hostIds") ?? [];

  const toggleHost = (id: string) => {
    const current = form.getValues("hostIds") ?? [];
    form.setValue(
      "hostIds",
      current.includes(id)
        ? current.filter((h) => h !== id)
        : [...current, id],
      { shouldValidate: true },
    );
  };

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent
        side="right"
        className="flex flex-col overflow-y-auto sm:max-w-lg"
      >
        <SheetHeader>
          <SheetTitle>
            {editingType
              ? "Editar tipo de agendamento"
              : "Novo tipo de agendamento"}
          </SheetTitle>
        </SheetHeader>

        <form
          onSubmit={form.handleSubmit(onSubmit)}
          className="flex flex-1 flex-col gap-4 pt-2"
        >
          <div className="space-y-2">
            <Label htmlFor="type-name">Nome *</Label>
            <Input
              id="type-name"
              {...form.register("name")}
              placeholder="Ex.: Reunião 30 min"
            />
            {form.formState.errors.name && (
              <p className="text-sm text-destructive">
                {form.formState.errors.name.message}
              </p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="type-description">Descrição</Label>
            <Textarea
              id="type-description"
              {...form.register("description")}
              rows={2}
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="type-duration">Duração (min) *</Label>
              <Input
                id="type-duration"
                type="number"
                min={5}
                max={480}
                {...form.register("durationMinutes")}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="type-interval">Intervalo de slots (min)</Label>
              <Input
                id="type-interval"
                type="number"
                min={5}
                max={60}
                {...form.register("slotIntervalMinutes")}
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="type-buffer-before">Buffer antes (min)</Label>
              <Input
                id="type-buffer-before"
                type="number"
                min={0}
                max={120}
                {...form.register("bufferBeforeMinutes")}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="type-buffer-after">Buffer depois (min)</Label>
              <Input
                id="type-buffer-after"
                type="number"
                min={0}
                max={120}
                {...form.register("bufferAfterMinutes")}
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="type-notice">Antecedência mín. (h)</Label>
              <Input
                id="type-notice"
                type="number"
                min={0}
                max={168}
                {...form.register("minNoticeHours")}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="type-max-days">Máx. dias à frente</Label>
              <Input
                id="type-max-days"
                type="number"
                min={1}
                max={365}
                {...form.register("maxDaysAhead")}
              />
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="type-location">Local</Label>
            <Select
              value={form.watch("locationKind") ?? ""}
              onValueChange={(v) =>
                form.setValue(
                  "locationKind",
                  v as CreateAppointmentTypeFormValues["locationKind"],
                )
              }
            >
              <SelectTrigger id="type-location">
                <SelectValue placeholder="Nenhum" />
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
            <Label htmlFor="type-assignment">Distribuição</Label>
            <Select
              value={form.watch("assignmentMode") ?? ""}
              onValueChange={(v) =>
                form.setValue(
                  "assignmentMode",
                  v as CreateAppointmentTypeFormValues["assignmentMode"],
                )
              }
            >
              <SelectTrigger id="type-assignment">
                <SelectValue placeholder="Rodízio" />
              </SelectTrigger>
              <SelectContent>
                {ASSIGNMENT_MODES.map((k) => (
                  <SelectItem key={k} value={k}>
                    {ASSIGNMENT_MODE_LABELS[k]}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label htmlFor="type-color">Cor</Label>
            <Input
              id="type-color"
              type="color"
              {...form.register("color")}
              className="h-9 w-16"
            />
          </div>

          <div className="space-y-2">
            <Label>Responsáveis *</Label>
            <div className="flex flex-wrap gap-2">
              {members.map((m) => (
                <Button
                  key={m.id}
                  type="button"
                  variant={
                    selectedHostIds.includes(m.id) ? "default" : "outline"
                  }
                  size="sm"
                  onClick={() => toggleHost(m.id)}
                >
                  {m.name}
                </Button>
              ))}
            </div>
            {form.formState.errors.hostIds && (
              <p className="text-sm text-destructive">
                {form.formState.errors.hostIds.message}
              </p>
            )}
          </div>

          <div className="flex items-center gap-3">
            <Switch
              id="type-public"
              checked={form.watch("publicBookingEnabled") ?? false}
              onCheckedChange={(v) => form.setValue("publicBookingEnabled", v)}
            />
            <Label htmlFor="type-public">
              Agendamento público (link externo)
            </Label>
          </div>

          <SheetFooter className="mt-auto border-t pt-4">
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
            >
              Cancelar
            </Button>
            <Button type="submit" disabled={isLoading}>
              {isLoading ? "Salvando…" : editingType ? "Salvar" : "Criar"}
            </Button>
          </SheetFooter>
        </form>
      </SheetContent>
    </Sheet>
  );
}
