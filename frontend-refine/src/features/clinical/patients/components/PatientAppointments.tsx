"use client";

import { useMemo, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { format, parseISO, isBefore } from "date-fns";
import { ptBR } from "date-fns/locale";
import { toast } from "sonner";
import { CalendarPlus, Check, ChevronDown, ChevronUp, Loader2, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { useMembers } from "@/features/identity/members/hooks/useMembers";
import { AppointmentStatusBadge } from "@/features/sales/scheduling/components/AppointmentStatusBadge";
import { CreateAppointmentDialog } from "@/features/sales/scheduling/components/CreateAppointmentDialog";
import {
  useAppointmentTypes,
  useAgendaSettings,
  useChangeAppointmentStatus,
} from "@/features/sales/scheduling/hooks/useScheduling";
import { useSubmitNewAppointment } from "@/features/sales/scheduling/hooks/useSubmitNewAppointment";
import { DEFAULT_AGENDA_SETTINGS } from "@/features/sales/scheduling/services/scheduling.service";
import { useChairs } from "@/features/sales/chair/hooks/useChairs";
import { useTags } from "@/features/sales/tag/hooks/useTags";
import type { CreateAppointmentFormValues } from "@/features/sales/scheduling/schemas/scheduling.schema";
import type {
  Appointment,
  AppointmentStatus,
} from "@/features/sales/scheduling/types/scheduling.types";
import { useContactAppointments } from "../hooks/usePatient";
import type { Contact } from "@/features/masterdata/contacts/types/contact.types";

const PAGE_SIZE = 10;
const PENDING_STATUSES: AppointmentStatus[] = ["SCHEDULED", "CONFIRMED"];

function formatLong(value: string): string {
  return format(parseISO(value), "EEEE, dd 'de' MMMM 'de' yyyy 'às' HH:mm", { locale: ptBR });
}

function AppointmentRow({
  appointment,
  hostName,
  typeName,
  index,
  onDecide,
  isUpdating,
}: {
  appointment: Appointment;
  hostName?: string;
  typeName?: string;
  index: number;
  onDecide: (id: string, status: AppointmentStatus) => void;
  isUpdating: boolean;
}) {
  const [showNotes, setShowNotes] = useState(false);
  const isPastPending =
    PENDING_STATUSES.includes(appointment.status) &&
    isBefore(parseISO(appointment.startAt), new Date());

  return (
    <li className="space-y-2 rounded-lg border p-3" data-testid={`appointment-${index}`}>
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div className="min-w-0 space-y-0.5">
          <p className="text-sm font-medium capitalize">{formatLong(appointment.startAt)}</p>
          <p className="truncate text-sm text-muted-foreground">
            {[typeName, hostName].filter(Boolean).join(" · ") || appointment.title}
          </p>
        </div>
        <AppointmentStatusBadge status={appointment.status} />
      </div>

      {appointment.notes && (
        <div>
          <button
            type="button"
            className="inline-flex items-center gap-1 text-xs font-medium text-muted-foreground hover:text-foreground"
            onClick={() => setShowNotes((v) => !v)}
            aria-expanded={showNotes}
          >
            {showNotes ? (
              <>
                <ChevronUp className="h-3.5 w-3.5" /> Ocultar observações
              </>
            ) : (
              <>
                <ChevronDown className="h-3.5 w-3.5" /> Ver observações
              </>
            )}
          </button>
          {showNotes && (
            <p className="mt-1 whitespace-pre-wrap text-sm text-muted-foreground">
              {appointment.notes}
            </p>
          )}
        </div>
      )}

      {isPastPending && (
        <div className="flex flex-wrap items-center gap-3 rounded-md bg-muted/50 p-2">
          <span className="text-sm">O paciente compareceu à consulta?</span>
          <div className="flex gap-2">
            <Button
              type="button"
              size="sm"
              className="gap-1"
              disabled={isUpdating}
              onClick={() => onDecide(appointment.id, "COMPLETED")}
            >
              <Check className="h-3.5 w-3.5" /> Compareceu
            </Button>
            <Button
              type="button"
              size="sm"
              variant="outline"
              className="gap-1"
              disabled={isUpdating}
              onClick={() => onDecide(appointment.id, "NO_SHOW")}
            >
              <X className="h-3.5 w-3.5" /> Não compareceu
            </Button>
          </div>
        </div>
      )}
    </li>
  );
}

function AppointmentSection({
  title,
  appointments,
  emptyMessage,
  hostNameById,
  typeNameById,
  onDecide,
  isUpdating,
}: {
  title: string;
  appointments: Appointment[];
  emptyMessage: string;
  hostNameById: Map<string, string>;
  typeNameById: Map<string, string>;
  onDecide: (id: string, status: AppointmentStatus) => void;
  isUpdating: boolean;
}) {
  const [visible, setVisible] = useState(PAGE_SIZE);
  const shown = appointments.slice(0, visible);

  return (
    <section className="space-y-3">
      <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
        {title}
      </h2>
      {appointments.length === 0 ? (
        <p className="text-sm text-muted-foreground">{emptyMessage}</p>
      ) : (
        <>
          <ul className="space-y-2">
            {shown.map((appointment, index) => (
              <AppointmentRow
                key={appointment.id}
                appointment={appointment}
                hostName={hostNameById.get(appointment.hostId)}
                typeName={
                  appointment.appointmentTypeId
                    ? typeNameById.get(appointment.appointmentTypeId)
                    : undefined
                }
                index={index}
                onDecide={onDecide}
                isUpdating={isUpdating}
              />
            ))}
          </ul>
          {visible < appointments.length && (
            <Button variant="outline" size="sm" onClick={() => setVisible((v) => v + PAGE_SIZE)}>
              Ver mais
            </Button>
          )}
        </>
      )}
    </section>
  );
}

export function PatientAppointments({ contact }: { contact: Contact }) {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const contactId = contact.id;
  const queryClient = useQueryClient();

  const { data: appointments = [], isLoading } = useContactAppointments(companyId, contactId);
  const { data: appointmentTypes = [] } = useAppointmentTypes(companyId);
  const { data: members = [] } = useMembers(companyId);
  const createAppointment = useSubmitNewAppointment(companyId);
  // Mesma origem da página da Agenda: expediente dos ajustes, cadeiras ativas e marcadores.
  const { data: agendaSettings = DEFAULT_AGENDA_SETTINGS } = useAgendaSettings(companyId);
  const { data: chairs = [] } = useChairs(companyId);
  const { data: tags = [] } = useTags(companyId);
  const activeChairs = useMemo(() => chairs.filter((c) => c.active), [chairs]);
  const changeStatus = useChangeAppointmentStatus(companyId);

  const [dialogOpen, setDialogOpen] = useState(false);

  const hostNameById = useMemo(() => {
    const map = new Map<string, string>();
    members.forEach((m) => map.set(m.userId, m.name));
    return map;
  }, [members]);

  const typeNameById = useMemo(() => {
    const map = new Map<string, string>();
    appointmentTypes.forEach((t) => map.set(t.id, t.name));
    return map;
  }, [appointmentTypes]);

  const { upcoming, history } = useMemo(() => {
    const now = new Date();
    const upcomingList: Appointment[] = [];
    const historyList: Appointment[] = [];
    appointments.forEach((a) => {
      if (a.status !== "CANCELED" && !isBefore(parseISO(a.startAt), now)) {
        upcomingList.push(a);
      } else {
        historyList.push(a);
      }
    });
    upcomingList.sort((a, b) => a.startAt.localeCompare(b.startAt));
    historyList.sort((a, b) => b.startAt.localeCompare(a.startAt));
    return { upcoming: upcomingList, history: historyList };
  }, [appointments]);

  const handleDecide = (id: string, status: AppointmentStatus) => {
    changeStatus.mutate(
      { id, status },
      {
        onSuccess: () => {
          queryClient.invalidateQueries({
            queryKey: ["contact-appointments", companyId, contactId],
          });
          toast.success(status === "COMPLETED" ? "Presença registrada" : "Ausência registrada");
        },
      },
    );
  };

  const handleCreate = (values: CreateAppointmentFormValues) => {
    void createAppointment.submit(values, () => {
      queryClient.invalidateQueries({ queryKey: ["contact-appointments", companyId, contactId] });
      setDialogOpen(false);
    });
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-lg font-semibold">Consultas</h1>
        <Button size="sm" className="gap-1.5" onClick={() => setDialogOpen(true)}>
          <CalendarPlus className="h-4 w-4" />
          Novo agendamento
        </Button>
      </div>

      {isLoading ? (
        <div className="flex items-center justify-center gap-2 py-20 text-muted-foreground">
          <Loader2 className="h-5 w-5 animate-spin" /> Carregando consultas…
        </div>
      ) : appointments.length === 0 ? (
        <p className="py-16 text-center text-sm text-muted-foreground">
          Não há nenhum agendamento.
        </p>
      ) : (
        <div className="space-y-6">
          <AppointmentSection
            title="Próximas consultas"
            appointments={upcoming}
            emptyMessage="Nenhuma consulta futura."
            hostNameById={hostNameById}
            typeNameById={typeNameById}
            onDecide={handleDecide}
            isUpdating={changeStatus.isPending}
          />
          <AppointmentSection
            title="Histórico de consultas"
            appointments={history}
            emptyMessage="Nenhuma consulta anterior."
            hostNameById={hostNameById}
            typeNameById={typeNameById}
            onDecide={handleDecide}
            isUpdating={changeStatus.isPending}
          />
        </div>
      )}

      <CreateAppointmentDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        isLoading={createAppointment.isPending}
        onSubmit={handleCreate}
        appointmentTypes={appointmentTypes}
        members={members.map((m) => ({ id: m.userId, name: m.name }))}
        contacts={[contact]}
        defaultContactId={contactId}
        defaultHostId={user?.id}
        workHoursStart={agendaSettings.workHoursStart}
        workHoursEnd={agendaSettings.workHoursEnd}
        chairs={activeChairs}
        tags={tags}
      />
    </div>
  );
}
