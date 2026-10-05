"use client";

import { useMemo, useRef, useEffect, useState } from "react";
import {
  format,
  startOfWeek,
  addDays,
  addMinutes,
  isSameDay,
  differenceInMinutes,
  isToday,
  isWeekend,
  parseISO,
  startOfDay,
} from "date-fns";
import { ptBR } from "date-fns/locale";
import { cn } from "@/lib/utils";
import type { Appointment, ScheduleBlock } from "../types/scheduling.types";

const DEFAULT_START_HOUR = 0;
const DEFAULT_END_HOUR = 24;

interface Props {
  currentDate: Date;
  view: "day" | "week";
  appointments: Appointment[];
  blocks: ScheduleBlock[];
  onSlotClick?: (start: Date, end: Date) => void;
  onAppointmentClick?: (appointment: Appointment) => void;
  selectedAppointmentId?: string | null;
  memberColorMap?: Map<string, string>;
  startHour?: number;
  endHour?: number;
  showWeekends?: boolean;
}

function getEventPosition(startAt: string, endAt: string, hourHeight: number, rangeStart: number) {
  const start = parseISO(startAt);
  const end = parseISO(endAt);
  const startMinutes = start.getHours() * 60 + start.getMinutes() - rangeStart * 60;
  const duration = differenceInMinutes(end, start);
  return {
    top: Math.max(0, (startMinutes / 60) * hourHeight),
    height: Math.max(16, (duration / 60) * hourHeight),
  };
}

function HourLabels({ hourHeight, rangeStart, rangeEnd }: { hourHeight: number; rangeStart: number; rangeEnd: number }) {
  const hours = [];
  for (let h = rangeStart; h < rangeEnd; h++) {
    hours.push(
      <div key={h} className="relative" style={{ height: hourHeight }}>
        <span className="absolute -top-2 right-2 text-[10px] tabular-nums text-muted-foreground">
          {String(h).padStart(2, "0")}:00
        </span>
      </div>,
    );
  }
  if (rangeEnd <= 24) {
    hours.push(
      <div key={rangeEnd} className="relative" style={{ height: 0 }}>
        <span className="absolute -top-2 right-2 text-[10px] tabular-nums text-muted-foreground">
          {String(rangeEnd === 24 ? 0 : rangeEnd).padStart(2, "0")}:00
        </span>
      </div>,
    );
  }
  return <div className="w-12 shrink-0 border-r bg-card">{hours}</div>;
}

function NowLine({ hourHeight, rangeStart, totalHours }: { hourHeight: number; rangeStart: number; totalHours: number }) {
  const now = new Date();
  const minutes = now.getHours() * 60 + now.getMinutes() - rangeStart * 60;
  if (minutes < 0 || minutes > totalHours * 60) return null;
  const top = (minutes / 60) * hourHeight;
  return (
    <div className="pointer-events-none absolute left-0 right-0 z-20" style={{ top }}>
      <div className="flex items-center">
        <div className="-ml-1 h-2.5 w-2.5 rounded-full bg-destructive" />
        <div className="flex-1 border-t-2 border-destructive" />
      </div>
    </div>
  );
}

function FiveMinuteLines({ hourHeight, rangeStart, rangeEnd }: { hourHeight: number; rangeStart: number; rangeEnd: number }) {
  const lines = [];
  const slotHeight = hourHeight / 12;
  for (let h = rangeStart; h < rangeEnd; h++) {
    const slots = [];
    for (let s = 0; s < 12; s++) {
      const isHourEnd = s === 11;
      const isHalf = s === 5;
      slots.push(
        <div
          key={s}
          className={cn(
            "absolute inset-x-0 border-b",
            isHourEnd
              ? "border-border/40"
              : isHalf
                ? "border-dashed border-border/25"
                : "border-border/10",
          )}
          style={{ top: (s + 1) * slotHeight }}
        />,
      );
    }
    lines.push(
      <div key={h} className="relative" style={{ height: hourHeight }}>
        {slots}
      </div>,
    );
  }
  return <>{lines}</>;
}


