import Link from "next/link";
import {
  BarChart3,
  CalendarDays,
  LayoutDashboard,
  PieChart,
  Plus,
  Wallet,
  type LucideIcon,
} from "lucide-react";

import { Card } from "@/components/ui/card";
import { ROUTES } from "@/lib/constants";

type ReportCardDefinition = {
  label: string;
  description: string;
  href: string;
  icon: LucideIcon;
  permission: string | null;
};

const REPORT_CARDS: ReportCardDefinition[] = [
  {
    label: "Visão geral",
    description: "Indicadores gerais de atendimentos e receita da clínica.",
    href: ROUTES.REPORTS_OVERVIEW,
    icon: LayoutDashboard,
    permission: null,
  },
  {
    label: "Agenda",
    description: "Comparecimentos, faltas e taxa de ocupação da agenda.",
    href: ROUTES.REPORTS_AGENDA,
    icon: CalendarDays,
    permission: null,
  },
  {
    label: "Analytics do CRM",
    description: "Leads, oportunidades e mensagens em linha do tempo.",
    href: ROUTES.REPORTS_ANALYTICS,
    icon: BarChart3,
    permission: "analytics:read",
  },
  {
    label: "Fluxo de caixa",
    description: "Entradas e saídas de caixa agrupadas por período.",
    href: ROUTES.REPORTS_CASH_FLOW,
    icon: Wallet,
    permission: "finance:read",
  },
  {
    label: "Distribuição de receita",
    description: "Receita por procedimento e forma de pagamento.",
    href: ROUTES.REPORTS_REVENUE,
    icon: PieChart,
    permission: "finance:read",
  },
];

type ReportCatalogGridProps = {
  can: (permission: string) => boolean;
  onSuggest: () => void;
};

export function ReportCatalogGrid({ can, onSuggest }: ReportCatalogGridProps) {
  const cards = REPORT_CARDS.filter((card) => !card.permission || can(card.permission));

  return (
    <div className="grid gap-4 sm:grid-cols-2">
      {cards.map(({ label, description, href, icon: Icon }) => (
        <Link key={href} href={href} className="group focus:outline-none">
          <Card className="flex h-full items-center gap-4 p-4 transition-colors group-hover:border-primary/40 group-hover:bg-muted/40 group-focus-visible:ring-2 group-focus-visible:ring-ring">
            <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-md bg-muted">
              <Icon className="h-5 w-5 text-muted-foreground" />
            </span>
            <span className="min-w-0 flex-1">
              <span className="block text-sm font-semibold">{label}</span>
              <span className="block text-xs text-muted-foreground">{description}</span>
            </span>
          </Card>
        </Link>
      ))}

      <Card className="flex h-full items-center gap-4 border-dashed p-4">
        <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-md bg-primary/10 text-primary">
          <Plus className="h-5 w-5" />
        </span>
        <span className="min-w-0 flex-1">
          <span className="block text-sm font-semibold">Sugerir um relatório</span>
          <span className="block text-xs text-muted-foreground">
            Não encontrou o que precisava? Conte pra gente.
          </span>
        </span>
        <button
          type="button"
          onClick={onSuggest}
          className="shrink-0 text-sm font-medium text-primary underline-offset-4 hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        >
          Quero sugerir
        </button>
      </Card>
    </div>
  );
}
