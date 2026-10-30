"use client";

import {
  AlertTriangle,
  CalendarClock,
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
import { PageTitle } from "@/components/common/PageTitle";
import { useAuthorization } from "@/features/identity/auth/hooks/useAuthorization";
import type { OperationalDashboard } from "@/features/analytics/dashboard/types/dashboard.types";
import { CrmModuleCard, type CrmModule } from "./CrmModuleCard";
import { CrmRecentItems } from "./CrmRecentItems";
import { ROUTES } from "@/lib/constants";
import { cn } from "@/lib/utils";

const formatCurrency = (value: number): string =>
  new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(value);

const modules: CrmModule[] = [
  {
    title: "Contatos",
    description: "Gerencie pessoas e relacionamentos.",
    icon: <Contact className="h-4 w-4" />,
    href: ROUTES.CONTACTS,
    permission: "contact:page:view",
  },
  {
    title: "Negociações",
    description: "Acompanhe oportunidades e o pipeline.",
    icon: <GitBranch className="h-4 w-4" />,
    href: ROUTES.PIPELINE,
    permission: "pipeline:page:view",
  },
  {
    title: "Leads",
    description: "Gerencie leads, qualificação e conversão.",
    icon: <Users className="h-4 w-4" />,
    href: ROUTES.LEADS,
    permission: "lead:page:view",
  },
  {
    title: "Contas",
    description: "Gerencie empresas e contas dos clientes.",
    icon: <Building2 className="h-4 w-4" />,
    comingSoon: true,
  },
  {
    title: "Projetos de clientes",
    description: "Acompanhe projetos e entregas.",
    icon: <FolderKanban className="h-4 w-4" />,
    comingSoon: true,
  },
  {
    title: "Atividades",
    description: "Organize tarefas, reuniões e próximos acompanhamentos.",
    icon: <CalendarDays className="h-4 w-4" />,
    href: ROUTES.ACTIVITIES,
    permission: "activity:page:view",
  },
  {
    title: "Painel de vendas",
    description: "Visualize indicadores, funil e desempenho.",
    icon: <BarChart3 className="h-4 w-4" />,
    href: ROUTES.REPORTS,
    permission: "analytics:read",
  },
];

/** Bloco comercial da tela inicial: só aparece com o módulo Comercial (CRM) ligado. */
export function CrmOverview({
  data,
  isLoading,
}: {
  data: OperationalDashboard | undefined;
  isLoading: boolean;
}) {
  const { can } = useAuthorization();

  const stats = [
    {
      title: "Oportunidades para atenção",
      value: data?.opportunitiesNeedingAttention ?? 0,
      description: "Precisam de follow-up",
      icon: <AlertTriangle className="h-5 w-5" />,
      accent: "warning" as const,
      iconGradient: "bg-crm-gradient-warning",
    },
    {
      title: "Tarefas hoje",
      value: data?.tasksDueToday ?? 0,
      description: "Vencendo hoje",
      icon: <CalendarClock className="h-5 w-5" />,
      accent: "primary" as const,
      iconGradient: "bg-crm-gradient-primary",
    },
    {
      title: "Pipeline em aberto",
      value: data?.openOpportunities ?? 0,
      description: `${formatCurrency(data?.openValue ?? 0)} em jogo`,
      icon: <TrendingUp className="h-5 w-5" />,
      accent: "success" as const,
      iconGradient: "bg-crm-gradient-success",
    },
    {
      title: "Oportunidades paradas",
      value: data?.staleOpportunities ?? 0,
      description: "Sem atividade há 7+ dias",
      icon: <Zap className="h-5 w-5" />,
      accent: "purple" as const,
      iconGradient: "bg-crm-gradient-secondary",
    },
  ];

  const visibleModules = modules.filter((m) => !m.permission || can(m.permission));

  return (
    <div className="space-y-6">
      <div className="space-y-1">
        <PageTitle>Comercial</PageTitle>
        <p className="text-sm text-muted-foreground">
          Central de gestão comercial e relacionamento com clientes.
        </p>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {stats.map((stat) => (
          <Card
            key={stat.title}
            accent={stat.accent}
            className="hover:-translate-y-1 hover:shadow-crm-lg"
          >
            <CardContent className="flex items-center justify-between gap-3 p-4">
              <div className="min-w-0 space-y-0.5">
                <p className="truncate text-sm text-muted-foreground">{stat.title}</p>
                <p className="text-2xl font-bold tabular-nums">
                  {isLoading ? <Loader2 className="h-5 w-5 animate-spin" /> : stat.value}
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
        ))}
      </div>

      {visibleModules.length === 0 ? (
        <Card>
          <CardContent className="flex flex-col items-center py-10 text-muted-foreground">
            <ShieldOff className="mb-3 h-8 w-8 opacity-50" />
            <p className="text-sm">Você não tem permissão para acessar os módulos do CRM.</p>
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {visibleModules.map((mod) => (
            <CrmModuleCard key={mod.title} mod={mod} />
          ))}
        </div>
      )}

      <div className="space-y-1">
        <PageTitle>Recentes</PageTitle>
        <p className="text-sm text-muted-foreground">Continue de onde você parou.</p>
      </div>
      <CrmRecentItems activities={data?.recentActivities ?? []} />
    </div>
  );
}
