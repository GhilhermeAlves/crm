"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { endOfDay, format, startOfDay } from "date-fns";
import { ptBR } from "date-fns/locale";
import {
  ArrowRight,
  CalendarCheck,
  CalendarDays,
  CircleDollarSign,
  Eye,
  EyeOff,
  ListTodo,
  Loader2,
  UserCheck,
} from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { PageTitle } from "@/components/common/PageTitle";
import { TypewriterText } from "@/components/common/TypewriterText";
import { NAVIGATION } from "@/components/layout/navigation";
import { readRecentShortcuts } from "@/components/layout/recentShortcuts";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { useAuthorization } from "@/features/identity/auth/hooks/useAuthorization";
import { useActiveCompanyModules } from "@/features/identity/modules/hooks/useCompanyModules";
import { useOperationalDashboard } from "@/features/analytics/dashboard/hooks/useOperationalDashboard";
import { CrmOverview } from "@/features/analytics/crm/components/CrmOverview";
import { useAppointments } from "@/features/sales/scheduling/hooks/useScheduling";
import {
  APPOINTMENT_STATUS_COLORS,
  APPOINTMENT_STATUS_LABELS,
} from "@/features/sales/scheduling/types/scheduling.types";
import { useHeadline } from "@/features/finance/hooks/useFinance";
import { centsToBRL } from "@/features/finance/utils/money";
import { ROUTES } from "@/lib/constants";
import { cn } from "@/lib/utils";

type Stat = {
  title: string;
  value: string | number;
  description: string;
  icon: React.ReactNode;
  accent: "primary" | "success" | "warning" | "purple";
  iconGradient: string;
  href: string;
  /** Valor sensível: começa escondido e abre no olho. */
  secret?: boolean;
};

/** Cores vivas para nomes e atalhos (classes completas para o Tailwind). */
const PALETTE = [
  {
    soft: "bg-sky-50 dark:bg-sky-950/40",
    solid: "bg-sky-500",
    text: "text-sky-700 dark:text-sky-300",
    border: "border-l-sky-500",
  },
  {
    soft: "bg-violet-50 dark:bg-violet-950/40",
    solid: "bg-violet-500",
    text: "text-violet-700 dark:text-violet-300",
    border: "border-l-violet-500",
  },
  {
    soft: "bg-emerald-50 dark:bg-emerald-950/40",
    solid: "bg-emerald-500",
    text: "text-emerald-700 dark:text-emerald-300",
    border: "border-l-emerald-500",
  },
  {
    soft: "bg-amber-50 dark:bg-amber-950/40",
    solid: "bg-amber-500",
    text: "text-amber-700 dark:text-amber-300",
    border: "border-l-amber-500",
  },
  {
    soft: "bg-rose-50 dark:bg-rose-950/40",
    solid: "bg-rose-500",
    text: "text-rose-700 dark:text-rose-300",
    border: "border-l-rose-500",
  },
  {
    soft: "bg-indigo-50 dark:bg-indigo-950/40",
    solid: "bg-indigo-500",
    text: "text-indigo-700 dark:text-indigo-300",
    border: "border-l-indigo-500",
  },
  {
    soft: "bg-teal-50 dark:bg-teal-950/40",
    solid: "bg-teal-500",
    text: "text-teal-700 dark:text-teal-300",
    border: "border-l-teal-500",
  },
  {
    soft: "bg-orange-50 dark:bg-orange-950/40",
    solid: "bg-orange-500",
    text: "text-orange-700 dark:text-orange-300",
    border: "border-l-orange-500",
  },
];

/** Mesma cor sempre para o mesmo nome. */
function colorFor(key: string) {
  let hash = 0;
  for (let i = 0; i < key.length; i++) hash = (hash * 31 + key.charCodeAt(i)) | 0;
  return PALETTE[Math.abs(hash) % PALETTE.length];
}

function initials(name: string) {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  return (
    ((parts[0]?.[0] ?? "") + (parts.length > 1 ? parts[parts.length - 1][0] : "")).toUpperCase() ||
    "?"
  );
}

