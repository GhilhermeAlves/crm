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
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useKeycloak } from "@/providers/KeycloakProvider";
import { TokenManager } from "@/store/token-manager";
import { ROUTES } from "@/lib/constants";
import { useDashboardKpis, useRecentActivities } from "@/features/dashboard/hooks/useDashboard";
import { KpiGrid } from "@/features/dashboard/components/KpiGrid";
import { ActivitiesFeed } from "@/features/dashboard/components/ActivitiesFeed";
import { DashboardSkeleton } from "@/features/dashboard/components/DashboardSkeleton";

const quickActions = [
  { label: "Novo Lead", href: ROUTES.LEADS, icon: Plus },
  { label: "Nova Campanha", href: ROUTES.CAMPAIGNS, icon: Megaphone },
  { label: "Ver Pipeline", href: ROUTES.PIPELINE, icon: GitBranch },
  { label: "Relatórios", href: ROUTES.REPORTS, icon: TrendingUp },
];

export default function DashboardPage() {
  const { user, logout } = useAuth();
  const keycloakCtx = useKeycloak();
  const { data: kpis, isLoading: kpisLoading } = useDashboardKpis();
  const { data: activities, isLoading: activitiesLoading } = useRecentActivities();

  const greeting = useMemo(() => {
    const hour = new Date().getHours();
    if (hour < 12) return "Bom dia";
    if (hour < 18) return "Boa tarde";
    return "Boa noite";
  }, []);

  const kcTokenPayload = useMemo(() => {
    if (!keycloakCtx.token) return null;
    try {
      const base64Url = keycloakCtx.token.split(".")[1];
      if (!base64Url) return null;
      const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
      const jsonPayload = decodeURIComponent(
        atob(base64).split("").map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2)).join("")
      );
      return JSON.parse(jsonPayload) as Record<string, string | undefined>;
    } catch {
      return null;
    }
  }, [keycloakCtx.token]);

  if (kpisLoading || activitiesLoading) {
    return <DashboardSkeleton />;
  }

  return (
    <div className="space-y-6">
      {/* Keycloak Validation Banner */}
      {TokenManager.isKeycloakAuth() && (
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
              Aqui está o resumo do seu CRM.{" "}
              {kpis && kpis.activeUsers > 0 ? (
                <span>
                  Você tem <span className="font-medium text-foreground">{kpis.activeUsers} usuário{kpis.activeUsers > 1 ? "s" : ""} ativo{kpis.activeUsers > 1 ? "s" : ""}</span> na plataforma.
                </span>
              ) : (
                <span>Bem-vindo ao CRM.</span>
              )}
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

      {/* KPI Grid */}
      {kpis && <KpiGrid kpis={kpis} />}

      <div className="grid gap-6 lg:grid-cols-2">
        {/* Recent Activities */}
        <ActivitiesFeed activities={activities || []} />

        <div className="space-y-6">
          {/* Quick Actions */}
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

          {/* Day Summary */}
          <Card>
            <CardHeader className="pb-4">
              <CardTitle className="text-base font-semibold">Resumo do Dia</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2 text-sm">
                    <Users className="h-4 w-4 text-blue-500" />
                    Usuários ativos
                  </div>
                  <span className="text-sm font-medium">{kpis?.activeUsers ?? "—"}</span>
                </div>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2 text-sm">
                    <UserIcon className="h-4 w-4 text-emerald-500" />
                    Novos usuários (mês)
                  </div>
                  <span className="text-sm font-medium">{kpis?.newUsersThisMonth ?? "—"}</span>
                </div>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2 text-sm">
                    <Shield className="h-4 w-4 text-amber-500" />
                    Eventos de auditoria
                  </div>
                  <span className="text-sm font-medium">{kpis?.auditEventsThisMonth ?? "—"}</span>
                </div>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2 text-sm">
                    <Clock className="h-4 w-4 text-purple-500" />
                    Atividades recentes
                  </div>
                  <span className="text-sm font-medium">{activities?.length ?? 0}</span>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
