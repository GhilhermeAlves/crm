"use client";

import { format, isSameDay, parseISO } from "date-fns";
import { cn } from "@/lib/utils";
import type { Appointment } from "@/features/sales/scheduling/types/scheduling.types";
import type { Chair } from "../types/chair.types";

type Props = {
  date: Date;
  chairs: Chair[];
  appointments: Appointment[];
  onAppointmentClick: (appointment: Appointment) => void;
  selectedAppointmentId?: string | null;
  memberColorMap?: Map<string, string>;
};

/** Agrupa as consultas do dia por cadeira ativa; as sem cadeira vão para a última coluna. */
export function groupByChair(
  date: Date,
  chairs: Chair[],
  appointments: Appointment[],
): { key: string; title: string; color: string | null; items: Appointment[] }[] {
  const ofDay = appointments
    .filter((a) => isSameDay(parseISO(a.startAt), date))
    .sort((a, b) => a.startAt.localeCompare(b.startAt));
  const active = chairs
    .filter((c) => c.active)
    .sort((a, b) => a.sortOrder - b.sortOrder || a.name.localeCompare(b.name));
  const activeIds = new Set(active.map((c) => c.id));
  const columns = active.map((c) => ({
    key: c.id,
    title: c.name,
    color: c.color,
    items: ofDay.filter((a) => a.chairId === c.id),
  }));
  const withoutChair = ofDay.filter((a) => !a.chairId || !activeIds.has(a.chairId));
  if (withoutChair.length > 0) {
    columns.push({ key: "none", title: "Sem cadeira", color: null, items: withoutChair });
  }
  return columns;
}

export function ChairsDayView({
  date,
  chairs,
  appointments,
  onAppointmentClick,
  selectedAppointmentId,
  memberColorMap,
}: Props) {
  const columns = groupByChair(date, chairs, appointments);

  if (chairs.filter((c) => c.active).length === 0) {
    return (
      <div className="flex flex-1 items-center justify-center p-6 text-center text-sm text-muted-foreground">
        Nenhuma cadeira ativa. Cadastre as cadeiras em Configurações → Agenda → Cadeiras.
      </div>
    );
  }

  return (
    <div className="flex flex-1 gap-2 overflow-x-auto rounded-lg border bg-card p-2">
      {columns.map((col) => (
        <section
          key={col.key}
          aria-label={col.title}
          className="flex w-56 shrink-0 flex-col rounded-md border bg-muted/20"
        >
          <header className="flex items-center gap-2 border-b px-3 py-2 text-sm font-medium">
            <span
              className="h-2.5 w-2.5 rounded-full"
              style={{ backgroundColor: col.color ?? "hsl(var(--muted-foreground))" }}
            />
            {col.title}
            <span className="ml-auto text-xs text-muted-foreground">{col.items.length}</span>
          </header>
          <div className="flex-1 space-y-1.5 overflow-y-auto p-2">
            {col.items.length === 0 && (
              <p className="py-4 text-center text-xs text-muted-foreground">Livre</p>
            )}
            {col.items.map((appt) => (
              <button
                key={appt.id}
                type="button"
                onClick={() => onAppointmentClick(appt)}
                className={cn(
                  "w-full rounded-md border-l-4 bg-card px-2 py-1.5 text-left text-xs shadow-sm hover:bg-muted/40",
                  selectedAppointmentId === appt.id && "ring-2 ring-ring",
                  appt.status === "CANCELED" && "line-through opacity-50",
                )}
                style={{
                  borderLeftColor: memberColorMap?.get(appt.hostId) ?? "hsl(var(--primary))",
                }}
              >
                <span className="block tabular-nums text-muted-foreground">
                  {format(parseISO(appt.startAt), "HH:mm")} –{" "}
                  {format(parseISO(appt.endAt), "HH:mm")}
                </span>
                <span className="block truncate font-medium">{appt.title}</span>
              </button>
            ))}
          </div>
        </section>
      ))}
    </div>
  );
}
