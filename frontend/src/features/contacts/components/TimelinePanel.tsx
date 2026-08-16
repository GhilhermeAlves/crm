"use client";

import type { TimelineEvent } from "../types/contact.types";

const EVENT_STYLE: Record<TimelineEvent["type"], { dot: string; accent: string }> = {
  ACTIVITY: { dot: "bg-blue-500", accent: "text-blue-600" },
  TASK_CREATED: { dot: "bg-slate-400", accent: "text-slate-600" },
  TASK_COMPLETED: { dot: "bg-emerald-500", accent: "text-emerald-600" },
  OPPORTUNITY_CREATED: { dot: "bg-indigo-500", accent: "text-indigo-600" },
  OPPORTUNITY_MOVED: { dot: "bg-purple-500", accent: "text-purple-600" },
  OPPORTUNITY_WON: { dot: "bg-emerald-500", accent: "text-emerald-600" },
  OPPORTUNITY_LOST: { dot: "bg-rose-500", accent: "text-rose-600" },
};

const formatDate = (iso: string): string =>
  new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(iso));

export function TimelinePanel({ events }: { events: TimelineEvent[] }) {
  if (events.length === 0) {
    return (
      <p className="py-6 text-center text-sm text-muted-foreground">
        Nenhum evento registrado para este contato.
      </p>
    );
  }

  return (
    <ol className="relative ml-3 space-y-6 border-l-2 border-border pl-5">
      {events.map((event) => {
        const style = EVENT_STYLE[event.type] ?? EVENT_STYLE.ACTIVITY;
        return (
          <li key={event.id} className="relative">
            <span
              className={`absolute -left-[26px] top-1 h-3 w-3 rounded-full ${style.dot} ring-2 ring-background`}
            />
            <div className="space-y-0.5">
              <p className={`text-sm font-medium ${style.accent}`}>{event.title}</p>
              {event.description && (
                <p className="text-xs text-muted-foreground">{event.description}</p>
              )}
              <p className="text-xs text-muted-foreground">{formatDate(event.occurredAt)}</p>
            </div>
          </li>
        );
      })}
    </ol>
  );
}
