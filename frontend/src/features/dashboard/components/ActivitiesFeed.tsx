import { ArrowRight, Clock } from "lucide-react";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { ROUTES } from "@/lib/constants";
import type { RecentActivity } from "../types/dashboard.types";

const activityDotColors: Record<string, string> = {
  CREATE: "bg-emerald-500",
  UPDATE: "bg-blue-500",
  DELETE: "bg-red-500",
  LOGIN: "bg-green-500",
  LOGOUT: "bg-gray-500",
};

function getActivityColor(action: string): string {
  return activityDotColors[action] || "bg-blue-500";
}

function formatTimeAgo(createdAt: string): string {
  const now = new Date();
  const date = new Date(createdAt);
  const diffMs = now.getTime() - date.getTime();
  const diffMin = Math.floor(diffMs / 60000);

  if (diffMin < 1) return "Agora mesmo";
  if (diffMin < 60) return `Há ${diffMin} min`;
  const diffHours = Math.floor(diffMin / 60);
  if (diffHours < 24) return `Há ${diffHours} hora${diffHours > 1 ? "s" : ""}`;
  const diffDays = Math.floor(diffHours / 24);
  if (diffDays < 7) return `Há ${diffDays} dia${diffDays > 1 ? "s" : ""}`;
  return date.toLocaleDateString("pt-BR");
}

type ActivitiesFeedProps = {
  activities: RecentActivity[];
};

export function ActivitiesFeed({ activities }: ActivitiesFeedProps) {
  if (activities.length === 0) {
    return (
      <Card>
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-4">
          <CardTitle className="text-base font-semibold">Atividades Recentes</CardTitle>
          <Button variant="ghost" size="sm" className="text-xs text-muted-foreground" asChild>
            <Link href={ROUTES.REPORTS}>
              Ver todas
              <ArrowRight className="ml-1 h-3 w-3" />
            </Link>
          </Button>
        </CardHeader>
        <CardContent>
          <div className="flex flex-col items-center justify-center py-8 text-center text-sm text-muted-foreground">
            <Clock className="mb-2 h-8 w-8" />
            <p>Nenhuma atividade registrada ainda</p>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-4">
        <CardTitle className="text-base font-semibold">Atividades Recentes</CardTitle>
        <Button variant="ghost" size="sm" className="text-xs text-muted-foreground" asChild>
          <Link href={ROUTES.REPORTS}>
            Ver todas
            <ArrowRight className="ml-1 h-3 w-3" />
          </Link>
        </Button>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {activities.map((activity) => (
            <div key={activity.id} className="flex items-start gap-3">
              <div className={`mt-1 h-2 w-2 shrink-0 rounded-full ${getActivityColor(activity.action)}`} />
              <div className="flex-1 space-y-1">
                <p className="text-sm">
                  {activity.userName && (
                    <span className="font-medium">{activity.userName}</span>
                  )}
                  {activity.description && (
                    <span> {activity.description}</span>
                  )}
                </p>
                <p className="text-xs text-muted-foreground">
                  {formatTimeAgo(activity.createdAt)} &middot; {activity.module}
                </p>
              </div>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}
