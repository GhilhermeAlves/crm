"use client";

import { useState } from "react";
import { format, parseISO } from "date-fns";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { isAdminLevelRole } from "@/features/identity/members/lib/labels";
import { useContactAppointments } from "@/features/clinical/patients/hooks/usePatient";
import {
  useCreateEvolution,
  useDeleteEvolution,
  useEvolutions,
  useFinalizeEvolution,
  useUpdateEvolution,
} from "../hooks/useEvolutions";
import type { Contact } from "@/features/masterdata/contacts/types/contact.types";
import type { Appointment } from "@/features/sales/scheduling/types/scheduling.types";

function appointmentLabel(app: Appointment) {
  const title = app.title || "Consulta";
  try {
    return `${format(parseISO(app.startAt), "dd/MM/yyyy HH:mm")} – ${title}`;
  } catch {
    return title;
  }
}

export function PatientEvolutionsTab({
  companyId,
  contactId,
  patient,
}: {
  companyId: string;
  contactId: string;
  patient: Contact;
}) {
  const { user } = useAuth();
  const { data: evolutions = [] } = useEvolutions(companyId, contactId);
  const { data: appointments = [] } = useContactAppointments(companyId, contactId);
  const createMutation = useCreateEvolution(companyId, contactId);
  const updateMutation = useUpdateEvolution(companyId, contactId);
  const finalizeMutation = useFinalizeEvolution(companyId, contactId);
  const deleteMutation = useDeleteEvolution(companyId, contactId);
  const [body, setBody] = useState("");
  const [appointmentId, setAppointmentId] = useState<string>("");
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editBody, setEditBody] = useState("");
  const [editAppointmentId, setEditAppointmentId] = useState<string>("");

  const isAdmin =
    isAdminLevelRole(user?.membershipRole) ||
    (user?.roles ?? []).some((role) => isAdminLevelRole(role));

  const handleCreate = () => {
    if (!body.trim()) return;
    createMutation.mutate(
      { body: body.trim(), appointmentId: appointmentId || null },
      {
        onSuccess: () => {
          setBody("");
          setAppointmentId("");
        },
      },
    );
  };

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle>Nova evolução</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <Textarea
            value={body}
            onChange={(e) => setBody(e.target.value)}
            placeholder="Nova evolução"
            rows={4}
          />
          <div className="flex flex-col gap-2 sm:flex-row">
            <Select
              value={appointmentId || "none"}
              onValueChange={(v) => setAppointmentId(v === "none" ? "" : v)}
            >
              <SelectTrigger className="w-full sm:w-[320px]">
                <SelectValue placeholder="Vincular à consulta (opcional)" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="none">Sem vínculo</SelectItem>
                {appointments.map((app) => (
                  <SelectItem key={app.id} value={app.id}>
                    {appointmentLabel(app)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Button disabled={createMutation.isPending || !body.trim()} onClick={handleCreate}>
              Salvar rascunho
            </Button>
          </div>
        </CardContent>
      </Card>

      <div className="space-y-3">
        {[...evolutions]
          .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
          .map((evo) => {
            const isAuthor = evo.createdBy === user?.id;
            const isDraft = evo.status === "DRAFT";
            const canModify = isDraft && (isAuthor || isAdmin);
            const canFinalize = isDraft && isAuthor;
            if (editingId === evo.id && isDraft) {
              return (
                <Card key={evo.id}>
                  <CardContent className="space-y-3 pt-6">
                    <Textarea
                      value={editBody}
                      onChange={(e) => setEditBody(e.target.value)}
                      rows={4}
                    />
                    <div className="flex flex-col gap-2 sm:flex-row">
                      <Select
                        value={editAppointmentId || "none"}
                        onValueChange={(v) => setEditAppointmentId(v === "none" ? "" : v)}
                      >
                        <SelectTrigger className="w-full sm:w-[320px]">
                          <SelectValue placeholder="Vincular à consulta (opcional)" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="none">Sem vínculo</SelectItem>
                          {appointments.map((app) => (
                            <SelectItem key={app.id} value={app.id}>
                              {appointmentLabel(app)}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                      <div className="flex gap-2">
                        <Button
                          disabled={updateMutation.isPending}
                          onClick={() =>
                            updateMutation.mutate(
                              {
                                evolutionId: evo.id,
                                payload: {
                                  body: editBody.trim(),
                                  appointmentId: editAppointmentId || null,
                                },
                              },
                              { onSuccess: () => setEditingId(null) },
                            )
                          }
                        >
                          Salvar
                        </Button>
                        <Button variant="outline" onClick={() => setEditingId(null)}>
                          Cancelar
                        </Button>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              );
            }
            return (
              <Card key={evo.id}>
                <CardContent className="space-y-2 pt-6">
                  <div className="flex items-start justify-between">
                    <div>
                      <p className="text-sm font-medium">{evo.createdByName || "Usuário"}</p>
                      <p className="text-xs text-muted-foreground">
                        {format(parseISO(evo.createdAt), "dd/MM/yyyy HH:mm")}
                      </p>
                    </div>
                    <Badge variant={isDraft ? "outline" : "secondary"}>
                      {isDraft ? "Rascunho" : "Finalizada"}
                    </Badge>
                  </div>
                  <p className="whitespace-pre-wrap text-sm">{evo.body}</p>
                  {!isDraft && evo.finalizedByName && evo.finalizedAt && (
                    <p className="text-xs text-muted-foreground">
                      Finalizada por {evo.finalizedByName} em{" "}
                      {format(parseISO(evo.finalizedAt), "dd/MM/yyyy HH:mm")}
                    </p>
                  )}
                  {isDraft && (
                    <div className="flex flex-wrap gap-2">
                      {canModify && (
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => {
                            setEditingId(evo.id);
                            setEditBody(evo.body);
                            setEditAppointmentId(evo.appointmentId || "");
                          }}
                        >
                          Editar
                        </Button>
                      )}
                      {canFinalize && (
                        <AlertDialog>
                          <AlertDialogTrigger asChild>
                            <Button size="sm">Finalizar</Button>
                          </AlertDialogTrigger>
                          <AlertDialogContent>
                            <AlertDialogHeader>
                              <AlertDialogTitle>Finalizar evolução?</AlertDialogTitle>
                              <AlertDialogDescription>
                                Depois de finalizada, a evolução não pode mais ser alterada nem
                                excluída.
                              </AlertDialogDescription>
                            </AlertDialogHeader>
                            <AlertDialogFooter>
                              <AlertDialogCancel>Cancelar</AlertDialogCancel>
                              <AlertDialogAction onClick={() => finalizeMutation.mutate(evo.id)}>
                                Finalizar
                              </AlertDialogAction>
                            </AlertDialogFooter>
                          </AlertDialogContent>
                        </AlertDialog>
                      )}
                      {canModify && (
                        <Button
                          variant="ghost"
                          size="sm"
                          className="text-destructive"
                          onClick={() => deleteMutation.mutate(evo.id)}
                        >
                          Excluir
                        </Button>
                      )}
                    </div>
                  )}
                </CardContent>
              </Card>
            );
          })}
      </div>
    </div>
  );
}
