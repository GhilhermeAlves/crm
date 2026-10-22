"use client";

import { Fragment, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { Eye, MessageCircle, MoreHorizontal, Plus, Trash2 } from "lucide-react";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { useMembers } from "@/features/identity/members/hooks/useMembers";
import { useContacts } from "@/features/masterdata/contacts/hooks/useContacts";
import {
  useAppointmentTypes,
  useCreateAppointment,
} from "@/features/sales/scheduling/hooks/useScheduling";
import { CreateAppointmentDialog } from "@/features/sales/scheduling/components/CreateAppointmentDialog";
import type { CreateAppointmentFormValues } from "@/features/sales/scheduling/schemas/scheduling.schema";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent } from "@/components/ui/card";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { ROUTES } from "@/lib/constants";
import { useQueryClient } from "@tanstack/react-query";
import {
  useCreateRecall,
  useDismissRecall,
  useDismissRecallSuggestion,
  useRecalls,
  useRecallSuggestions,
  useScheduleRecall,
} from "../hooks/useRecalls";
import { useTags } from "@/features/sales/tag/hooks/useTags";
import { CreateRecallDialog } from "./CreateRecallDialog";
import { useRecallPermissions } from "../schemas/recall.schema";
import { RECALL_WINDOW_LABELS, type Recall, type RecallWindow } from "../types/recall.types";
import { formatDateOnly, isOverdue, whatsappUrl } from "../utils/recall-utils";

const WINDOWS = Object.keys(RECALL_WINDOW_LABELS) as RecallWindow[];
const SINCE_OPTIONS = [
  { value: 1, label: "1 mês atrás" },
  { value: 3, label: "3 meses atrás" },
  { value: 6, label: "6 meses atrás" },
];

type ScheduleTarget = {
  recallId: string | null;
  contactId: string;
  hostId: string;
};

