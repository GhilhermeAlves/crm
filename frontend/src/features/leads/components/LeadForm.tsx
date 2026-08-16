"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import type { Lead } from "../types/lead.types";
import {
  leadFormSchema,
  leadStatusLabels,
  leadSourceLabels,
  leadClassificationLabels,
  leadStatuses,
  leadSources,
  leadClassifications,
  type LeadFormValues,
} from "../schemas/lead.schema";
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
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

interface LeadFormProps {
  lead?: Lead;
  onSubmit: (data: LeadFormValues) => void;
  onCancel?: () => void;
  isLoading?: boolean;
  mode: "create" | "edit";
}

export function LeadForm({
  lead,
  onSubmit,
  onCancel,
  isLoading,
  mode,
}: LeadFormProps) {
  const isEdit = mode === "edit";

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors },
  } = useForm<LeadFormValues>({
    resolver: zodResolver(leadFormSchema),
    defaultValues: {
      contactId: lead?.contactId || "",
      status: lead?.status || "NEW",
      source: lead?.source || "MANUAL",
      score: lead ? String(lead.score) : "0",
      classification: lead?.classification || "",
      assignedTo: lead?.assignedTo || "",
      notes: lead?.notes || "",
    },
  });

  const watchedStatus = watch("status");
  const watchedSource = watch("source");
  const watchedClassification = watch("classification");

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle>{isEdit ? "Dados do Lead" : "Novo Lead"}</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {!isEdit && (
            <div className="space-y-2">
              <Label htmlFor="contactId">Contato (ID) *</Label>
              <Input
                id="contactId"
                placeholder="UUID do contato (ex.: 3fa85f64-5717-4562-b3fc-2c963f66afa6)"
                {...register("contactId")}
              />
              {errors.contactId && (
                <p className="text-sm text-destructive">
                  {errors.contactId.message}
                </p>
              )}
            </div>
          )}

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label>Origem *</Label>
              <Select
                value={watchedSource}
                onValueChange={(val) =>
                  setValue("source", val as typeof watchedSource)
                }
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {leadSources.map((value) => (
                    <SelectItem key={value} value={value}>
                      {leadSourceLabels[value]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>Status</Label>
              <Select
                value={watchedStatus}
                onValueChange={(val) =>
                  setValue("status", val as typeof watchedStatus)
                }
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {leadStatuses.map((value) => (
                    <SelectItem key={value} value={value}>
                      {leadStatusLabels[value]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="score">Score (0-100)</Label>
              <Input
                id="score"
                type="text"
                inputMode="numeric"
                {...register("score")}
              />
              {errors.score && (
                <p className="text-sm text-destructive">
                  {errors.score.message}
                </p>
              )}
            </div>
            <div className="space-y-2">
              <Label>Classificação</Label>
              <Select
                value={watchedClassification || "none"}
                onValueChange={(val) =>
                  setValue(
                    "classification",
                    (val === "none" ? "" : val) as
                      "" | typeof watchedClassification,
                  )
                }
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">Sem classificação</SelectItem>
                  {leadClassifications.map((value) => (
                    <SelectItem key={value} value={value}>
                      {leadClassificationLabels[value]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="assignedTo">Responsável (ID do usuário)</Label>
            <Input
              id="assignedTo"
              placeholder="UUID do usuário responsável"
              {...register("assignedTo")}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="notes">Notas</Label>
            <Textarea id="notes" rows={3} {...register("notes")} />
          </div>
        </CardContent>
      </Card>

      <div className="flex justify-end gap-3">
        {onCancel && (
          <Button type="button" variant="outline" onClick={onCancel}>
            Cancelar
          </Button>
        )}
        <Button type="submit" disabled={isLoading}>
          {isLoading
            ? "Salvando..."
            : isEdit
              ? "Salvar Alterações"
              : "Criar Lead"}
        </Button>
      </div>
    </form>
  );
}
