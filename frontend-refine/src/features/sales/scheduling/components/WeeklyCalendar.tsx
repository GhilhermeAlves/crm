"use client";

import { useMemo, useRef } from "react";
import {
  format,
  startOfWeek,
  endOfWeek,
  startOfDay,
  addDays,
  addMinutes,
  isSameDay,
  differenceInMinutes,
  isToday,
  parseISO,
} from "date-fns";
import { ptBR } from "date-fns/locale";
import { cn } from "@/lib/utils";
import type { Appointment, ScheduleBlock } from "../types/scheduling.types";
import { APPOINTMENT_STATUS_COLORS, APPOINTMENT_STATUS_LABELS } from "../types/scheduling.types";

const HOUR_HEIGHT = 60;
const START_HOUR = 6;
const END_HOUR = 22;
const TOTAL_HOURS = END_HOUR - START_HOUR;

interface Props {
  currentDate: Date;
  view: "day" | "week";
  appointments: Appointment[];
  blocks: ScheduleBlock[];
  onSlotClick?: (start: Date, end: Date) => void;
  onAppointmentClick?: (appointment: Appointment) => void;
  selectedAppointmentId?: string | null;
  memberColorMap?: Map<string, string>;
}

function getEventPosition(startAt: string, endAt: string) {
  const start = parseISO(startAt);
  const end = parseISO(endAt);
  const startMinutes = start.getHours() * 60 + start.getMinutes() - START_HOUR * 60;
  const duration = differenceInMinutes(end, start);
  return {
    top: Math.max(0, (startMinutes / 60) * HOUR_HEIGHT),
    height: Math.max(20, (duration / 60) * HOUR_HEIGHT),
  };
}

function HourLabels() {
  const hours = [];
  for (let h = START_HOUR; h < END_HOUR; h++) {
    hours.push(
      <div key={h} className="relative" style={{ height: HOUR_HEIGHT }}>
        <span className="absolute -top-2.5 right-2 text-xs tabular-nums text-muted-foreground">
          {String(h).padStart(2, "0")}:00
        </span>
      </div>,
    );
  }
  return <div className="w-14 shrink-0 border-r">{hours}</div>;
}

function NowLine() {
  const now = new Date();
  const minutes = now.getHours() * 60 + now.getMinutes() - START_HOUR * 60;
  if (minutes < 0 || minutes > TOTAL_HOURS * 60) return null;
  const top = (minutes / 60) * HOUR_HEIGHT;
  return (
    <div className="pointer-events-none absolute left-0 right-0 z-20" style={{ top }}>
      <div className="flex items-center">
        <div className="-ml-1 h-2.5 w-2.5 rounded-full bg-destructive" />
        <div className="flex-1 border-t-2 border-destructive" />
      </div>
    </div>
  );
}

