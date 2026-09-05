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
import { useContacts } from "@/features/contacts/hooks/useContacts";
import { useMembers } from "@/features/members/hooks/useMembers";
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
  companyId: string | null;
  onSubmit: (data: LeadFormValues) => void;
  onCancel?: () => void;
  isLoading?: boolean;
  mode: "create" | "edit";
}

export function LeadForm({ lead, companyId, onSubmit, onCancel, isLoading, mode }: LeadFormProps) {
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

  const isEnabled = !!companyId;
  const { data: contacts = [], isLoading: contactsLoading } = useContacts(companyId);
  const { data: members = [], isLoading: membersLoading } = useMembers(companyId);

  const watchedStatus = watch("status");
  const watchedSource = watch("source");
  const watchedClassification = watch("classification");
  const watchedContactId = watch("contactId");
  const watchedAssignedTo = watch("assignedTo");

  const selectedContact = contacts.find((c) => c.id === watchedContactId);

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle>{isEdit ? "Dados do Lead" : "Novo Lead"}</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {!isEdit && (
            <div className="space-y-2">
              <Label htmlFor="contactId">Contato *</Label>
              <Select
                value={watchedContactId}
                onValueChange={(val) => setValue("contactId", val as string)}
              >
                <SelectTrigger id="contactId">
                  <SelectValue placeholder="Selecione o contato..." />
                </SelectTrigger>
                <SelectContent>
                  {contacts.map((c) => (
                    <SelectItem key={c.id} value={c.id}>
                      {c.firstName} {c.lastName ?? ""}
                      {c.email ? ` · ${c.email}` : ""}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {contactsLoading && isEnabled && (
                <p className="text-sm text-muted-foreground">Carregando contatos...</p>
              )}
              {!contactsLoading && contacts.length === 0 && isEnabled && (
                <p className="text-sm text-muted-foreground">
                  Nenhum contato cadastrado. Crie um contato antes de cadastrar o lead.
                </p>
              )}
              {selectedContact && (
                <p className="text-sm text-muted-foreground">
                  {selectedContact.firstName} {selectedContact.lastName ?? ""} —{" "}
                  {selectedContact.email ?? "sem e-mail"} ·{" "}
                  {selectedContact.phone ?? "sem telefone"}
                </p>
              )}
              {errors.contactId && (
                <p className="text-sm text-destructive">{errors.contactId.message}</p>
              )}
            </div>
          )}

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label>Origem *</Label>
              <Select
                value={watchedSource}
                onValueChange={(val) => setValue("source", val as typeof watchedSource)}
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
                onValueChange={(val) => setValue("status", val as typeof watchedStatus)}
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
              <Input id="score" type="text" inputMode="numeric" {...register("score")} />
              {errors.score && <p className="text-sm text-destructive">{errors.score.message}</p>}
            </div>
            <div className="space-y-2">
              <Label>Classificação</Label>
              <Select
                value={watchedClassification || "none"}
                onValueChange={(val) =>
                  setValue(
                    "classification",
                    (val === "none" ? "" : val) as "" | typeof watchedClassification,
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
            <Label htmlFor="assignedTo">Responsável</Label>
            <Select
              value={watchedAssignedTo}
              onValueChange={(val) => setValue("assignedTo", val as string)}
            >
              <SelectTrigger id="assignedTo">
                <SelectValue placeholder="Selecione o responsável..." />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="none">Sem responsável</SelectItem>
                {members.map((member) => (
                  <SelectItem key={member.userId} value={member.userId}>
                    {member.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            {membersLoading && isEnabled && (
              <p className="text-sm text-muted-foreground">Carregando membros...</p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="notes">Observações</Label>
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
          {isLoading ? "Salvando..." : isEdit ? "Salvar Alterações" : "Criar Lead"}
        </Button>
      </div>
    </form>
  );
}
