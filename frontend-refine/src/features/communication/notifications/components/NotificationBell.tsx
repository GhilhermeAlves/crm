"use client";

import { useMemo, useState } from "react";
import {
  Bell,
  Briefcase,
  CheckCheck,
  CheckSquare,
  ChevronRight,
  ClipboardList,
  Info,
  type LucideIcon,
  Mail,
  MessageCircle,
  Settings2,
  UserPlus,
  Workflow,
  X,
} from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import {
  useNotifications,
  useUnreadCount,
  useMarkNotificationRead,
  useMarkAllNotificationsRead,
  useNotificationPermissions,
} from "@/features/communication/notifications/hooks/useNotifications";
import { formatRelativeTime } from "@/features/communication/notifications/lib/format";
import { ReportExportDownloadButton } from "@/features/reports/export/ReportExportDownloadButton";
import { notificationHref } from "../lib/links";
import type { Notification, NotificationType } from "../types/notification.types";
import { ROUTES } from "@/lib/constants";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";

/** Ícone e cor (círculo) de cada tipo, como no painel do Capim. */
const TYPE_STYLE: Record<NotificationType, { icon: LucideIcon; className: string }> = {
  TASK: { icon: CheckSquare, className: "bg-amber-100 text-amber-700 dark:bg-amber-500/15" },
  WORKFLOW: { icon: Workflow, className: "bg-violet-100 text-violet-700 dark:bg-violet-500/15" },
  INVITATION: { icon: Mail, className: "bg-sky-100 text-sky-700 dark:bg-sky-500/15" },
  MESSAGE: {
    icon: MessageCircle,
    className: "bg-emerald-100 text-emerald-700 dark:bg-emerald-500/15",
  },
  LEAD: { icon: UserPlus, className: "bg-sky-100 text-sky-700 dark:bg-sky-500/15" },
  OPPORTUNITY: {
    icon: Briefcase,
    className: "bg-indigo-100 text-indigo-700 dark:bg-indigo-500/15",
  },
  SYSTEM: { icon: Settings2, className: "bg-primary/10 text-primary" },
  INFO: { icon: Info, className: "bg-muted text-muted-foreground" },
};

/** Formulário respondido pelo paciente chega como SYSTEM: ícone próprio. */
function styleFor(n: Notification) {
  if (n.metadata?.includes('"PATIENT_FORM"')) {
    return { icon: ClipboardList, className: "bg-rose-100 text-rose-700 dark:bg-rose-500/15" };
  }
  return TYPE_STYLE[n.type] ?? TYPE_STYLE.INFO;
}

type Tab = "ALL" | "MESSAGES" | "TASKS" | "SYSTEM";

const TABS: { value: Tab; label: string; types?: NotificationType[] }[] = [
  { value: "ALL", label: "Todas" },
  { value: "MESSAGES", label: "Mensagens", types: ["MESSAGE", "LEAD", "OPPORTUNITY"] },
  { value: "TASKS", label: "Tarefas", types: ["TASK", "WORKFLOW"] },
  { value: "SYSTEM", label: "Sistema", types: ["SYSTEM", "INFO", "INVITATION"] },
];

function inTab(n: Notification, tab: Tab) {
  const types = TABS.find((t) => t.value === tab)?.types;
  return !types || types.includes(n.type);
}

/** "Hoje", "Ontem" ou "10 de outubro". */
function dayLabel(iso: string) {
  const date = new Date(iso);
  const today = new Date();
  const yesterday = new Date();
  yesterday.setDate(today.getDate() - 1);
  if (date.toDateString() === today.toDateString()) return "Hoje";
  if (date.toDateString() === yesterday.toDateString()) return "Ontem";
  return date.toLocaleDateString("pt-BR", {
    day: "numeric",
    month: "long",
    ...(date.getFullYear() !== today.getFullYear() && { year: "numeric" }),
  });
}

