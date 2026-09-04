"use client";

import { useMemo } from "react";
import Link from "next/link";
import { AlertTriangle, CalendarClock, CheckCircle2, Loader2, TrendingUp, Zap } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useOperationalDashboard } from "@/features/dashboard/hooks/useOperationalDashboard";
import { AttentionList } from "@/components/dashboard/AttentionList";
import { ActivityTimeline } from "@/features/activities/components/ActivityTimeline";
import { TaskList } from "@/features/tasks/components/TaskList";
import { useChangeTaskStatus } from "@/features/tasks/hooks/useTasks";
import { useTaskPermissions } from "@/features/tasks/schemas/task.schema";
import { useOpportunityPermissions } from "@/features/pipeline/schemas/pipeline.schema";
import { ROUTES } from "@/lib/constants";

const formatCurrency = (value: number): string =>
  new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(value);

export default function DashboardPage() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const { data, isLoading } = useOperationalDashboard(companyId);
  const changeStatus = useChangeTaskStatus(companyId);
  const taskPerms = useTaskPermissions();
  const oppPerms = useOpportunityPermissions();

  const greeting = useMemo(() => {
    const hour = new Date().getHours();
    if (hour < 12) return "Bom dia";
    if (hour < 18) return "Boa tarde";
    return "Boa noite";
  }, []);

  const stats = [
    {
      title: "Oportunidades para atenção",
      value: data?.opportunitiesNeedingAttention ?? 0,
      description: "Precisam de follow-up",
      icon: <AlertTriangle className="h-5 w-5 text-amber-500" />,
    },
    {
      title: "Tarefas hoje",
      value: data?.tasksDueToday ?? 0,
      description: "Vencendo hoje",
      icon: <CalendarClock className="h-5 w-5 text-blue-500" />,
    },
    {
      title: "Pipeline em aberto",
      value: data?.openOpportunities ?? 0,
      description: `${formatCurrency(data?.openValue ?? 0)} em jogo`,
      icon: <TrendingUp className="h-5 w-5 text-emerald-500" />,
    },
    {
      title: "Oportunidades paradas",
      value: data?.staleOpportunities ?? 0,
      description: "Sem atividade há 7+ dias",
      icon: <Zap className="h-5 w-5 text-purple-500" />,
    },
  ];

  return (
    <div className="space-y-6">
      {/* Welcome */}
      <Card className="border-primary/20 bg-gradient-to-r from-primary/5 to-primary/10">
        <CardContent className="flex flex-col gap-4 p-6 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-1">
            <h2 className="text-xl font-semibold lg:text-2xl">
              {greeting}, {user?.name?.split(" ")[0] || "usuário"}!
            </h2>
            <p className="text-muted-foreground">
              {isLoading
                ? "Reunindo o que merece sua atenção…"
                : data?.greeting || "Não há pendencias registradas."}
            </p>
          </div>
          {oppPerms.canCreate && (
            <Button asChild size="sm">
              <Link href={ROUTES.PIPELINE}>
                <CheckCircle2 className="mr-1 h-4 w-4" />
                Ver Pipeline
              </Link>
            </Button>
          )}
        </CardContent>
      </Card>

      {/* KPI strip */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {stats.map((stat) => (
          <Card key={stat.title}>
            <CardContent className="flex items-center justify-between p-6">
              <div className="space-y-1">
                <p className="text-sm font-medium text-muted-foreground">{stat.title}</p>
                <p className="text-2xl font-bold">
                  {isLoading ? <Loader2 className="h-5 w-5 animate-spin" /> : stat.value}
                </p>
                <p className="text-xs text-muted-foreground">{stat.description}</p>
              </div>
              <div className="rounded-lg bg-muted p-3">{stat.icon}</div>
            </CardContent>
          </Card>
        ))}
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* O que merece atenção */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-4">
            <CardTitle className="text-base font-semibold">Necessitam de atenção</CardTitle>
            <Button variant="ghost" size="sm" className="text-xs text-muted-foreground" asChild>
              <Link href={ROUTES.PIPELINE}>Ver pipeline</Link>
            </Button>
          </CardHeader>
          <CardContent>
            <AttentionList
              opportunities={data?.attentionOpportunities ?? []}
              isLoading={isLoading}
            />
          </CardContent>
        </Card>

        {/* Tarefas hoje */}
        <Card>
          <CardHeader className="pb-4">
            <CardTitle className="text-base font-semibold">Tarefas para hoje</CardTitle>
          </CardHeader>
          <CardContent>
            <TaskList
              tasks={data?.dueToday ?? []}
              isLoading={isLoading}
              canUpdate={taskPerms.canUpdate}
              onChangeStatus={(id, status) => changeStatus.mutate({ id, status })}
            />
            {taskPerms.canCreate && (
              <Button variant="outline" size="sm" className="mt-3 w-full" asChild>
                <Link href={ROUTES.TASKS}>Gerenciar tarefas</Link>
              </Button>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Recent activities */}
      <Card>
        <CardHeader className="pb-4">
          <CardTitle className="text-base font-semibold">Atividades recentes</CardTitle>
        </CardHeader>
        <CardContent>
          <ActivityTimeline activities={data?.recentActivities ?? []} isLoading={isLoading} />
        </CardContent>
      </Card>
    </div>
  );
}