function DayColumn({
  date,
  appointments,
  blocks,
  onSlotClick,
  onAppointmentClick,
  selectedAppointmentId,
  memberColorMap,
  hourHeight,
  rangeStart,
  rangeEnd,
  totalHours,
}: {
  date: Date;
  appointments: Appointment[];
  blocks: ScheduleBlock[];
  onSlotClick?: (start: Date, end: Date) => void;
  onAppointmentClick?: (appointment: Appointment) => void;
  selectedAppointmentId?: string | null;
  memberColorMap?: Map<string, string>;
  hourHeight: number;
  rangeStart: number;
  rangeEnd: number;
  totalHours: number;
}) {
  const colRef = useRef<HTMLDivElement>(null);

  const dayAppointments = appointments.filter((a) => isSameDay(parseISO(a.startAt), date));
  const dayBlocks = blocks.filter((b) => isSameDay(parseISO(b.startAt), date));

  const handleClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (!onSlotClick || !colRef.current) return;
    const rect = colRef.current.getBoundingClientRect();
    const y = e.clientY - rect.top;
    const slotMinutes = 5;
    const minutes =
      Math.round(((y / hourHeight) * 60) / slotMinutes) * slotMinutes + rangeStart * 60;
    const start = addMinutes(startOfDay(date), minutes);
    const end = addMinutes(start, 30);
    onSlotClick(start, end);
  };

  return (
    <div
      ref={colRef}
      className="relative flex-1 cursor-pointer border-r last:border-r-0"
      style={{ height: totalHours * hourHeight }}
      onClick={handleClick}
    >
      <FiveMinuteLines hourHeight={hourHeight} rangeStart={rangeStart} rangeEnd={rangeEnd} />

      {isToday(date) && <NowLine hourHeight={hourHeight} rangeStart={rangeStart} totalHours={totalHours} />}

      {dayBlocks.map((block) => {
        const pos = getEventPosition(block.startAt, block.endAt, hourHeight, rangeStart);
        return (
          <div
            key={block.id}
            className="absolute left-0.5 right-0.5 z-10 overflow-hidden rounded border border-dashed border-muted-foreground/30 bg-muted/60 px-1 py-0.5 text-[10px] text-muted-foreground"
            style={{ top: pos.top, height: pos.height }}
            onClick={(e) => e.stopPropagation()}
          >
            <span className="block truncate">{block.reason || "Bloqueio"}</span>
          </div>
        );
      })}

      {dayAppointments.map((appt) => {
        const pos = getEventPosition(appt.startAt, appt.endAt, hourHeight, rangeStart);
        const isSelected = selectedAppointmentId === appt.id;
        const memberColor = memberColorMap?.get(appt.hostId);
        return (
          <button
            key={appt.id}
            className={cn(
              "absolute left-0.5 right-0.5 z-10 overflow-hidden rounded px-1 py-0.5 text-left text-[11px] font-medium shadow-sm transition-shadow",
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
            <span className="block truncate leading-tight">{appt.title}</span>
            {pos.height > 28 && (
              <span className="block truncate text-[10px] opacity-80">
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
  startHour = DEFAULT_START_HOUR,
  endHour = DEFAULT_END_HOUR,
  showWeekends = true,
}: Props) {
  const gridRef = useRef<HTMLDivElement>(null);
  const [containerHeight, setContainerHeight] = useState(0);

  const rangeStart = Math.max(0, Math.min(23, startHour));
  const rangeEnd = Math.max(rangeStart + 1, Math.min(24, endHour));
  const totalHours = rangeEnd - rangeStart;

  const days = useMemo(() => {
    if (view === "day") return [currentDate];
    const start = startOfWeek(currentDate, { weekStartsOn: 1 });
    const allDays = Array.from({ length: 7 }, (_, i) => addDays(start, i));
    return showWeekends ? allDays : allDays.filter((d) => !isWeekend(d));
  }, [currentDate, view, showWeekends]);

  useEffect(() => {
    const el = gridRef.current;
    if (!el) return;
    const observer = new ResizeObserver(([entry]) => {
      setContainerHeight(entry.contentRect.height);
    });
    observer.observe(el);
    return () => observer.disconnect();
  }, []);

  const MIN_HOUR_HEIGHT = 48;
  const fitsInView = containerHeight > 0 && containerHeight / totalHours >= MIN_HOUR_HEIGHT;
  const hourHeight = fitsInView ? containerHeight / totalHours : MIN_HOUR_HEIGHT;

  return (
    <div ref={gridRef} className="flex flex-1 flex-col overflow-y-auto rounded-lg border bg-card">
      {/* Header — sticky so it stays visible while scrolling */}
      <div className="sticky top-0 z-10 flex border-b bg-muted/30">
        <div className="w-12 shrink-0 border-r bg-muted/30" />
        {days.map((d) => {
          const today = isToday(d);
          return (
            <div
              key={d.toISOString()}
              className={cn(
                "flex-1 border-r py-1.5 text-center text-xs last:border-r-0",
                today ? "bg-primary/10" : "bg-muted/30",
              )}
            >
              <span className="text-muted-foreground">
                {format(d, "EEE.", { locale: ptBR })}{" "}
              </span>
              <span
                className={cn(
                  "inline-flex h-6 w-6 items-center justify-center rounded-full font-medium tabular-nums",
                  today && "bg-primary text-primary-foreground",
                )}
              >
                {format(d, "d")}
              </span>
            </div>
          );
        })}
      </div>

      {/* Grid body */}
      <div className="flex flex-1">
        <HourLabels hourHeight={hourHeight} rangeStart={rangeStart} rangeEnd={rangeEnd} />
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
            hourHeight={hourHeight}
            rangeStart={rangeStart}
            rangeEnd={rangeEnd}
            totalHours={totalHours}
          />
        ))}
      </div>
    </div>
  );
}