function DayColumn({
  date,
  appointments,
  blocks,
  onSlotClick,
  onAppointmentClick,
  selectedAppointmentId,
  memberColorMap,
}: {
  date: Date;
  appointments: Appointment[];
  blocks: ScheduleBlock[];
  onSlotClick?: (start: Date, end: Date) => void;
  onAppointmentClick?: (appointment: Appointment) => void;
  selectedAppointmentId?: string | null;
  memberColorMap?: Map<string, string>;
}) {
  const colRef = useRef<HTMLDivElement>(null);

  const dayAppointments = appointments.filter((a) => isSameDay(parseISO(a.startAt), date));
  const dayBlocks = blocks.filter((b) => isSameDay(parseISO(b.startAt), date));

  const handleClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (!onSlotClick || !colRef.current) return;
    const rect = colRef.current.getBoundingClientRect();
    const y = e.clientY - rect.top;
    const minutes = Math.round(((y / HOUR_HEIGHT) * 60) / 15) * 15 + START_HOUR * 60;
    const start = addMinutes(startOfDay(date), minutes);
    const end = addMinutes(start, 30);
    onSlotClick(start, end);
  };

  return (
    <div
      ref={colRef}
      className="relative flex-1 cursor-pointer border-r last:border-r-0"
      style={{ height: TOTAL_HOURS * HOUR_HEIGHT }}
      onClick={handleClick}
    >
      {Array.from({ length: TOTAL_HOURS }).map((_, i) => (
        <div key={i} className="border-b border-dashed" style={{ height: HOUR_HEIGHT }} />
      ))}

      {isToday(date) && <NowLine />}

      {dayBlocks.map((block) => {
        const pos = getEventPosition(block.startAt, block.endAt);
        return (
          <div
            key={block.id}
            className="absolute left-1 right-1 z-10 overflow-hidden rounded border border-dashed border-muted-foreground/30 bg-muted/60 px-1.5 py-0.5 text-xs text-muted-foreground"
            style={{ top: pos.top, height: pos.height }}
            onClick={(e) => e.stopPropagation()}
          >
            <span className="block truncate">
              {block.source === "GOOGLE" ? "🔗 " : ""}
              {block.reason || "Bloqueio"}
            </span>
          </div>
        );
      })}

      {dayAppointments.map((appt) => {
        const pos = getEventPosition(appt.startAt, appt.endAt);
        const isSelected = selectedAppointmentId === appt.id;
        const memberColor = memberColorMap?.get(appt.hostId);
        return (
          <button
            key={appt.id}
            className={cn(
              "absolute left-1 right-1 z-10 overflow-hidden rounded px-1.5 py-0.5 text-left text-xs font-medium transition-shadow",
              !memberColor && "bg-primary/90 text-primary-foreground hover:bg-primary",
              isSelected && "ring-2 ring-ring ring-offset-1",
            )}
            style={{
              top: pos.top,
              height: pos.height,
              ...(memberColor ? { backgroundColor: memberColor, color: "#fff" } : {}),
            }}
            onClick={(e) => {
              e.stopPropagation();
              onAppointmentClick?.(appt);
            }}
          >
            <span className="block truncate">{appt.title}</span>
            {pos.height > 30 && (
              <span className="block truncate opacity-80">
                {format(parseISO(appt.startAt), "HH:mm")} – {format(parseISO(appt.endAt), "HH:mm")}
              </span>
            )}
          </button>
        );
      })}
    </div>
  );
}

export function WeeklyCalendar({
  currentDate,
  view,
  appointments,
  blocks,
  onSlotClick,
  onAppointmentClick,
  selectedAppointmentId,
  memberColorMap,
}: Props) {
  const days = useMemo(() => {
    if (view === "day") return [currentDate];
    const start = startOfWeek(currentDate, { weekStartsOn: 1 });
    return Array.from({ length: 7 }, (_, i) => addDays(start, i));
  }, [currentDate, view]);

  return (
    <div className="flex flex-1 flex-col overflow-hidden rounded-lg border bg-card">
      <div className="flex border-b">
        <div className="w-14 shrink-0" />
        {days.map((d) => (
          <div
            key={d.toISOString()}
            className={cn(
              "flex-1 border-r py-2 text-center text-sm font-medium last:border-r-0",
              isToday(d) && "bg-primary/10 text-primary",
            )}
          >
            <span className="hidden sm:inline">{format(d, "EEE", { locale: ptBR })} </span>
            <span className={cn("tabular-nums", isToday(d) && "font-bold")}>{format(d, "dd")}</span>
          </div>
        ))}
      </div>

      <div className="flex flex-1 overflow-y-auto">
        <HourLabels />
        {days.map((d) => (
          <DayColumn
            key={d.toISOString()}
            date={d}
            appointments={appointments}
            blocks={blocks}
            onSlotClick={onSlotClick}
            onAppointmentClick={onAppointmentClick}
            selectedAppointmentId={selectedAppointmentId}
            memberColorMap={memberColorMap}
          />
        ))}
      </div>
    </div>
  );
}
