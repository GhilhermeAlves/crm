"use client";

import { useMemo, type ReactNode } from "react";
import {
  Phone,
  Users,
  Mail,
  MessageSquare,
  StickyNote,
  FileText,
  Repeat,
  CircleDot,
  History,
} from "lucide-react";
import type { Activity, ActivityType } from "@/features/activities/types/activity.types";
import { ACTIVITY_TYPE_LABELS } from "@/features/activities/types/activity.types";
import { CrmRecentCard, type CrmRecentItem } from "./CrmRecentCard";
import { EmptyState } from "@/components/common/EmptyState";
import { ROUTES } from "@/lib/constants";

const MAX_ITEMS = 4;

const TYPE_ICONS: Record<ActivityType, ReactNode> = {
  CALL: <Phone className="h-4 w-4 text-blue-500" />,
  MEETING: <Users className="h-4 w-4 text-indigo-500" />,
  EMAIL: <Mail className="h-4 w-4 text-amber-500" />,
  MESSAGE: <MessageSquare className="h-4 w-4 text-cyan-500" />,
  NOTE: <StickyNote className="h-4 w-4 text-slate-500" />,
  PROPOSAL: <FileText className="h-4 w-4 text-rose-500" />,
  FOLLOW_UP: <Repeat className="h-4 w-4 text-emerald-500" />,
  OTHER: <CircleDot className="h-4 w-4 text-muted-foreground" />,
};

function formatTimeLabel(iso: string): string {
  const minutes = Math.round((Date.now() - new Date(iso).getTime()) / 60000);
  if (minutes < 1) return "agora mesmo";
  if (minutes < 60) return `há ${minutes} min`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `há ${hours} h`;
  const days = Math.floor(hours / 24);
  if (days === 1) return "há 1 dia";
  return `há ${days} dias`;
}

export function CrmRecentItems({ activities }: { activities: Activity[] }) {
  const items = useMemo<CrmRecentItem[]>(() => {
    return activities
      .slice()
      .sort((a, b) => new Date(b.activityAt).getTime() - new Date(a.activityAt).getTime())
      .slice(0, MAX_ITEMS)
      .map((activity) => ({
        id: activity.id,
        title: activity.subject,
        subtitle: ACTIVITY_TYPE_LABELS[activity.type] ?? activity.type,
        icon: TYPE_ICONS[activity.type] ?? <CircleDot className="h-4 w-4 text-muted-foreground" />,
        timeLabel: formatTimeLabel(activity.activityAt),
        href: ROUTES.ACTIVITIES,
      }));
  }, [activities]);

  if (items.length === 0) {
    return (
      <EmptyState
        icon={<History className="h-8 w-8 text-muted-foreground" />}
        title="Nenhum registro recente"
        description="As últimas atividades comerciais aparecerão aqui."
        className="rounded-xl border border-border/60 py-10"
      />
    );
  }

  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      {items.map((item) => (
        <CrmRecentCard key={item.id} item={item} />
      ))}
    </div>
  );
}