export function RecallsTab() {
  const { user } = useAuth();
  const router = useRouter();
  const queryClient = useQueryClient();
  const companyId = user?.companyId ?? null;
  const perms = useRecallPermissions();

  const [search, setSearch] = useState("");
  const [hostFilter, setHostFilter] = useState("all");
  const [windowFilter, setWindowFilter] = useState<RecallWindow>("NEXT_7_DAYS");
  const [tagFilter, setTagFilter] = useState("all");
  const [sinceMonths, setSinceMonths] = useState(3);
  const [expandedContactId, setExpandedContactId] = useState<string | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [scheduleTarget, setScheduleTarget] = useState<ScheduleTarget | null>(null);
  const [dismissRecallTarget, setDismissRecallTarget] = useState<Recall | null>(null);
  const [dismissSuggestionTarget, setDismissSuggestionTarget] = useState<{
    contactId: string;
    contactName: string;
  } | null>(null);

  const recallsQuery = useRecalls(companyId, {
    q: search.trim() || undefined,
    hostId: hostFilter === "all" ? undefined : hostFilter,
    tagId: tagFilter === "all" ? undefined : tagFilter,
    window: windowFilter,
    pageSize: 100,
  });
  const suggestionsQuery = useRecallSuggestions(companyId, sinceMonths);
  const membersQuery = useMembers(companyId);
  const contactsQuery = useContacts(companyId);
  const appointmentTypesQuery = useAppointmentTypes(companyId);
  const { data: tags = [] } = useTags(companyId);
  const tagNameById = useMemo(() => new Map(tags.map((t) => [t.id, t.name])), [tags]);

  const createRecall = useCreateRecall(companyId);
  const dismissRecall = useDismissRecall(companyId);
  const scheduleRecall = useScheduleRecall(companyId);
  const dismissSuggestion = useDismissRecallSuggestion(companyId);
  const createAppointment = useCreateAppointment(companyId);

  const members = useMemo(() => membersQuery.data ?? [], [membersQuery.data]);
  const contacts = useMemo(() => contactsQuery.data ?? [], [contactsQuery.data]);
  const appointmentTypes = useMemo(
    () => appointmentTypesQuery.data ?? [],
    [appointmentTypesQuery.data],
  );

  const memberOptions = useMemo(
    () => members.map((member) => ({ id: member.userId, name: member.name })),
    [members],
  );
  const contactsById = useMemo(
    () => new Map(contacts.map((contact) => [contact.id, contact])),
    [contacts],
  );

  const recalls = recallsQuery.data?.content ?? [];
  const suggestions = suggestionsQuery.data ?? [];

  const contactName = (contactId: string) => {
    const contact = contactsById.get(contactId);
    if (!contact) return "Paciente";
    return `${contact.firstName} ${contact.lastName ?? ""}`.trim() || "Paciente";
  };

  const memberName = (hostId: string) =>
    members.find((member) => member.userId === hostId)?.name ?? "—";

  const handleCreateRecall = (values: {
    contactId: string;
    phone: string;
    hostId: string;
    tagId?: string;
    dueInAmount: number;
    dueInUnit: "DAYS" | "WEEKS" | "MONTHS" | "YEARS";
  }) => {
    createRecall.mutate(
      {
        contactId: values.contactId,
        phone: values.phone,
        hostId: values.hostId,
        tagId: values.tagId,
        dueInAmount: values.dueInAmount,
        dueInUnit: values.dueInUnit,
      },
      { onSuccess: () => setCreateOpen(false) },
    );
  };

  const handleSchedule = (values: CreateAppointmentFormValues) => {
    createAppointment.mutate(
      {
        ...values,
        startAt: new Date(values.startAt).toISOString(),
        endAt: new Date(values.endAt).toISOString(),
      },
      {
        onSuccess: (created) => {
          if (scheduleTarget?.recallId) {
            scheduleRecall.mutate({
              recallId: scheduleTarget.recallId,
              appointmentId: created.id,
            });
          }
          queryClient.invalidateQueries({ queryKey: ["recall-suggestions", companyId] });
          setScheduleTarget(null);
        },
      },
    );
  };

  return (
    <div className="flex flex-1 flex-col gap-4 overflow-y-auto p-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold">Retornos</h2>
          <p className="text-sm text-muted-foreground">
            Veja pacientes com retorno programado e sugestões geradas pelo sistema.
          </p>
        </div>
        {perms.canCreate && (
          <Button size="sm" onClick={() => setCreateOpen(true)}>
            <Plus className="mr-2 h-4 w-4" />
            Criar retorno
          </Button>
        )}
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <Input
          placeholder="Buscar paciente…"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          className="w-56"
        />
        <Select value={hostFilter} onValueChange={setHostFilter}>
          <SelectTrigger className="w-48">
            <SelectValue placeholder="Dentista" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Todos os dentistas</SelectItem>
            {memberOptions.map((member) => (
              <SelectItem key={member.id} value={member.id}>
                {member.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Select
          value={windowFilter}
          onValueChange={(value) => setWindowFilter(value as RecallWindow)}
        >
          <SelectTrigger className="w-40" aria-label="Janela de vencimento">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {WINDOWS.map((window) => (
              <SelectItem key={window} value={window}>
                {RECALL_WINDOW_LABELS[window]}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Select value={tagFilter} onValueChange={setTagFilter}>
          <SelectTrigger className="w-44" aria-label="Filtrar por marcador">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Todos os marcadores</SelectItem>
            {tags.map((tag) => (
              <SelectItem key={tag.id} value={tag.id}>
                {tag.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <Card>
        <CardContent className="pt-6">
          <h3 className="mb-3 text-sm font-semibold">Programados</h3>
          {recallsQuery.isLoading ? (
            <div className="space-y-2">
              {Array.from({ length: 3 }).map((_, index) => (
                <div key={index} className="h-12 animate-pulse rounded bg-muted" />
              ))}
            </div>
          ) : recalls.length === 0 ? (
            <p className="text-sm text-muted-foreground">
              Nenhum retorno previsto para o período selecionado.
            </p>
          ) : (
            <div className="rounded-md border">
              <div className="overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Paciente</TableHead>
                      <TableHead>Dentista</TableHead>
                      <TableHead>Data prevista</TableHead>
                      <TableHead>Marcador</TableHead>
                      <TableHead className="w-[80px]"></TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {recalls.map((recall) => (
                      <TableRow key={recall.id}>
                        <TableCell>
                          <p className="font-medium">{contactName(recall.contactId)}</p>
                          {recall.phone && (
                            <p className="text-xs text-muted-foreground">{recall.phone}</p>
                          )}
                        </TableCell>
                        <TableCell className="text-sm">{memberName(recall.hostId)}</TableCell>
                        <TableCell>
                          <div className="flex items-center gap-2">
                            <span className="text-sm">{formatDateOnly(recall.dueDate)}</span>
                            {isOverdue(recall.dueDate) && recall.status === "PENDING" && (
                              <Badge variant="destructive">vencido</Badge>
                            )}
                            {recall.status === "SCHEDULED" && (
                              <Badge variant="secondary">agendado</Badge>
                            )}
                          </div>
                        </TableCell>
                        <TableCell className="text-sm text-muted-foreground">
                          {(recall.tagId && tagNameById.get(recall.tagId)) ?? recall.tag ?? "—"}
                        </TableCell>
                        <TableCell>
                          <div className="flex items-center justify-end gap-1">
                            {perms.canCreate && recall.status === "PENDING" && (
                              <Button
                                size="sm"
                                variant="outline"
                                onClick={() =>
                                  setScheduleTarget({
                                    recallId: recall.id,
                                    contactId: recall.contactId,
                                    hostId: recall.hostId,
                                  })
                                }
                              >
                                Agendar
                              </Button>
                            )}
                            {perms.canUpdate && (
                              <DropdownMenu>
                                <DropdownMenuTrigger asChild>
                                  <Button
                                    variant="ghost"
                                    size="icon"
                                    className="h-8 w-8"
                                    aria-label="Ações do retorno"
                                  >
                                    <MoreHorizontal className="h-4 w-4" />
                                  </Button>
                                </DropdownMenuTrigger>
                                <DropdownMenuContent align="end">
                                  <DropdownMenuItem
                                    className="text-destructive"
                                    onClick={() => setDismissRecallTarget(recall)}
                                  >
                                    <Trash2 className="mr-2 h-4 w-4" />
                                    Descartar
                                  </DropdownMenuItem>
                                </DropdownMenuContent>
                              </DropdownMenu>
                            )}
                          </div>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardContent className="pt-6">
          <div className="mb-3 flex items-center gap-2">
            <h3 className="text-sm font-semibold">Sugeridos</h3>
            <Badge variant="outline">{suggestions.length}</Badge>
            <div className="ml-auto flex items-center gap-2">
              <Select
                value={String(sinceMonths)}
                onValueChange={(value) => setSinceMonths(Number(value))}
              >
                <SelectTrigger className="w-40" aria-label="Período das sugestões">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {SINCE_OPTIONS.map((option) => (
                    <SelectItem key={option.value} value={String(option.value)}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
          {suggestionsQuery.isLoading ? (
            <div className="space-y-2">
              {Array.from({ length: 3 }).map((_, index) => (
                <div key={index} className="h-12 animate-pulse rounded bg-muted" />
              ))}
            </div>
          ) : suggestions.length === 0 ? (
            <p className="text-sm text-muted-foreground">Nenhuma sugestão de retorno no momento.</p>
          ) : (
            <div className="rounded-md border">
              <div className="overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Paciente</TableHead>
                      <TableHead>Dentista</TableHead>
                      <TableHead>Última consulta</TableHead>
                      <TableHead className="w-[280px]">Ações</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {suggestions.map((suggestion) => {
                      const whatsapp = whatsappUrl(suggestion.mobile ?? suggestion.phone);
                      const expanded = expandedContactId === suggestion.contactId;
                      return (
                        <Fragment key={suggestion.contactId}>
                          <TableRow>
                            <TableCell>
                              <p className="font-medium">{suggestion.contactName}</p>
                              {(suggestion.mobile ?? suggestion.phone) && (
                                <p className="text-xs text-muted-foreground">
                                  {suggestion.mobile ?? suggestion.phone}
                                </p>
                              )}
                            </TableCell>
                            <TableCell className="text-sm">
                              {memberName(suggestion.hostId)}
                            </TableCell>
                            <TableCell className="text-sm">
                              {new Date(suggestion.lastAppointmentAt).toLocaleDateString("pt-BR", {
                                day: "2-digit",
                                month: "2-digit",
                                year: "numeric",
                              })}
                            </TableCell>
                            <TableCell>
                              <div className="flex items-center gap-1">
                                <Button
                                  size="sm"
                                  variant="ghost"
                                  onClick={() =>
                                    setExpandedContactId(expanded ? null : suggestion.contactId)
                                  }
                                >
                                  <Eye className="mr-1 h-4 w-4" />
                                  Visualizar
                                </Button>
                                {whatsapp && (
                                  <a
                                    href={whatsapp}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    aria-label="Conversar no WhatsApp"
                                    title="Conversar no WhatsApp"
                                    className="inline-flex h-8 w-8 items-center justify-center rounded-md text-emerald-600 hover:bg-emerald-50 hover:text-emerald-700"
                                  >
                                    <MessageCircle className="h-4 w-4" />
                                  </a>
                                )}
                                {perms.canCreate && (
                                  <Button
                                    size="sm"
                                    variant="outline"
                                    onClick={() =>
                                      setScheduleTarget({
                                        recallId: null,
                                        contactId: suggestion.contactId,
                                        hostId: suggestion.hostId,
                                      })
                                    }
                                  >
                                    Agendar
                                  </Button>
                                )}
                                <DropdownMenu>
                                  <DropdownMenuTrigger asChild>
                                    <Button
                                      variant="ghost"
                                      size="icon"
                                      className="h-8 w-8"
                                      aria-label="Ações da sugestão"
                                    >
                                      <MoreHorizontal className="h-4 w-4" />
                                    </Button>
                                  </DropdownMenuTrigger>
                                  <DropdownMenuContent align="end">
                                    <DropdownMenuItem
                                      onClick={() =>
                                        router.push(`${ROUTES.PATIENTS}/${suggestion.contactId}`)
                                      }
                                    >
                                      Ver ficha clínica
                                    </DropdownMenuItem>
                                    {perms.canUpdate && (
                                      <DropdownMenuItem
                                        className="text-destructive"
                                        onClick={() =>
                                          setDismissSuggestionTarget({
                                            contactId: suggestion.contactId,
                                            contactName: suggestion.contactName,
                                          })
                                        }
                                      >
                                        <Trash2 className="mr-2 h-4 w-4" />
                                        Descartar sugestão
                                      </DropdownMenuItem>
                                    )}
                                  </DropdownMenuContent>
                                </DropdownMenu>
                              </div>
                            </TableCell>
                          </TableRow>
                          {expanded && (
                            <TableRow>
                              <TableCell colSpan={4} className="bg-muted/40">
                                <p className="mb-1 text-xs font-medium text-muted-foreground">
                                  Últimos procedimentos
                                </p>
                                {suggestion.recentProcedures.length === 0 ? (
                                  <p className="text-sm text-muted-foreground">
                                    Nenhum procedimento registrado.
                                  </p>
                                ) : (
                                  <ul className="list-inside list-disc text-sm">
                                    {suggestion.recentProcedures.map((procedure, index) => (
                                      <li key={index}>{procedure}</li>
                                    ))}
                                  </ul>
                                )}
                              </TableCell>
                            </TableRow>
                          )}
                        </Fragment>
                      );
                    })}
                  </TableBody>
                </Table>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      <CreateRecallDialog
        open={createOpen}
        onOpenChange={setCreateOpen}
        contacts={contacts}
        members={memberOptions}
        isLoading={createRecall.isPending}
        onSubmit={handleCreateRecall}
        tags={tags}
      />

      <CreateAppointmentDialog
        open={scheduleTarget !== null}
        onOpenChange={(open) => {
          if (!open) setScheduleTarget(null);
        }}
        isLoading={createAppointment.isPending}
        onSubmit={handleSchedule}
        appointmentTypes={appointmentTypes}
        members={memberOptions}
        contacts={contacts}
        defaultContactId={scheduleTarget?.contactId}
        defaultHostId={scheduleTarget?.hostId}
      />

      <ConfirmDialog
        open={dismissRecallTarget !== null}
        onOpenChange={(open) => {
          if (!open) setDismissRecallTarget(null);
        }}
        title="Descartar retorno"
        description={`Tem certeza que deseja descartar o retorno de ${dismissRecallTarget ? contactName(dismissRecallTarget.contactId) : ""}?`}
        confirmLabel="Descartar"
        variant="destructive"
        isLoading={dismissRecall.isPending}
        onConfirm={() => {
          if (dismissRecallTarget) {
            dismissRecall.mutate(dismissRecallTarget.id, {
              onSuccess: () => setDismissRecallTarget(null),
            });
          }
        }}
      />

      <ConfirmDialog
        open={dismissSuggestionTarget !== null}
        onOpenChange={(open) => {
          if (!open) setDismissSuggestionTarget(null);
        }}
        title="Descartar sugestão"
        description={`Tem certeza que deseja descartar a sugestão de retorno para ${dismissSuggestionTarget?.contactName ?? ""}? Ela não será exibida novamente.`}
        confirmLabel="Descartar"
        variant="destructive"
        isLoading={dismissSuggestion.isPending}
        onConfirm={() => {
          if (dismissSuggestionTarget) {
            dismissSuggestion.mutate(dismissSuggestionTarget.contactId, {
              onSuccess: () => setDismissSuggestionTarget(null),
            });
          }
        }}
      />
    </div>
  );
}