export function NotificationBell() {
  const { user } = useAuth();
  const router = useRouter();
  const companyId = user?.companyId ?? null;
  const perms = useNotificationPermissions();
  const [open, setOpen] = useState(false);
  const [tab, setTab] = useState<Tab>("ALL");

  const { data: notifications = [] } = useNotifications(companyId);
  const { data: unread = 0 } = useUnreadCount(companyId);
  const markRead = useMarkNotificationRead(companyId);
  const markAllRead = useMarkAllNotificationsRead(companyId);

  const groups = useMemo(() => {
    const result: { day: string; items: Notification[] }[] = [];
    for (const n of notifications.filter((item) => inTab(item, tab))) {
      const day = dayLabel(n.createdAt);
      const last = result[result.length - 1];
      if (last?.day === day) last.items.push(n);
      else result.push({ day, items: [n] });
    }
    return result;
  }, [notifications, tab]);

  const unreadIn = (value: Tab) => notifications.filter((n) => !n.read && inTab(n, value)).length;

  const openNotification = (n: Notification) => {
    if (!n.read && perms.canUpdate) markRead.mutate(n.id);
    const href = notificationHref(n.metadata) ?? (n.type === "MESSAGE" ? ROUTES.INBOX : null);
    if (href) {
      setOpen(false);
      router.push(href);
    }
  };

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button variant="ghost" size="icon" className="relative h-9 w-9" aria-label="Notificações">
          <Bell className="h-4 w-4" />
          {unread > 0 && (
            <span className="absolute -right-0.5 -top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-semibold text-white">
              {unread > 9 ? "+9" : unread}
            </span>
          )}
        </Button>
      </PopoverTrigger>
      <PopoverContent
        align="end"
        sideOffset={8}
        className="w-[min(400px,calc(100vw-16px))] rounded-2xl p-0 shadow-xl"
      >
        <div className="flex items-start justify-between gap-2 px-5 pb-2 pt-4">
          <div>
            <h2 className="text-base font-semibold">Notificações</h2>
            <p className="text-xs text-muted-foreground">
              {unread > 0
                ? `Você tem ${unread} ${unread === 1 ? "notificação nova" : "notificações novas"}`
                : "Você está em dia"}
            </p>
          </div>
          <div className="flex items-center gap-1">
            {unread > 0 && perms.canUpdate && (
              <Button
                variant="ghost"
                size="sm"
                className="h-8 px-2 text-xs text-primary"
                onClick={() => markAllRead.mutate()}
              >
                <CheckCheck className="mr-1 h-3.5 w-3.5" />
                Marcar como lidas
              </Button>
            )}
            <Button
              variant="ghost"
              size="icon"
              className="h-8 w-8"
              onClick={() => setOpen(false)}
              aria-label="Fechar"
            >
              <X className="h-4 w-4" />
            </Button>
          </div>
        </div>

        <div className="flex gap-4 border-b px-5" role="tablist">
          {TABS.map((t) => {
            const count = unreadIn(t.value);
            return (
              <button
                key={t.value}
                type="button"
                role="tab"
                aria-selected={tab === t.value}
                onClick={() => setTab(t.value)}
                className={cn(
                  "-mb-px flex items-center gap-1.5 border-b-2 pb-2 text-sm transition-colors",
                  tab === t.value
                    ? "border-primary font-semibold text-primary"
                    : "border-transparent text-muted-foreground hover:text-foreground",
                )}
              >
                {t.label}
                {count > 0 && (
                  <span
                    className={cn(
                      "rounded-full px-1.5 text-[10px] font-semibold leading-4",
                      tab === t.value
                        ? "bg-primary text-primary-foreground"
                        : "bg-muted text-muted-foreground",
                    )}
                  >
                    {count > 9 ? "+9" : count}
                  </span>
                )}
              </button>
            );
          })}
        </div>

        {groups.length === 0 ? (
          <div className="flex flex-col items-center gap-2 px-5 py-10 text-center">
            <span className="flex h-12 w-12 items-center justify-center rounded-full bg-muted">
              <Bell className="h-5 w-5 text-muted-foreground" />
            </span>
            <p className="text-sm text-muted-foreground">Nenhuma notificação por aqui</p>
          </div>
        ) : (
          <ScrollArea className="max-h-[420px]">
            <div className="space-y-3 px-5 py-3">
              {groups.map((group) => (
                <section key={group.day} className="space-y-2">
                  <p className="text-xs font-medium text-muted-foreground">{group.day}</p>
                  {group.items.map((n) => {
                    const { icon: Icon, className } = styleFor(n);
                    return (
                      <div
                        key={n.id}
                        role="button"
                        tabIndex={0}
                        onClick={() => openNotification(n)}
                        onKeyDown={(e) => {
                          if (e.key === "Enter" || e.key === " ") {
                            e.preventDefault();
                            openNotification(n);
                          }
                        }}
                        className={cn(
                          "flex cursor-pointer gap-3 rounded-xl p-3 transition-colors",
                          n.read
                            ? "bg-muted/40 hover:bg-muted/70"
                            : "bg-primary/5 hover:bg-primary/10",
                        )}
                      >
                        <span
                          className={cn(
                            "flex h-9 w-9 shrink-0 items-center justify-center rounded-full",
                            className,
                          )}
                        >
                          <Icon className="h-4 w-4" />
                        </span>
                        <div className="min-w-0 flex-1 space-y-0.5">
                          <div className="flex items-start justify-between gap-2">
                            <p
                              className={cn(
                                "text-sm leading-snug",
                                n.read ? "font-medium text-muted-foreground" : "font-semibold",
                              )}
                            >
                              {n.title}
                            </p>
                            {!n.read && (
                              <span className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-primary" />
                            )}
                          </div>
                          {n.body && (
                            <p className="line-clamp-3 text-xs text-muted-foreground">{n.body}</p>
                          )}
                          <ReportExportDownloadButton
                            companyId={companyId}
                            metadata={n.metadata}
                            onDownloaded={() => {
                              if (!n.read && perms.canUpdate) markRead.mutate(n.id);
                            }}
                          />
                          <p className="text-[11px] text-muted-foreground">
                            {formatRelativeTime(n.createdAt)}
                          </p>
                        </div>
                      </div>
                    );
                  })}
                </section>
              ))}
            </div>
          </ScrollArea>
        )}

        <Link
          href={ROUTES.NOTIFICATIONS}
          onClick={() => setOpen(false)}
          className="flex items-center justify-between border-t px-5 py-3 text-sm font-medium text-primary hover:bg-muted/50"
        >
          Ver todas as notificações
          <ChevronRight className="h-4 w-4" />
        </Link>
      </PopoverContent>
    </Popover>
  );
}
