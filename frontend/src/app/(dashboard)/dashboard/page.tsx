"use client";

import { useMemo } from "react";
import Link from "next/link";
import {
  Users,
  Contact,
  GitBranch,
  DollarSign,
  Calendar,
  Megaphone,
  TrendingUp,
  TrendingDown,
  ArrowRight,
  Clock,
  CheckCircle2,
  AlertCircle,
  Zap,
  Plus,
  LogOut,
  CheckCircle,
  Shield,
  User as UserIcon,
} from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { BadgeStatus } from "@/components/common/BadgeStatus";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useKeycloak } from "@/providers/KeycloakProvider";
import { TokenManager } from "@/store/token-manager";
import { decodeJwtPayload } from "@/lib/jwt";
import { ROUTES } from "@/lib/constants";

const stats = [
  {
    title: "Usuários",
    value: "12",
    description: "Total ativos",
    icon: Users,
    trend: { value: 8, isPositive: true },
  },
  {
    title: "Clientes",
    value: "248",
    description: "Cadastrados",
    icon: Contact,
    trend: { value: 12, isPositive: true },
  },
  {
    title: "Leads",
    value: "86",
    description: "Em pipeline",
    icon: TrendingUp,
    trend: { value: 24, isPositive: true },
  },
  {
    title: "Negócios",
    value: "34",
    description: "Em andamento",
    icon: GitBranch,
    trend: { value: -3, isPositive: false },
  },
  {
    title: "Financeiro",
    value: "R$ 45.2k",
    description: "Receita do mês",
    icon: DollarSign,
    trend: { value: 15, isPositive: true },
  },
  {
    title: "Campanhas",
    value: "5",
    description: "Ativas",
    icon: Megaphone,
    trend: { value: 2, isPositive: true },
  },
];

const recentActivities = [
  { id: "1", text: "João Silva aceitou a proposta", time: "Há 5 min", type: "success" as const },
  { id: "2", text: "Novo lead: Maria Santos", time: "Há 15 min", type: "info" as const },
  { id: "3", text: "Reunião com Empresa X cancelada", time: "Há 1 hora", type: "warning" as const },
  { id: "4", text: "Campanha Black Friday iniciada", time: "Há 2 horas", type: "info" as const },
  { id: "5", text: "Negócio perdido: Tech Corp", time: "Há 3 horas", type: "danger" as const },
];

const quickActions = [
  { label: "Novo Lead", href: ROUTES.LEADS, icon: Plus },
  { label: "Nova Campanha", href: ROUTES.CAMPAIGNS, icon: Megaphone },
  { label: "Ver Pipeline", href: ROUTES.PIPELINE, icon: GitBranch },
  { label: "Relatórios", href: ROUTES.REPORTS, icon: TrendingUp },
];

const activityDotColors = {
  success: "bg-emerald-500",
  info: "bg-blue-500",
  warning: "bg-amber-500",
  danger: "bg-red-500",
};

