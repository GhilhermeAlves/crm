"use client";

import { Bell, CheckCheck, ChevronRight } from "lucide-react";
import Link from "next/link";
import { useAuth } from "@/features/auth/hooks/useAuth";
import {
  useNotifications,
  useUnreadCount,
  useMarkNotificationRead,
  useMarkAllNotificationsRead,
  useNotificationPermissions,
} from "@/features/notifications/hooks/useNotifications";
import { formatRelativeTime } from "@/features/notifications/lib/format";
import { ROUTES } from "@/lib/constants";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

const TYPE_LABEL: Record<string, string> = {
  TASK: "Tarefa",
  WORKFLOW: "Automação",
  INVITATION: "Convite",
  MESSAGE: "Mensagem",
  LEAD: "Lead",
  OPPORTUNITY: "Negócio",
  SYSTEM: "Sistema",
  INFO: "Info",
};

export function NotificationBell() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const perms = useNotificationPermissions();

  const { data: notifications = [] } = useNotifications(companyId);
  const { data: unread = 0 } = useUnreadCount(companyId);
  const markRead = useMarkNotificationRead(companyId);
  const markAllRead = useMarkAllNotificationsRead(companyId);

  const recent = notifications.slice(0, 5);

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" size="icon" className="relative h-9 w-9" aria-label="Notificações">
          <Bell className="h-4 w-4" />
          {unread > 0 && (
            <span className="absolute -right-0.5 -top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-semibold text-white">
              {unread > 9 ? "9+" : unread}
            </span>
          )}
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent className="w-80" align="end">
        <DropdownMenuLabel className="flex items-center justify-between">
          <span>Notificações</span>
          {unread > 0 && perms.canUpdate && (
            <Button
              variant="ghost"
              size="sm"
              className="h-auto p-0 text-xs text-primary"
              onClick={() => markAllRead.mutate()}
            >
              <CheckCheck className="mr-1 h-3 w-3" />
              Marcar como lidas
            </Button>
          )}
        </DropdownMenuLabel>
        <DropdownMenuSeparator />

        {recent.length === 0 ? (
          <div className="px-4 py-6 text-center text-sm text-muted-foreground">
            Nenhuma notificação
          </div>
        ) : (
          <ScrollArea className="max-h-80">
            {recent.map((n) => (
              <DropdownMenuItem
                key={n.id}
                className="flex cursor-pointer flex-col items-start gap-1 py-3"
                onClick={() => {
                  if (!n.read && perms.canUpdate) markRead.mutate(n.id);
                }}
              >
                <div className="flex w-full items-center justify-between gap-2">
                  <p className={`text-sm font-medium ${n.read ? "text-muted-foreground" : ""}`}>
                    {n.title}
                  </p>
                  <span className="shrink-0 text-[11px] text-muted-foreground">
                    {formatRelativeTime(n.createdAt)}
                  </span>
                </div>
                {n.body && <p className="text-xs text-muted-foreground">{n.body}</p>}
                <span className="text-[10px] uppercase tracking-wide text-muted-foreground">
                  {TYPE_LABEL[n.type] ?? n.type}
                </span>
              </DropdownMenuItem>
            ))}
          </ScrollArea>
        )}

        <DropdownMenuSeparator />
        <Link href={ROUTES.NOTIFICATIONS} passHref legacyBehavior>
          <DropdownMenuItem className="justify-between text-sm text-primary">
            Ver todas as notificações
            <ChevronRight className="h-4 w-4" />
          </DropdownMenuItem>
        </Link>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}