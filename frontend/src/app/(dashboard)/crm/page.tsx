"use client";

import { useMemo } from "react";
import Link from "next/link";
import {
  AlertTriangle,
  CalendarClock,
  CheckCircle2,
  Loader2,
  TrendingUp,
  Zap,
  Contact,
  Building2,
  CalendarDays,
  BarChart3,
  FolderKanban,
  GitBranch,
  Users,
  ShieldOff,
} from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { PageTitle } from "@/components/common/PageTitle";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useAuthorization } from "@/features/auth/hooks/useAuthorization";
import { useOperationalDashboard } from "@/features/dashboard/hooks/useOperationalDashboard";
import { useOpportunityPermissions } from "@/features/pipeline/schemas/pipeline.schema";
import { CrmModuleCard, type CrmModule } from "@/features/crm/components/CrmModuleCard";
import { ROUTES } from "@/lib/constants";

const formatCurrency = (value: number): string =>
  new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(value);

const modules: CrmModule[] = [
  {
    title: "Contatos",
    description: "Gerencie pessoas e relacionamentos com clientes.",
    icon: <Contact className="h-5 w-5" />,
    href: "/contacts",
    permission: "contact:page:view",
  },
  {
    title: "Negociações",
    description: "Acompanhe oportunidades e o pipeline comercial.",
    icon: <GitBranch className="h-5 w-5" />,
    href: "/pipeline",
    permission: "pipeline:page:view",
  },
  {
    title: "Leads",
    description: "Gerencie leads, qualificação e conversão.",
    icon: <Users className="h-5 w-5" />,
    href: "/leads",
    permission: "lead:page:view",
  },
  {
    title: "Contas",
    description: "Gerencie empresas e contas relacionadas aos clientes.",
    icon: <Building2 className="h-5 w-5" />,
    comingSoon: true,
  },
  {
    title: "Projetos de clientes",
    description: "Acompanhe projetos e entregas relacionadas aos clientes.",
    icon: <FolderKanban className="h-5 w-5" />,
    comingSoon: true,
  },
  {
    title: "Atividades",
    description: "Organize tarefas, contatos, reuniões e próximos acompanhamentos.",
    icon: <CalendarDays className="h-5 w-5" />,
    href: "/activities",
    permission: "activity:page:view",
  },
  {
    title: "Painel de vendas",
    description: "Visualize indicadores, funil e desempenho comercial.",
    icon: <BarChart3 className="h-5 w-5" />,
    href: "/reports",
    permission: "analytics:read",
  },
];

export default function CrmHomePage() {
  const { user } = useAuth();
  const { can } = useAuthorization();
  const companyId = user?.companyId ?? null;
  const { data, isLoading } = useOperationalDashboard(companyId);
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

  const visibleModules = modules.filter((m) => !m.permission || can(m.permission));

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

      {/* Módulos */}
      <div className="space-y-1">
        <PageTitle>CRM</PageTitle>
        <p className="text-sm text-muted-foreground">
          Central de gestão comercial e relacionamento com clientes.
        </p>
      </div>

      {visibleModules.length === 0 ? (
        <Card>
          <CardContent className="flex flex-col items-center py-12 text-muted-foreground">
            <ShieldOff className="mb-4 h-10 w-10 opacity-50" />
            <p>Você não tem permissão para acessar os módulos do CRM.</p>
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {visibleModules.map((mod) => (
            <CrmModuleCard key={mod.title} mod={mod} />
          ))}
        </div>
      )}
    </div>
  );
}
