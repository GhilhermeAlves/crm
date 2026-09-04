"use client";

import {
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
import { useAuthorization } from "@/features/auth/hooks/useAuthorization";
import { CrmModuleCard, type CrmModule } from "@/features/crm/components/CrmModuleCard";

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
  const { can } = useAuthorization();

  const visibleModules = modules.filter((m) => !m.permission || can(m.permission));

  return (
    <div className="space-y-6">
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
