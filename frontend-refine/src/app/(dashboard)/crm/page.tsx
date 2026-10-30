"use client";

import { useMemo } from "react";
import Link from "next/link";
import { endOfDay, format, startOfDay } from "date-fns";
import { ptBR } from "date-fns/locale";
import {
  ArrowRight,
  CalendarCheck,
  CalendarDays,
  CircleDollarSign,
  ListTodo,
  Loader2,
  UserCheck,
} from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { PageTitle } from "@/components/common/PageTitle";
import { NAVIGATION } from "@/components/layout/navigation";
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
};

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
    });
  }
  const statLoading = (title: string) =>
    title === "Receita do mês"
      ? financeLoading
      : title === "Tarefas hoje"
        ? dashboardLoading
        : agendaLoading;

  // Atalhos: os mesmos itens do menu, filtrados por módulo e permissão.
  const shortcuts = NAVIGATION.flatMap((group) => group.items).filter(
    (item) =>
      item.href !== ROUTES.CRM &&
      !item.crmAdminOnly &&
      (!item.module || isEnabled(item.module)) &&
      (!item.permission || can(item.permission)),
  );

  return (
    <div className="space-y-6">
      <Card accent="primary">
        <CardContent className="flex flex-col gap-3 p-5 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-0.5">
            <PageTitle as="h2" className="text-xl lg:text-2xl">
              {greeting}, {user?.name?.split(" ")[0] || "usuário"}!
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
                    <p className="text-2xl font-bold tabular-nums">
                      {statLoading(stat.title) ? (
                        <Loader2 className="h-5 w-5 animate-spin" />
                      ) : (
                        stat.value
                      )}
                    </p>
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
                  <ul className="divide-y">
                    {todays.slice(0, 8).map((appointment) => (
                      <li key={appointment.id} className="flex items-center gap-3 py-2">
                        <span className="w-12 shrink-0 text-sm font-semibold tabular-nums">
                          {format(new Date(appointment.startAt), "HH:mm")}
                        </span>
                        <span className="min-w-0 flex-1 truncate text-sm">{appointment.title}</span>
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
                    ))}
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
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-6">
            {shortcuts.map((item) => {
              const Icon = item.icon;
              return (
                <Link key={item.href} href={item.href}>
                  <Card className="h-full hover:-translate-y-0.5 hover:shadow-crm-lg">
                    <CardContent className="flex flex-col items-center gap-2 p-4 text-center">
                      <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10 text-primary">
                        <Icon className="h-5 w-5" />
                      </div>
                      <span className="text-sm font-medium">{item.label}</span>
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
