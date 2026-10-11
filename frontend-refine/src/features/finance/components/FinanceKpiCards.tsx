import { CalendarClock, TrendingDown, TrendingUp, Wallet, type LucideIcon } from "lucide-react";

import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";

import type { SummaryResponse } from "../types/finance.types";
import { centsToBRL } from "../utils/money";
import type { FinanceTab } from "./FinanceTypeTabs";

type KpiTone = "income" | "expense" | "neutral";

type KpiDefinition = {
  label: string;
  valueCents: number;
  icon: LucideIcon;
  tone: KpiTone;
  negativeRed?: boolean;
};

type KpiCardProps = {
  label: string;
  valueCents: number;
  icon: LucideIcon;
  tone: KpiTone;
  negativeRed?: boolean;
  isLoading: boolean;
};

const TONE_CLASSES: Record<KpiTone, string> = {
  income: "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400",
  expense: "bg-red-500/10 text-red-600 dark:text-red-400",
  neutral: "bg-muted text-muted-foreground",
};

function KpiCard({ label, valueCents, icon: Icon, tone, negativeRed, isLoading }: KpiCardProps) {
  const isNegative = negativeRed && valueCents < 0;

  return (
    <Card>
      <CardContent className="flex items-center gap-3 p-4">
        <span
          className={cn(
            "flex h-10 w-10 shrink-0 items-center justify-center rounded-full",
            TONE_CLASSES[tone],
          )}
        >
          <Icon className="h-5 w-5" />
        </span>
        <div className="min-w-0">
          <p className="text-xs text-muted-foreground">{label}</p>
          {isLoading ? (
            <Skeleton className="mt-1.5 h-6 w-24" />
          ) : valueCents === 0 ? (
            <p className="mt-0.5 text-sm text-muted-foreground">Sem registros</p>
          ) : (
            <p
              className={cn(
                "mt-0.5 text-lg font-semibold tabular-nums",
                isNegative && "text-red-600 dark:text-red-400",
              )}
            >
              {centsToBRL(valueCents)}
            </p>
          )}
        </div>
      </CardContent>
    </Card>
  );
}

type FinanceKpiCardsProps = {
  tab: FinanceTab;
  summary?: SummaryResponse;
  isLoading?: boolean;
};

function buildKpis(tab: FinanceTab, summary?: SummaryResponse): KpiDefinition[] {
  const incomeTotalCents = summary?.incomeTotalCents ?? 0;
  const expenseTotalCents = summary?.expenseTotalCents ?? 0;
  const balanceCents = summary?.balanceCents ?? 0;
  const incomeForecastCents = summary?.incomeForecastCents ?? 0;
  const expenseForecastCents = summary?.expenseForecastCents ?? 0;

  if (tab === "income") {
    return [
      { label: "Entradas totais", valueCents: incomeTotalCents, icon: TrendingUp, tone: "income" },
      {
        label: "Total previsto",
        valueCents: incomeForecastCents,
        icon: CalendarClock,
        tone: "neutral",
      },
    ];
  }

  if (tab === "expense") {
    return [
      {
        label: "Saídas totais",
        valueCents: expenseTotalCents,
        icon: TrendingDown,
        tone: "expense",
      },
      {
        label: "Total previsto",
        valueCents: expenseForecastCents,
        icon: CalendarClock,
        tone: "neutral",
      },
    ];
  }

  return [
    { label: "Receitas", valueCents: incomeTotalCents, icon: TrendingUp, tone: "income" },
    { label: "Despesas", valueCents: expenseTotalCents, icon: TrendingDown, tone: "expense" },
    {
      label: "Saldo",
      valueCents: balanceCents,
      icon: Wallet,
      tone: "neutral",
      negativeRed: true,
    },
  ];
}

export function FinanceKpiCards({ tab, summary, isLoading = false }: FinanceKpiCardsProps) {
  const kpis = buildKpis(tab, summary);

  return (
    <div className={cn("grid gap-4", kpis.length === 3 ? "sm:grid-cols-3" : "sm:grid-cols-2")}>
      {kpis.map((kpi) => (
        <KpiCard
          key={kpi.label}
          label={kpi.label}
          valueCents={kpi.valueCents}
          icon={kpi.icon}
          tone={kpi.tone}
          negativeRed={kpi.negativeRed}
          isLoading={isLoading}
        />
      ))}
    </div>
  );
}
