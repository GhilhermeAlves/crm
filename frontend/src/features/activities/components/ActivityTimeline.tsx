"use client";

import { format } from "date-fns";
import { ptBR } from "date-fns/locale";
import type { Activity } from "../types/activity.types";
import { ACTIVITY_TYPE_ICONS, ACTIVITY_TYPE_LABELS } from "../types/activity.types";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Badge } from "@/components/ui/badge";

type Props = {
  activities: Activity[];
  isLoading?: boolean;
};

export function ActivityTimeline({ activities, isLoading }: Props) {
  if (isLoading) {
    return <p className="py-6 text-center text-sm text-muted-foreground">Carregando…</p>;
  }

  if (activities.length === 0) {
    return (
      <p className="py-6 text-center text-sm text-muted-foreground">
        Nenhuma atividade registrada ainda.
      </p>
    );
  }

  const sorted = [...activities].sort(
    (a, b) => new Date(b.activityAt).getTime() - new Date(a.activityAt).getTime()
  );

  return (
    <ScrollArea className="max-h-[420px] pr-3">
      <ol className="relative space-y-4 border-l border-border pl-5">
        {sorted.map((activity) => (
          <li key={activity.id} className="relative">
            <span className="absolute -left-[27px] top-1 flex h-4 w-4 items-center justify-center rounded-full bg-background text-xs ring-1 ring-border">
              {ACTIVITY_TYPE_ICONS[activity.type]}
            </span>
            <div className="space-y-1">
              <div className="flex items-center justify-between gap-2">
                <p className="text-sm font-medium">{activity.subject}</p>
                <span className="text-xs text-muted-foreground">
                  {format(new Date(activity.activityAt), "dd/MM 'às' HH:mm", { locale: ptBR })}
                </span>
              </div>
              {activity.description && (
                <p className="text-sm text-muted-foreground">{activity.description}</p>
              )}
              <Badge variant="secondary">{ACTIVITY_TYPE_LABELS[activity.type]}</Badge>
            </div>
          </li>
        ))}
      </ol>
    </ScrollArea>
  );
}