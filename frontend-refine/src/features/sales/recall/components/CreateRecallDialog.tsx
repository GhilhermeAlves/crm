"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
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
import { Sheet, SheetContent, SheetFooter, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { ContactCombobox } from "@/features/sales/scheduling/components/ContactCombobox";
import type { Contact } from "@/features/masterdata/contacts/types/contact.types";
import { createRecallSchema, type CreateRecallFormValues } from "../schemas/recall.schema";
import { RECALL_DUE_UNIT_LABELS, type RecallDueUnit } from "../types/recall.types";

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  contacts: Contact[];
  members: { id: string; name: string }[];
  isLoading: boolean;
  onSubmit: (values: CreateRecallFormValues) => void;
  tags?: { id: string; name: string }[];
}

const NO_TAG = "__none__";

const DUE_UNITS = Object.keys(RECALL_DUE_UNIT_LABELS) as RecallDueUnit[];

export function CreateRecallDialog({
  open,
  onOpenChange,
  contacts,
  members,
  isLoading,
  onSubmit,
  tags = [],
}: Props) {
  const form = useForm<CreateRecallFormValues>({
    resolver: zodResolver(createRecallSchema),
    defaultValues: {
      contactId: "",
      phone: "",
      hostId: "",
      tagId: undefined,
      dueInAmount: 6,
      dueInUnit: "MONTHS",
    },
  });

  useEffect(() => {
    if (open) {
      form.reset({
        contactId: "",
        phone: "",
        hostId: "",
        tagId: undefined,
        dueInAmount: 6,
        dueInUnit: "MONTHS",
      });
    }
  }, [open, form]);

  const handleContactChange = (contactId: string | undefined) => {
    form.setValue("contactId", contactId ?? "", { shouldValidate: true });
    const contact = contacts.find((c) => c.id === contactId);
    if (contact) {
      form.setValue("phone", contact.mobile ?? contact.phone ?? "", { shouldValidate: true });
    }
  };

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="flex flex-col overflow-y-auto sm:max-w-lg">
        <SheetHeader>
          <SheetTitle>Criar retorno</SheetTitle>
        </SheetHeader>
        <form onSubmit={form.handleSubmit(onSubmit)} className="flex flex-1 flex-col gap-4 pt-2">
          <div className="space-y-2">
            <Label>Paciente *</Label>
            <ContactCombobox
              contacts={contacts}
              value={form.watch("contactId") || undefined}
              onChange={handleContactChange}
              placeholder="Selecione o paciente"
            />
            {form.formState.errors.contactId && (
              <p className="text-sm text-destructive">{form.formState.errors.contactId.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="recall-phone">Celular *</Label>
            <Input id="recall-phone" placeholder="(11) 99999-9999" {...form.register("phone")} />
            {form.formState.errors.phone && (
              <p className="text-sm text-destructive">{form.formState.errors.phone.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label>Dentista *</Label>
            <Select
              value={form.watch("hostId")}
              onValueChange={(value) => form.setValue("hostId", value, { shouldValidate: true })}
            >
              <SelectTrigger>
                <SelectValue placeholder="Selecione o dentista" />
              </SelectTrigger>
              <SelectContent>
                {members.map((member) => (
                  <SelectItem key={member.id} value={member.id}>
                    {member.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            {form.formState.errors.hostId && (
              <p className="text-sm text-destructive">{form.formState.errors.hostId.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="recall-tag">Marcador</Label>
            <Select
              value={form.watch("tagId") ?? NO_TAG}
              onValueChange={(value) =>
                form.setValue("tagId", value === NO_TAG ? undefined : value)
              }
            >
              <SelectTrigger id="recall-tag">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={NO_TAG}>Sem marcador</SelectItem>
                {tags.map((tag) => (
                  <SelectItem key={tag.id} value={tag.id}>
                    {tag.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <p className="text-xs text-muted-foreground">
              Opcional. Os marcadores são cadastrados em Configurações → Agenda → Marcadores.
            </p>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-2">
              <Label htmlFor="recall-amount">Prazo *</Label>
              <Input id="recall-amount" type="number" min={1} {...form.register("dueInAmount")} />
              {form.formState.errors.dueInAmount && (
                <p className="text-sm text-destructive">
                  {form.formState.errors.dueInAmount.message}
                </p>
              )}
            </div>
            <div className="space-y-2">
              <Label>Unidade *</Label>
              <Select
                value={form.watch("dueInUnit")}
                onValueChange={(value) =>
                  form.setValue("dueInUnit", value as RecallDueUnit, { shouldValidate: true })
                }
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {DUE_UNITS.map((unit) => (
                    <SelectItem key={unit} value={unit}>
                      {RECALL_DUE_UNIT_LABELS[unit]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {form.formState.errors.dueInUnit && (
                <p className="text-sm text-destructive">
                  {form.formState.errors.dueInUnit.message}
                </p>
              )}
            </div>
          </div>
          <p className="text-xs text-muted-foreground">O prazo é calculado a partir de hoje.</p>

          <SheetFooter className="mt-auto border-t pt-4">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancelar
            </Button>
            <Button type="submit" disabled={isLoading}>
              {isLoading ? "Criando…" : "Criar"}
            </Button>
          </SheetFooter>
        </form>
      </SheetContent>
    </Sheet>
  );
}
