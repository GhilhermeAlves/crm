"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { opportunityFormSchema, type OpportunityFormValues } from "../schemas/pipeline.schema";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";

interface CreateOpportunityFormProps {
  onSubmit: (values: {
    title: string;
    value: number;
    contactId: string;
    expectedCloseDate?: string;
    notes?: string;
  }) => void;
  onCancel?: () => void;
  isLoading?: boolean;
}

export function CreateOpportunityForm({
  onSubmit,
  onCancel,
  isLoading,
}: CreateOpportunityFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<OpportunityFormValues>({
    resolver: zodResolver(opportunityFormSchema),
    defaultValues: {
      title: "",
      value: "",
      contactId: "",
      expectedCloseDate: "",
      notes: "",
    },
  });

  return (
    <form
      onSubmit={handleSubmit((values) =>
        onSubmit({
          title: values.title,
          value: Number(values.value),
          contactId: values.contactId,
          expectedCloseDate: values.expectedCloseDate || undefined,
          notes: values.notes || undefined,
        }),
      )}
      className="space-y-4"
    >
      <div className="space-y-2">
        <Label htmlFor="title">Título *</Label>
        <Input id="title" placeholder="Ex.: Contrato anual" {...register("title")} />
        {errors.title && <p className="text-sm text-destructive">{errors.title.message}</p>}
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div className="space-y-2">
          <Label htmlFor="value">Valor (R$) *</Label>
          <Input
            id="value"
            type="text"
            inputMode="decimal"
            placeholder="0,00"
            {...register("value")}
          />
          {errors.value && <p className="text-sm text-destructive">{errors.value.message}</p>}
        </div>
        <div className="space-y-2">
          <Label htmlFor="expectedCloseDate">Previsão de fechamento</Label>
          <Input id="expectedCloseDate" type="datetime-local" {...register("expectedCloseDate")} />
        </div>
      </div>

      <div className="space-y-2">
        <Label htmlFor="contactId">Contato (ID) *</Label>
        <Input
          id="contactId"
          placeholder="UUID do contato (ex.: 3fa85f64-5717-4562-b3fc-2c963f66afa6)"
          {...register("contactId")}
        />
        {errors.contactId && <p className="text-sm text-destructive">{errors.contactId.message}</p>}
      </div>

      <div className="space-y-2">
        <Label htmlFor="notes">Notas</Label>
        <Textarea id="notes" rows={3} {...register("notes")} />
      </div>

      <div className="flex justify-end gap-3 pt-2">
        {onCancel && (
          <Button type="button" variant="outline" onClick={onCancel}>
            Cancelar
          </Button>
        )}
        <Button type="submit" disabled={isLoading}>
          {isLoading ? "Salvando..." : "Criar Oportunidade"}
        </Button>
      </div>
    </form>
  );
}