export default function DashboardPage() {
  const { user, logout } = useAuth();
  const keycloakCtx = useKeycloak();

  const greeting = useMemo(() => {
    const hour = new Date().getHours();
    if (hour < 12) return "Bom dia";
    if (hour < 18) return "Boa tarde";
    return "Boa noite";
  }, []);

  // Identidade OIDC do JWT do Keycloak (exibição UX — não é autorização).
  const kcTokenPayload = useMemo(() => {
    return decodeJwtPayload<Record<string, string | undefined>>(keycloakCtx.token);
  }, [keycloakCtx.token]);

  return (
    <div className="space-y-6">
      {/* Keycloak Validation Banner */}
      {TokenManager.getAccessToken() && (
        <Card className="border-emerald-200 bg-emerald-50 dark:border-emerald-800 dark:bg-emerald-950/50">
          <CardContent className="flex items-start gap-4 p-6">
            <CheckCircle className="mt-0.5 h-5 w-5 shrink-0 text-emerald-600 dark:text-emerald-400" />
            <div className="flex-1 space-y-2">
              <p className="font-semibold text-emerald-800 dark:text-emerald-300">
                Autenticação Keycloak realizada com sucesso
              </p>
              <div className="grid gap-x-8 gap-y-1 text-sm text-emerald-700 dark:text-emerald-400 sm:grid-cols-2">
                <div className="flex items-center gap-2">
                  <UserIcon className="h-3.5 w-3.5" />
                  <span>Nome: {kcTokenPayload?.name as string || user?.name || "—"}</span>
                </div>
                <div className="flex items-center gap-2">
                  <UserIcon className="h-3.5 w-3.5" />
                  <span>Username: {kcTokenPayload?.preferred_username as string || "—"}</span>
                </div>
                <div className="flex items-center gap-2">
                  <UserIcon className="h-3.5 w-3.5" />
                  <span>Email: {kcTokenPayload?.email as string || user?.email || "—"}</span>
                </div>
                <div className="flex items-center gap-2">
                  <Shield className="h-3.5 w-3.5" />
                  <span>Subject (sub): {kcTokenPayload?.sub as string || "—"}</span>
                </div>
                <div className="flex items-center gap-2">
                  <Shield className="h-3.5 w-3.5" />
                  <span>Realm: CRM</span>
                </div>
                <div className="flex items-center gap-2">
                  <Shield className="h-3.5 w-3.5" />
                  <span>Client: crm-frontend</span>
                </div>
              </div>
            </div>
            <Button variant="destructive" size="sm" onClick={logout} className="shrink-0">
              <LogOut className="mr-1 h-4 w-4" />
              Sair
            </Button>
          </CardContent>
        </Card>
      )}

      {/* Welcome Card */}
      <Card className="bg-gradient-to-r from-primary/5 to-primary/10 border-primary/20">
        <CardContent className="flex flex-col gap-4 p-6 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-1">
            <h2 className="text-xl font-semibold lg:text-2xl">
              {greeting}, {user?.name?.split(" ")[0] || kcTokenPayload?.name || "usuário"}!
            </h2>
            <p className="text-muted-foreground">
              Aqui está o resumo do seu CRM. Você tem{" "}
              <span className="font-medium text-foreground">3 pendências</span> para hoje.
            </p>
          </div>
          <div className="flex gap-2">
            <Button asChild size="sm">
              <Link href={ROUTES.LEADS}>
                <Plus className="mr-1 h-4 w-4" />
                Novo Lead
              </Link>
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Stats Grid */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {stats.map((stat) => {
          const Icon = stat.icon;
          return (
            <Card key={stat.title} className="transition-shadow hover:shadow-md">
              <CardContent className="p-6">
                <div className="flex items-center justify-between">
                  <div className="space-y-1">
                    <p className="text-sm font-medium text-muted-foreground">{stat.title}</p>
                    <p className="text-2xl font-bold">{stat.value}</p>
                    <div className="flex items-center gap-2">
                      <span
                        className={`flex items-center gap-1 text-xs font-medium ${
                          stat.trend.isPositive
                            ? "text-emerald-600 dark:text-emerald-400"
                            : "text-red-600 dark:text-red-400"
                        }`}
                      >
                        {stat.trend.isPositive ? (
                          <TrendingUp className="h-3 w-3" />
                        ) : (
                          <TrendingDown className="h-3 w-3" />
                        )}
                        {stat.trend.isPositive ? "+" : ""}
                        {stat.trend.value}%
                      </span>
                      <p className="text-xs text-muted-foreground">{stat.description}</p>
                    </div>
                  </div>
                  <div className="rounded-lg bg-muted p-3">
                    <Icon className="h-5 w-5 text-muted-foreground" />
                  </div>
                </div>
              </CardContent>
            </Card>
          );
        })}
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* Recent Activities */}
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
              {recentActivities.map((activity) => (
                <div key={activity.id} className="flex items-start gap-3">
                  <div className={`mt-1 h-2 w-2 shrink-0 rounded-full ${activityDotColors[activity.type]}`} />
                  <div className="flex-1 space-y-1">
                    <p className="text-sm">{activity.text}</p>
                    <p className="text-xs text-muted-foreground">{activity.time}</p>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* Quick Actions + Day Summary */}
        <div className="space-y-6">
          <Card>
            <CardHeader className="pb-4">
              <CardTitle className="text-base font-semibold">Ações Rápidas</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-2 gap-3">
                {quickActions.map((action) => {
                  const Icon = action.icon;
                  return (
                    <Button
                      key={action.label}
                      variant="outline"
                      className="h-auto flex-col gap-2 py-4"
                      asChild
                    >
                      <Link href={action.href}>
                        <Icon className="h-5 w-5" />
                        <span className="text-xs">{action.label}</span>
                      </Link>
                    </Button>
                  );
                })}
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="pb-4">
              <CardTitle className="text-base font-semibold">Resumo do Dia</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2 text-sm">
                    <CheckCircle2 className="h-4 w-4 text-emerald-500" />
                    Tarefas concluídas
                  </div>
                  <span className="text-sm font-medium">8/12</span>
                </div>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2 text-sm">
                    <Clock className="h-4 w-4 text-amber-500" />
                    Reuniões hoje
                  </div>
                  <span className="text-sm font-medium">3</span>
                </div>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2 text-sm">
                    <AlertCircle className="h-4 w-4 text-blue-500" />
                    Leads pendentes
                  </div>
                  <span className="text-sm font-medium">5</span>
                </div>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2 text-sm">
                    <Zap className="h-4 w-4 text-purple-500" />
                    Conversões hoje
                  </div>
                  <span className="text-sm font-medium">2</span>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
