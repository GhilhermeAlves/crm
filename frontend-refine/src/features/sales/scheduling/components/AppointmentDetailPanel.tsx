"use client";

import { useState } from "react";
import Link from "next/link";
import { format } from "date-fns";
import { ptBR } from "date-fns/locale";
import {
  X,
  Clock,
  User,
  MapPin,
  FileText,
  Phone,
  Mail,
  Trash2,
  CircleCheck,
  CircleX,
  MessageCircle,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { cn } from "@/lib/utils";
import { ROUTES } from "@/lib/constants";
import {
  APPOINTMENT_STATUS_LABELS,
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

/** Situação do atendimento, em botões de um clique. */
const QUICK_STATUSES: AppointmentStatus[] = ["SCHEDULED", "CONFIRMED", "CANCELED"];

/** Espelha as regras do backend (Appointment.confirm/cancel/complete/markNoShow). */
export function canMoveTo(current: AppointmentStatus, target: AppointmentStatus): boolean {
  if (current === target || current === "CANCELED") return false;
  if (target === "CONFIRMED") return current === "SCHEDULED";
  // Voltar para pendente só remarcando.
  return target !== "SCHEDULED";
}

/** Só dígitos, com DDI 55 quando o número veio sem ele. */
export function whatsappUrl(phone: string): string | null {
  const digits = phone.replace(/\D/g, "");
  if (digits.length < 10) return null;
  return `https://wa.me/${digits.length <= 11 ? `55${digits}` : digits}`;
}

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
  const [confirmCancel, setConfirmCancel] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const start = new Date(appointment.startAt);
  const end = new Date(appointment.endAt);
  const status = appointment.status;
  const whatsapp = contact?.phone ? whatsappUrl(contact.phone) : null;

  const changeTo = (target: AppointmentStatus) => {
    if (target === "CANCELED") setConfirmCancel(true);
    else onChangeStatus(appointment.id, target);
  };

  return (
    <div className="flex w-80 flex-col gap-4 border-l bg-card p-4">
      <div className="flex items-start justify-between gap-2">
        <h3 className="text-lg font-semibold leading-tight">{appointment.title}</h3>
        <div className="flex shrink-0 items-center">
          {canDelete && (
            <Button
              variant="ghost"
              size="icon"
              aria-label="Excluir agendamento"
              title="Excluir agendamento"
              onClick={() => setConfirmDelete(true)}
            >
              <Trash2 className="h-4 w-4" />
            </Button>
          )}
          <Button variant="ghost" size="icon" aria-label="Fechar" onClick={onClose}>
            <X className="h-4 w-4" />
          </Button>
        </div>
      </div>

      <div className="flex items-center gap-2 text-sm text-muted-foreground">
        <Clock className="h-4 w-4 shrink-0" />
        <span>
          {format(start, "EEEE, dd 'de' MMMM", { locale: ptBR })} ·{" "}
          {format(start, "HH:mm", { locale: ptBR })} – {format(end, "HH:mm", { locale: ptBR })}
        </span>
      </div>

      <div className="space-y-2">
        <p className="text-xs font-medium text-muted-foreground">Situação do atendimento</p>
        <div className="flex flex-wrap gap-1.5" role="group" aria-label="Situação do atendimento">
          {QUICK_STATUSES.map((s) => {
            const active = status === s;
            return (
              <Button
                key={s}
                type="button"
                size="sm"
                variant={active ? "default" : "outline"}
                aria-pressed={active}
                className="h-7 rounded-full px-3 text-xs"
                disabled={!active && (!canUpdate || !canMoveTo(status, s))}
                onClick={() => !active && changeTo(s)}
              >
                {APPOINTMENT_STATUS_LABELS[s]}
              </Button>
            );
          })}
        </div>
      </div>

      <div className="grid grid-cols-2 gap-2">
        <Button
          type="button"
          variant={status === "COMPLETED" ? "default" : "outline"}
          aria-pressed={status === "COMPLETED"}
          className={cn(status === "COMPLETED" && "bg-emerald-600 hover:bg-emerald-600")}
          disabled={status !== "COMPLETED" && (!canUpdate || !canMoveTo(status, "COMPLETED"))}
          onClick={() => status !== "COMPLETED" && changeTo("COMPLETED")}
        >
          <CircleCheck className="h-4 w-4" />
          {APPOINTMENT_STATUS_LABELS.COMPLETED}
        </Button>
        <Button
          type="button"
          variant={status === "NO_SHOW" ? "destructive" : "outline"}
          aria-pressed={status === "NO_SHOW"}
          disabled={status !== "NO_SHOW" && (!canUpdate || !canMoveTo(status, "NO_SHOW"))}
          onClick={() => status !== "NO_SHOW" && changeTo("NO_SHOW")}
        >
          <CircleX className="h-4 w-4" />
          {APPOINTMENT_STATUS_LABELS.NO_SHOW}
        </Button>
      </div>

      {contact && (
        <div className="space-y-1 rounded-md border bg-muted/30 p-3">
          <div className="flex items-center gap-2 text-sm font-medium">
            <User className="h-4 w-4 shrink-0" />
            <Link href={`${ROUTES.CONTACTS}/${contact.id}`} className="hover:underline">
              {contact.firstName} {contact.lastName}
            </Link>
          </div>
          {contact.phone && (
            <div className="flex items-center gap-2 text-xs text-muted-foreground">
              <Phone className="h-3 w-3 shrink-0" />
              {contact.phone}
              {whatsapp && (
                <a
                  href={whatsapp}
                  target="_blank"
                  rel="noopener noreferrer"
                  aria-label="Conversar no WhatsApp"
                  title="Conversar no WhatsApp"
                  className="ml-auto text-emerald-600 hover:text-emerald-700"
                >
                  <MessageCircle className="h-4 w-4" />
                </a>
              )}
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

      <ConfirmDialog
        open={confirmCancel}
        onOpenChange={setConfirmCancel}
        title="Cancelar agendamento?"
        description="Um agendamento cancelado não pode ser reaberto nem remarcado."
        confirmLabel="Cancelar agendamento"
        cancelLabel="Voltar"
        variant="destructive"
        onConfirm={() => {
          onChangeStatus(appointment.id, "CANCELED");
          setConfirmCancel(false);
        }}
      />
      <ConfirmDialog
        open={confirmDelete}
        onOpenChange={setConfirmDelete}
        title="Excluir agendamento?"
        description="O agendamento será removido da agenda."
        confirmLabel="Excluir"
        variant="destructive"
        onConfirm={() => {
          onDelete(appointment.id);
          setConfirmDelete(false);
        }}
      />
    </div>
  );
}
