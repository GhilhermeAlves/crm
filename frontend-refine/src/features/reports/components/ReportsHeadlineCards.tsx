import { CalendarCheck, FileText, TrendingUp } from "lucide-react";

import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";

import { centsToBRL } from "@/features/finance/utils/money";

type ReportsHeadlineCardsProps = {
  revenueThisMonthCents: number;
  attendancesThisWeek: number;
  budgetsCreatedThisMonth: number;
  isLoading?: boolean;
};

type HeadlineCardProps = {
  label: string;
  value: string;
  footer: string;
  icon: typeof TrendingUp;
  toneClass: string;
  isLoading: boolean;
};

function HeadlineCard({
  label,
  value,
  footer,
  icon: Icon,
  toneClass,
  isLoading,
}: HeadlineCardProps) {
  return (
    <Card>
      <CardContent className="space-y-3 p-4">
        <div className="flex items-center gap-3">
          <span
            className={cn(
              "flex h-10 w-10 shrink-0 items-center justify-center rounded-full",
              toneClass,
            )}
          >
            <Icon className="h-5 w-5" />
          </span>
          <p className="text-sm text-muted-foreground">{label}</p>
        </div>
        {isLoading ? (
          <Skeleton className="h-7 w-28" />
        ) : (
          <p className="text-2xl font-semibold tabular-nums">{value}</p>
        )}
        <p className="text-xs text-muted-foreground">{footer}</p>
      </CardContent>
    </Card>
  );
}

export function ReportsHeadlineCards({
  revenueThisMonthCents,
  attendancesThisWeek,
  budgetsCreatedThisMonth,
  isLoading = false,
}: ReportsHeadlineCardsProps) {
  return (
    <div className="grid gap-4 sm:grid-cols-3">
      <HeadlineCard
        label="Receita gerada"
        value={centsToBRL(revenueThisMonthCents)}
        footer="Dados deste mês"
        icon={TrendingUp}
        toneClass="bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"
        isLoading={isLoading}
      />
      <HeadlineCard
        label="Comparecimentos"
        value={`${attendancesThisWeek.toLocaleString("pt-BR")} consultas`}
        footer="Dados desta semana"
        icon={CalendarCheck}
        toneClass="bg-cyan-500/10 text-cyan-600 dark:text-cyan-400"
        isLoading={isLoading}
      />
      <HeadlineCard
        label="Orçamentos"
        value={`${budgetsCreatedThisMonth.toLocaleString("pt-BR")} criados`}
        footer="Dados deste mês"
        icon={FileText}
        toneClass="bg-amber-500/10 text-amber-600 dark:text-amber-400"
        isLoading={isLoading}
      />
    </div>
  );
}
