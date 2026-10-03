"use client";

import { format } from "date-fns";
import { ptBR } from "date-fns/locale";
import { X, Clock, User, MapPin, FileText, Phone, Mail } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { AppointmentStatusBadge } from "./AppointmentStatusBadge";
import {
  LOCATION_KIND_LABELS,
  type Appointment,
  type AppointmentStatus,
} from "../types/scheduling.types";
import type { Contact } from "@/features/masterdata/contacts/types/contact.types";

interface Props {
  appointment: Appointment;
  contact?: Contact | null;
  hostName?: string;
  onClose: () => void;
  onChangeStatus: (id: string, status: AppointmentStatus) => void;
  onDelete: (id: string) => void;
  canUpdate: boolean;
  canDelete: boolean;
}

const STATUS_TRANSITIONS: Record<AppointmentStatus, AppointmentStatus[]> = {
  SCHEDULED: ["CONFIRMED", "CANCELED"],
  CONFIRMED: ["COMPLETED", "NO_SHOW", "CANCELED"],
  CANCELED: [],
  COMPLETED: [],
  NO_SHOW: [],
};

const STATUS_ACTION_LABELS: Record<AppointmentStatus, string> = {
  SCHEDULED: "Agendar",
  CONFIRMED: "Confirmar",
  CANCELED: "Cancelar",
  COMPLETED: "Concluir",
  NO_SHOW: "Marcar falta",
};

export function AppointmentDetailPanel({
  appointment,
  contact,
  hostName,
  onClose,
  onChangeStatus,
  onDelete,
  canUpdate,
  canDelete,
}: Props) {
  const start = new Date(appointment.startAt);
  const end = new Date(appointment.endAt);
  const transitions = STATUS_TRANSITIONS[appointment.status];

  return (
    <div className="flex w-80 flex-col gap-4 border-l bg-card p-4">
      <div className="flex items-start justify-between">
        <h3 className="text-lg font-semibold leading-tight">{appointment.title}</h3>
        <Button variant="ghost" size="icon" onClick={onClose}>
          <X className="h-4 w-4" />
        </Button>
      </div>

      <AppointmentStatusBadge status={appointment.status} />

      {contact && (
        <div className="space-y-1 rounded-md border bg-muted/30 p-3">
          <div className="flex items-center gap-2 text-sm font-medium">
            <User className="h-4 w-4 shrink-0" />
            {contact.firstName} {contact.lastName}
          </div>
          {contact.phone && (
            <div className="flex items-center gap-2 text-xs text-muted-foreground">
              <Phone className="h-3 w-3 shrink-0" />
              {contact.phone}
            </div>
          )}
          {contact.email && (
            <div className="flex items-center gap-2 text-xs text-muted-foreground">
              <Mail className="h-3 w-3 shrink-0" />
              {contact.email}
            </div>
          )}
        </div>
      )}

      {hostName && (
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <User className="h-4 w-4 shrink-0" />
          <span>{hostName}</span>
        </div>
      )}

      <div className="flex items-center gap-2 text-sm text-muted-foreground">
        <Clock className="h-4 w-4 shrink-0" />
        <span>
          {format(start, "dd MMM yyyy HH:mm", { locale: ptBR })} –{" "}
          {format(end, "HH:mm", { locale: ptBR })}
        </span>
      </div>

      {appointment.locationKind && (
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <MapPin className="h-4 w-4 shrink-0" />
          <span>
            {LOCATION_KIND_LABELS[appointment.locationKind]}
            {appointment.locationDetail ? ` · ${appointment.locationDetail}` : ""}
          </span>
        </div>
      )}

      {appointment.meetingUrl && (
        <a
          href={appointment.meetingUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="truncate text-sm text-primary hover:underline"
        >
          Link da reunião
        </a>
      )}

      {appointment.notes && (
        <div className="flex items-start gap-2 text-sm text-muted-foreground">
          <FileText className="mt-0.5 h-4 w-4 shrink-0" />
          <p className="whitespace-pre-wrap">{appointment.notes}</p>
        </div>
      )}

      {canUpdate && transitions.length > 0 && (
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="outline" size="sm" className="w-full">
              Alterar status
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent>
            {transitions.map((s) => (
              <DropdownMenuItem key={s} onClick={() => onChangeStatus(appointment.id, s)}>
                {STATUS_ACTION_LABELS[s]}
              </DropdownMenuItem>
            ))}
          </DropdownMenuContent>
        </DropdownMenu>
      )}

      {canDelete && (
        <Button
          variant="destructive"
          size="sm"
          className="w-full"
          onClick={() => onDelete(appointment.id)}
        >
          Excluir
        </Button>
      )}
    </div>
  );
}