const SHORTCUT_COUNT = 4;

/**
 * Tela inicial depois do login. Mostra só o que a empresa usa: agenda e tarefas
 * do dia, receita do mês, atalhos dos módulos ligados e, com o módulo Comercial,
 * o painel de vendas.
 */
export default function HomePage() {
  const { user } = useAuth();
  const { can } = useAuthorization();
  const { isEnabled } = useActiveCompanyModules();
  const companyId = user?.companyId ?? null;

  const showCrm = isEnabled("CRM");
  const showAgenda = isEnabled("AGENDA") && can("appointment:read");
  const showTasks = isEnabled("TASKS") && can("task:page:view");
  const showFinance = isEnabled("FINANCE") && can("finance:read");

  const { data: dashboard, isLoading: dashboardLoading } = useOperationalDashboard(companyId);
  const today = useMemo(() => new Date(), []);
  const { data: appointments = [], isLoading: agendaLoading } = useAppointments(
    showAgenda ? companyId : null,
    startOfDay(today).toISOString(),
    endOfDay(today).toISOString(),
  );
  const { data: headline, isLoading: financeLoading } = useHeadline(showFinance ? companyId : null);
  const [showRevenue, setShowRevenue] = useState(false);
  const [recent, setRecent] = useState<string[]>([]);
  useEffect(() => setRecent(readRecentShortcuts()), []);

  const greeting = useMemo(() => {
    const hour = today.getHours();
    if (hour < 12) return "Bom dia";
    if (hour < 18) return "Boa tarde";
    return "Boa noite";
  }, [today]);

  const todays = useMemo(
    () =>
      appointments
        .filter((a) => a.status !== "CANCELED")
        .sort((a, b) => a.startAt.localeCompare(b.startAt)),
    [appointments],
  );
  const confirmed = todays.filter((a) => a.status === "CONFIRMED" || a.status === "COMPLETED");
  const tasksToday = dashboard?.dueToday ?? [];

  const stats: Stat[] = [];
  if (showAgenda) {
    stats.push(
      {
        title: "Atendimentos hoje",
        value: todays.length,
        description: todays.length === 1 ? "agendamento" : "agendamentos",
        icon: <CalendarDays className="h-5 w-5" />,
        accent: "primary",
        iconGradient: "bg-crm-gradient-primary",
        href: ROUTES.AGENDA,
      },
      {
        title: "Confirmados",
        value: confirmed.length,
        description: `de ${todays.length} de hoje`,
        icon: <UserCheck className="h-5 w-5" />,
        accent: "success",
        iconGradient: "bg-crm-gradient-success",
        href: ROUTES.AGENDA,
      },
    );
  }
  if (showTasks) {
    stats.push({
      title: "Tarefas hoje",
      value: dashboard?.tasksDueToday ?? 0,
      description: "vencendo hoje",
      icon: <ListTodo className="h-5 w-5" />,
      accent: "warning",
      iconGradient: "bg-crm-gradient-warning",
      href: ROUTES.TASKS,
    });
  }
  if (showFinance) {
    stats.push({
      title: "Receita do mês",
      value: centsToBRL(headline?.revenueThisMonthCents ?? 0),
      description: "recebido neste mês",
      icon: <CircleDollarSign className="h-5 w-5" />,
      accent: "purple",
      iconGradient: "bg-crm-gradient-secondary",
      href: ROUTES.FINANCE_CONTROL,
      secret: true,
    });
  }
  const statLoading = (title: string) =>
    title === "Receita do mês"
      ? financeLoading
      : title === "Tarefas hoje"
        ? dashboardLoading
        : agendaLoading;

  // Atalhos: os últimos módulos abertos (completando com os primeiros do menu),
  // filtrados por módulo e permissão.
  const available = NAVIGATION.flatMap((group) => group.items).filter(
    (item) =>
      item.href !== ROUTES.CRM &&
      !item.crmAdminOnly &&
      (!item.module || isEnabled(item.module)) &&
      (!item.permission || can(item.permission)),
  );
  const shortcuts = [
    ...recent.flatMap((href) => available.filter((item) => item.href === href)),
    ...available.filter((item) => !recent.includes(item.href)),
  ].slice(0, SHORTCUT_COUNT);

  return (
    <div className="space-y-6">
      <Card accent="primary">
        <CardContent className="flex flex-col gap-3 p-5 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-0.5">
            <PageTitle as="h2" className="text-xl lg:text-2xl">
              <TypewriterText text={`${greeting}, ${user?.name?.split(" ")[0] || "usuário"}!`} />
            </PageTitle>
            <p className="text-sm text-muted-foreground first-letter:uppercase">
              {format(today, "EEEE, d 'de' MMMM", { locale: ptBR })}
              {showAgenda && !agendaLoading
                ? ` · ${
                    todays.length === 0
                      ? "nenhum atendimento hoje"
                      : `${todays.length} ${todays.length === 1 ? "atendimento" : "atendimentos"} hoje`
                  }`
                : ""}
            </p>
          </div>
          {showAgenda && (
            <Button asChild size="sm">
              <Link href={ROUTES.AGENDA}>
                <CalendarCheck className="mr-1 h-4 w-4" />
                Abrir agenda
              </Link>
            </Button>
          )}
        </CardContent>
      </Card>

      {stats.length > 0 && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {stats.map((stat) => (
            <Link key={stat.title} href={stat.href}>
              <Card
                accent={stat.accent}
                className="h-full hover:-translate-y-1 hover:shadow-crm-lg"
              >
                <CardContent className="flex items-center justify-between gap-3 p-4">
                  <div className="min-w-0 space-y-0.5">
                    <p className="truncate text-sm text-muted-foreground">{stat.title}</p>
                    <div className="flex items-center gap-1.5">
                      <p className="text-2xl font-bold tabular-nums">
                        {statLoading(stat.title) ? (
                          <Loader2 className="h-5 w-5 animate-spin" />
                        ) : stat.secret && !showRevenue ? (
                          "R$ ••••••"
                        ) : (
                          stat.value
                        )}
                      </p>
                      {stat.secret && (
                        <button
                          type="button"
                          className="rounded p-1 text-muted-foreground hover:bg-muted hover:text-foreground"
                          aria-label={showRevenue ? "Esconder valor" : "Mostrar valor"}
                          onClick={(event) => {
                            event.preventDefault();
                            event.stopPropagation();
                            setShowRevenue((v) => !v);
                          }}
                        >
                          {showRevenue ? (
                            <EyeOff className="h-4 w-4" />
                          ) : (
                            <Eye className="h-4 w-4" />
                          )}
                        </button>
                      )}
                    </div>
                    <p className="truncate text-xs text-muted-foreground">{stat.description}</p>
                  </div>
                  <div
                    className={cn(
                      "flex h-10 w-10 shrink-0 items-center justify-center rounded-lg text-white shadow-crm-sm",
                      stat.iconGradient,
                    )}
                  >
                    {stat.icon}
                  </div>
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      )}

      {(showAgenda || showTasks) && (
        <div className="grid gap-4 lg:grid-cols-2">
          {showAgenda && (
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-base">Agenda de hoje</CardTitle>
                <Button asChild variant="ghost" size="sm">
                  <Link href={ROUTES.AGENDA}>
                    Ver agenda <ArrowRight className="ml-1 h-4 w-4" />
                  </Link>
                </Button>
              </CardHeader>
              <CardContent>
                {agendaLoading ? (
                  <div className="flex justify-center py-8 text-muted-foreground">
                    <Loader2 className="h-5 w-5 animate-spin" />
                  </div>
                ) : todays.length === 0 ? (
                  <p className="py-8 text-center text-sm text-muted-foreground">
                    Nenhum atendimento marcado para hoje.
                  </p>
                ) : (
                  <ul className="space-y-2">
                    {todays.slice(0, 8).map((appointment) => {
                      const color = colorFor(appointment.title);
                      return (
                        <li
                          key={appointment.id}
                          className={cn(
                            "flex items-center gap-3 rounded-lg border-l-4 px-3 py-2",
                            color.soft,
                            color.border,
                          )}
                        >
                          <span
                            className={cn(
                              "flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-xs font-bold text-white shadow-crm-sm",
                              color.solid,
                            )}
                          >
                            {initials(appointment.title)}
                          </span>
                          <div className="min-w-0 flex-1">
                            <p className={cn("truncate text-sm font-semibold", color.text)}>
                              {appointment.title}
                            </p>
                            <p className="text-xs tabular-nums text-muted-foreground">
                              {format(new Date(appointment.startAt), "HH:mm")}
                              {" – "}
                              {format(new Date(appointment.endAt), "HH:mm")}
                            </p>
                          </div>
                          <Badge
                            variant="outline"
                            className={cn(
                              "shrink-0 border-0",
                              APPOINTMENT_STATUS_COLORS[appointment.status],
                            )}
                          >
                            {APPOINTMENT_STATUS_LABELS[appointment.status]}
                          </Badge>
                        </li>
                      );
                    })}
                    {todays.length > 8 && (
                      <li className="pt-2 text-center text-xs text-muted-foreground">
                        + {todays.length - 8} atendimentos
                      </li>
                    )}
                  </ul>
                )}
              </CardContent>
            </Card>
          )}

          {showTasks && (
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-base">Tarefas de hoje</CardTitle>
                <Button asChild variant="ghost" size="sm">
                  <Link href={ROUTES.TASKS}>
                    Ver tarefas <ArrowRight className="ml-1 h-4 w-4" />
                  </Link>
                </Button>
              </CardHeader>
              <CardContent>
                {dashboardLoading ? (
                  <div className="flex justify-center py-8 text-muted-foreground">
                    <Loader2 className="h-5 w-5 animate-spin" />
                  </div>
                ) : tasksToday.length === 0 ? (
                  <p className="py-8 text-center text-sm text-muted-foreground">
                    Nenhuma tarefa vencendo hoje.
                  </p>
                ) : (
                  <ul className="divide-y">
                    {tasksToday.slice(0, 8).map((task) => (
                      <li key={task.id} className="flex items-center gap-3 py-2">
                        <span className="w-12 shrink-0 text-sm tabular-nums text-muted-foreground">
                          {task.dueAt ? format(new Date(task.dueAt), "HH:mm") : "—"}
                        </span>
                        <span className="min-w-0 flex-1 truncate text-sm">{task.title}</span>
                      </li>
                    ))}
                  </ul>
                )}
              </CardContent>
            </Card>
          )}
        </div>
      )}

      {shortcuts.length > 0 && (
        <div className="space-y-3">
          <PageTitle>Acesso rápido</PageTitle>
          <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
            {shortcuts.map((item, index) => {
              const Icon = item.icon;
              const color = PALETTE[(index * 3) % PALETTE.length];
              return (
                <Link key={item.href} href={item.href}>
                  <Card
                    className={cn(
                      "h-full border-0 transition-transform hover:-translate-y-1 hover:shadow-crm-lg",
                      color.soft,
                    )}
                  >
                    <CardContent className="flex items-center gap-3 p-4">
                      <div
                        className={cn(
                          "flex h-12 w-12 shrink-0 items-center justify-center rounded-xl text-white shadow-crm-sm",
                          color.solid,
                        )}
                      >
                        <Icon className="h-6 w-6" />
                      </div>
                      <span className={cn("text-base font-semibold", color.text)}>
                        {item.label}
                      </span>
                    </CardContent>
                  </Card>
                </Link>
              );
            })}
          </div>
        </div>
      )}

      {showCrm && <CrmOverview data={dashboard} isLoading={dashboardLoading} />}
    </div>
  );
}
