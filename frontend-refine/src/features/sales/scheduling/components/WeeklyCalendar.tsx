"use client";

import { useMemo, useRef, useEffect, useCallback } from "react";
import {
  format,
  startOfWeek,
  addDays,
  addMinutes,
  isSameDay,
  differenceInMinutes,
  isToday,
  parseISO,
  startOfDay,
} from "date-fns";
import { ptBR } from "date-fns/locale";
import { cn } from "@/lib/utils";
import type { Appointment, ScheduleBlock } from "../types/scheduling.types";

const START_HOUR = 0;
const END_HOUR = 24;
const TOTAL_HOURS = END_HOUR - START_HOUR;
const SCROLL_TO_HOUR = 7;

interface Props {
  currentDate: Date;
  view: "day" | "week";
  appointments: Appointment[];
  blocks: ScheduleBlock[];
  onSlotClick?: (start: Date, end: Date) => void;
  onAppointmentClick?: (appointment: Appointment) => void;
  selectedAppointmentId?: string | null;
  memberColorMap?: Map<string, string>;
  hourHeight: number;
  onZoom?: (delta: number) => void;
}

function getEventPosition(startAt: string, endAt: string, hourHeight: number) {
  const start = parseISO(startAt);
  const end = parseISO(endAt);
  const startMinutes = start.getHours() * 60 + start.getMinutes() - START_HOUR * 60;
  const duration = differenceInMinutes(end, start);
  return {
    top: Math.max(0, (startMinutes / 60) * hourHeight),
    height: Math.max(16, (duration / 60) * hourHeight),
  };
}

function HourLabels({ hourHeight }: { hourHeight: number }) {
  const hours = [];
  for (let h = START_HOUR; h < END_HOUR; h++) {
    hours.push(
      <div key={h} className="relative" style={{ height: hourHeight }}>
        <span className="absolute -top-2 right-2 text-[10px] tabular-nums text-muted-foreground">
          {String(h).padStart(2, "0")}:00
        </span>
      </div>,
    );
  }
  return <div className="w-12 shrink-0 border-r bg-card">{hours}</div>;
}

function NowLine({ hourHeight }: { hourHeight: number }) {
  const now = new Date();
  const minutes = now.getHours() * 60 + now.getMinutes() - START_HOUR * 60;
  if (minutes < 0 || minutes > TOTAL_HOURS * 60) return null;
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

function HalfHourLines({ hourHeight }: { hourHeight: number }) {
  const lines = [];
  for (let h = START_HOUR; h < END_HOUR; h++) {
    lines.push(
      <div key={h} className="relative" style={{ height: hourHeight }}>
        <div className="absolute inset-x-0 bottom-0 border-b border-border/40" />
        <div
          className="absolute inset-x-0 border-b border-dashed border-border/20"
          style={{ top: hourHeight / 2 }}
        />
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
}: {
  date: Date;
  appointments: Appointment[];
  blocks: ScheduleBlock[];
  onSlotClick?: (start: Date, end: Date) => void;
  onAppointmentClick?: (appointment: Appointment) => void;
  selectedAppointmentId?: string | null;
  memberColorMap?: Map<string, string>;
  hourHeight: number;
}) {
  const colRef = useRef<HTMLDivElement>(null);

  const dayAppointments = appointments.filter((a) => isSameDay(parseISO(a.startAt), date));
  const dayBlocks = blocks.filter((b) => isSameDay(parseISO(b.startAt), date));

  const handleClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (!onSlotClick || !colRef.current) return;
    const rect = colRef.current.getBoundingClientRect();
    const y = e.clientY - rect.top + colRef.current.scrollTop;
    const slotMinutes = hourHeight >= 80 ? 5 : hourHeight >= 40 ? 15 : 30;
    const minutes =
      Math.round(((y / hourHeight) * 60) / slotMinutes) * slotMinutes + START_HOUR * 60;
    const start = addMinutes(startOfDay(date), minutes);
    const end = addMinutes(start, 30);
    onSlotClick(start, end);
  };

  return (
    <div
      ref={colRef}
      className="relative flex-1 cursor-pointer border-r last:border-r-0"
      style={{ height: TOTAL_HOURS * hourHeight }}
      onClick={handleClick}
    >
      <HalfHourLines hourHeight={hourHeight} />

      {isToday(date) && <NowLine hourHeight={hourHeight} />}

      {dayBlocks.map((block) => {
        const pos = getEventPosition(block.startAt, block.endAt, hourHeight);
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
        const pos = getEventPosition(appt.startAt, appt.endAt, hourHeight);
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
  hourHeight,
  onZoom,
}: Props) {
  const scrollRef = useRef<HTMLDivElement>(null);
  const didScroll = useRef(false);

  const days = useMemo(() => {
    if (view === "day") return [currentDate];
    const start = startOfWeek(currentDate, { weekStartsOn: 1 });
    return Array.from({ length: 7 }, (_, i) => addDays(start, i));
  }, [currentDate, view]);

  useEffect(() => {
    if (scrollRef.current && !didScroll.current) {
      scrollRef.current.scrollTop = SCROLL_TO_HOUR * hourHeight;
      didScroll.current = true;
    }
  }, [hourHeight]);

  const handleWheel = useCallback(
    (e: React.WheelEvent) => {
      if (e.ctrlKey && onZoom) {
        e.preventDefault();
        onZoom(e.deltaY > 0 ? -1 : 1);
      }
    },
    [onZoom],
  );

  return (
    <div className="flex flex-1 flex-col overflow-hidden rounded-lg border bg-card">
      <div className="flex shrink-0 border-b bg-muted/30">
        <div className="w-12 shrink-0" />
        {days.map((d) => {
          const today = isToday(d);
          return (
            <div
              key={d.toISOString()}
              className={cn(
                "flex-1 border-r py-1.5 text-center text-xs last:border-r-0",
                today && "bg-primary/10",
              )}
            >
              <span className="text-muted-foreground">{format(d, "EEE", { locale: ptBR })} </span>
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

      <div
        ref={scrollRef}
        className="flex flex-1 overflow-y-auto overflow-x-hidden"
        onWheel={handleWheel}
      >
        <HourLabels hourHeight={hourHeight} />
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
          />
        ))}
      </div>
    </div>
  );
}
