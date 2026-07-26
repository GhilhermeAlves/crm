import { Users, UserCheck, UserX, UserPlus, Shield, Activity } from "lucide-react";
import { KpiCard } from "./KpiCard";
import type { DashboardKpis } from "../types/dashboard.types";

type KpiGridProps = {
  kpis: DashboardKpis;
};

export function KpiGrid({ kpis }: KpiGridProps) {
  const kpiCards = [
    {
      title: "Total de Usuários",
      value: kpis.totalUsers,
      description: "Cadastrados",
      icon: Users,
    },
    {
      title: "Usuários Ativos",
      value: kpis.activeUsers,
      description: `de ${kpis.totalUsers} total`,
      icon: UserCheck,
      trend: {
        value: kpis.totalUsers > 0 ? Math.round((kpis.activeUsers / kpis.totalUsers) * 100) : 0,
        isPositive: true,
      },
    },
    {
      title: "Usuários Inativos",
      value: kpis.inactiveUsers,
      description: "Desativados",
      icon: UserX,
    },
    {
      title: "Novos Usuários",
      value: kpis.newUsersThisMonth,
      description: "Este mês",
      icon: UserPlus,
    },
    {
      title: "Eventos de Auditoria",
      value: kpis.auditEventsThisMonth,
      description: "Este mês",
      icon: Shield,
    },
    {
      title: "Total de Eventos",
      value: kpis.totalAuditEvents,
      description: "Acumulados",
      icon: Activity,
    },
  ];

  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {kpiCards.map((kpi) => (
        <KpiCard
          key={kpi.title}
          title={kpi.title}
          value={kpi.value}
          description={kpi.description}
          icon={kpi.icon}
          trend={"trend" in kpi ? (kpi as { trend: { value: number; isPositive: boolean } }).trend : undefined}
        />
      ))}
    </div>
  );
}
