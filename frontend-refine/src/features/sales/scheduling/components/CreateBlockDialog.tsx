"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetFooter } from "@/components/ui/sheet";
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
import { createBlockSchema, type CreateBlockFormValues } from "../schemas/scheduling.schema";

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  isLoading: boolean;
  onSubmit: (values: CreateBlockFormValues) => void;
  members: { id: string; name: string }[];
  defaultStart?: string;
  defaultEnd?: string;
}

export function CreateBlockDialog({
  open,
  onOpenChange,
  isLoading,
  onSubmit,
  members,
  defaultStart,
  defaultEnd,
}: Props) {
  const form = useForm<CreateBlockFormValues>({
    resolver: zodResolver(createBlockSchema),
    defaultValues: {
      hostId: "",
      startAt: defaultStart ?? "",
      endAt: defaultEnd ?? "",
      reason: "",
    },
  });

  useEffect(() => {
    if (open) {
      form.reset({
        hostId: "",
        startAt: defaultStart ?? "",
        endAt: defaultEnd ?? "",
        reason: "",
      });
    }
  }, [open, defaultStart, defaultEnd, form]);

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="flex flex-col overflow-y-auto sm:max-w-md">
        <SheetHeader>
          <SheetTitle>Bloquear horário</SheetTitle>
        </SheetHeader>

        <form
          onSubmit={form.handleSubmit((v) => onSubmit(v))}
          className="flex flex-1 flex-col gap-4 pt-4"
        >
          <div className="space-y-2">
            <Label htmlFor="block-hostId">Responsável *</Label>
            <Select value={form.watch("hostId")} onValueChange={(v) => form.setValue("hostId", v)}>
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
            {form.formState.errors.hostId && (
              <p className="text-sm text-destructive">{form.formState.errors.hostId.message}</p>
            )}
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="block-startAt">Início *</Label>
              <Input id="block-startAt" type="datetime-local" {...form.register("startAt")} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="block-endAt">Fim *</Label>
              <Input id="block-endAt" type="datetime-local" {...form.register("endAt")} />
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="block-reason">Motivo</Label>
            <Input id="block-reason" {...form.register("reason")} placeholder="Ex.: Almoço" />
          </div>

          <SheetFooter className="mt-auto border-t pt-4">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancelar
            </Button>
            <Button type="submit" disabled={isLoading}>
              {isLoading ? "Salvando…" : "Bloquear"}
            </Button>
          </SheetFooter>
        </form>
      </SheetContent>
    </Sheet>
  );
}
