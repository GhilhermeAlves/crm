"use client";

import { useState } from "react";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { PageTitle } from "@/components/common/PageTitle";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { CheckCheck, Inbox } from "lucide-react";
import {
  useNotifications,
  useUnreadCount,
  useMarkNotificationRead,
  useMarkAllNotificationsRead,
  useNotificationPermissions,
} from "@/features/notifications/hooks/useNotifications";
import { formatRelativeTime } from "@/features/notifications/lib/format";
import type { Notification, NotificationType } from "@/features/notifications/types/notification.types";

const TYPE_STYLE: Record<NotificationType, string> = {
  TASK: "bg-blue-100 text-blue-700 dark:bg-blue-900/40 dark:text-blue-300",
  WORKFLOW: "bg-purple-100 text-purple-700 dark:bg-purple-900/40 dark:text-purple-300",
  INVITATION: "bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-300",
  MESSAGE: "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-300",
  LEAD: "bg-orange-100 text-orange-700 dark:bg-orange-900/40 dark:text-orange-300",
  OPPORTUNITY: "bg-teal-100 text-teal-700 dark:bg-teal-900/40 dark:text-teal-300",
  SYSTEM: "bg-slate-200 text-slate-700 dark:bg-slate-800 dark:text-slate-300",
  INFO: "bg-muted text-muted-foreground",
};

const TYPE_LABEL: Record<NotificationType, string> = {
  TASK: "Tarefa",
  WORKFLOW: "Automação",
  INVITATION: "Convite",
  MESSAGE: "Mensagem",
  LEAD: "Lead",
  OPPORTUNITY: "Negócio",
  SYSTEM: "Sistema",
  INFO: "Info",
};

function NotificationRow({
  notification,
  onRead,
  canUpdate,
}: {
  notification: Notification;
  onRead: (id: string) => void;
  canUpdate: boolean;
}) {
  return (
    <button
      type="button"
      onClick={() => {
        if (!notification.read && canUpdate) onRead(notification.id);
      }}
      className={`flex w-full cursor-pointer flex-col gap-1 rounded-lg border p-4 text-left transition-colors hover:bg-accent ${
        notification.read ? "border-border" : "border-primary/30 bg-muted/40"
      }`}
    >
      <div className="flex w-full items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <span
            className={`rounded px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide ${TYPE_STYLE[notification.type] ?? TYPE_STYLE.INFO}`}
          >
            {TYPE_LABEL[notification.type] ?? notification.type}
          </span>
          {!notification.read && <span className="h-2 w-2 rounded-full bg-primary" />}
        </div>
        <span className="shrink-0 text-xs text-muted-foreground">
          {formatRelativeTime(notification.createdAt)}
        </span>
      </div>
      <p className="text-sm font-medium">{notification.title}</p>
      {notification.body && <p className="text-sm text-muted-foreground">{notification.body}</p>}
    </button>
  );
}

export default function NotificationsPage() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const perms = useNotificationPermissions();
  const [onlyUnread, setOnlyUnread] = useState(false);

  const { data: notifications = [], isLoading } = useNotifications(companyId);
  const { data: unread = 0 } = useUnreadCount(companyId);
  const markRead = useMarkNotificationRead(companyId);
  const markAllRead = useMarkAllNotificationsRead(companyId);

  const visible = onlyUnread ? notifications.filter((n) => !n.read) : notifications;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <PageTitle>Notificações</PageTitle>
        {perms.canUpdate && unread > 0 && (
          <Button variant="outline" onClick={() => markAllRead.mutate()}>
            <CheckCheck className="mr-2 h-4 w-4" />
            Marcar todas como lidas
          </Button>
        )}
      </div>

      {perms.canUpdate && (
        <div className="flex items-center gap-2">
          <Button
            variant={onlyUnread ? "secondary" : "ghost"}
            size="sm"
            onClick={() => setOnlyUnread(false)}
          >
            Todas
          </Button>
          <Button
            variant={onlyUnread ? "ghost" : "secondary"}
            size="sm"
            onClick={() => setOnlyUnread(true)}
          >
            Não lidas ({unread})
          </Button>
        </div>
      )}

      <Card>
        <CardHeader>
          <CardTitle className="text-base font-semibold">Histórico de notificações</CardTitle>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="space-y-2">
              {Array.from({ length: 3 }).map((_, i) => (
                <div key={i} className="h-20 animate-pulse rounded-lg bg-muted" />
              ))}
            </div>
          ) : visible.length === 0 ? (
            <div className="flex flex-col items-center gap-2 py-10 text-center">
              <Inbox className="h-10 w-10 text-muted-foreground" />
              <p className="text-sm text-muted-foreground">
                {onlyUnread ? "Nenhuma notificação não lida" : "Nenhuma notificação"}
              </p>
            </div>
          ) : (
            <div className="space-y-2">
              {visible.map((n) => (
                <NotificationRow
                  key={n.id}
                  notification={n}
                  canUpdate={perms.canUpdate}
                  onRead={(id) => markRead.mutate(id)}
                />
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}