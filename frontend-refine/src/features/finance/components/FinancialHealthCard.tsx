"use client";

import { useMemo, useState } from "react";
import {
  Area,
  AreaChart,
  Bar,
  CartesianGrid,
  ComposedChart,
  Line,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
  type TooltipProps,
} from "recharts";
import { ChevronLeft, ChevronRight } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";

import { useFinanceHealth } from "../hooks/useFinance";
import { centsToBRL } from "../utils/money";

const MONTHS_SHORT = [
  "jan",
  "fev",
  "mar",
  "abr",
  "mai",
  "jun",
  "jul",
  "ago",
  "set",
  "out",
  "nov",
  "dez",
];

const MONTHS_LONG = [
  "janeiro",
  "fevereiro",
  "março",
  "abril",
  "maio",
  "junho",
  "julho",
  "agosto",
  "setembro",
  "outubro",
  "novembro",
  "dezembro",
];

type ChartPoint = {
  month: string;
  label: string;
  fullLabel: string;
  income: number;
  expense: number;
  balance: number;
  incomeCents: number;
  expenseCents: number;
  balanceCents: number;
  accumulated?: number;
  accumulatedCents?: number;
};

const splitMonth = (month: string) => {
  const [year, monthNumber] = month.split("-");
  return { year: Number(year), monthIndex: Number(monthNumber) - 1 };
};

const shortLabel = (month: string) => {
  const { year, monthIndex } = splitMonth(month);
  return `${MONTHS_SHORT[monthIndex] ?? month} ${String(year).slice(2)}`;
};

const longLabel = (month: string) => {
  const { year, monthIndex } = splitMonth(month);
  return `${MONTHS_LONG[monthIndex] ?? month} de ${year}`;
};

const compactBRL = (value: number) => {
  const abs = Math.abs(value);
  const sign = value < 0 ? "-" : "";
  if (abs >= 1000) {
    const thousands = new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 1 }).format(
      abs / 1000,
    );
    return `${sign}R$ ${thousands} mil`;
  }
  return `${sign}R$ ${new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 0 }).format(abs)}`;
};

function renderHealthTooltip({ active, payload }: TooltipProps<number, string>) {
  if (!active || !payload || payload.length === 0) return null;
  const point = payload[0].payload as ChartPoint;
  return (
    <div className="rounded-md border bg-popover p-2.5 text-xs shadow-md">
      <p className="mb-1.5 font-medium capitalize">{point.fullLabel}</p>
      <p className="text-emerald-600 dark:text-emerald-400">
        Receitas: {centsToBRL(point.incomeCents)}
      </p>
      <p className="text-red-600 dark:text-red-400">Despesas: {centsToBRL(point.expenseCents)}</p>
      <p className="text-muted-foreground">Saldo: {centsToBRL(point.balanceCents)}</p>
    </div>
  );
}

function renderAccumulatedTooltip({ active, payload }: TooltipProps<number, string>) {
  if (!active || !payload || payload.length === 0) return null;
  const point = payload[0].payload as ChartPoint;
  return (
    <div className="rounded-md border bg-popover p-2.5 text-xs shadow-md">
      <p className="mb-1.5 font-medium capitalize">{point.fullLabel}</p>
      <p className="text-muted-foreground">
        Saldo acumulado: {centsToBRL(point.accumulatedCents ?? 0)}
      </p>
    </div>
  );
}

type FinancialHealthCardProps = {
  companyId: string | null;
  months: number;
  onMonthsChange: (months: number) => void;
  className?: string;
};

export function FinancialHealthCard({
  companyId,
  months,
  onMonthsChange,
  className,
}: FinancialHealthCardProps) {
  const [slide, setSlide] = useState(0);
  const { data, isLoading, isError, refetch } = useFinanceHealth(companyId, months);

  const chartData = useMemo<ChartPoint[]>(() => {
    const base: ChartPoint[] = (data?.months ?? []).map((month) => ({
      month: month.month,
      label: shortLabel(month.month),
      fullLabel: longLabel(month.month),
      income: month.incomeCents / 100,
      expense: month.expenseCents / 100,
      balance: month.balanceCents / 100,
      incomeCents: month.incomeCents,
      expenseCents: month.expenseCents,
      balanceCents: month.balanceCents,
    }));

    let accumulated = 0;
    return base.map((point) => {
      accumulated += point.balanceCents;
      return { ...point, accumulated: accumulated / 100, accumulatedCents: accumulated };
    });
  }, [data]);

  return (
    <Card className={className}>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle className="text-base font-semibold">Saúde financeira</CardTitle>
        <div className="flex items-center gap-1 rounded-full border p-0.5">
          {[6, 12].map((option) => (
            <button
              key={option}
              type="button"
              onClick={() => onMonthsChange(option)}
              aria-pressed={months === option}
              className={cn(
                "rounded-full px-2.5 py-1 text-xs font-medium transition-colors",
                months === option
                  ? "border border-primary bg-primary/10 text-primary"
                  : "border border-transparent text-muted-foreground hover:bg-muted",
              )}
            >
              {option} meses
            </button>
          ))}
        </div>
      </CardHeader>

      <CardContent className="space-y-2">
        {isLoading ? (
          <Skeleton className="h-[280px] w-full" />
        ) : isError ? (
          <div className="flex h-[280px] flex-col items-center justify-center gap-3 text-center">
            <p className="text-sm text-muted-foreground">Não foi possível carregar o gráfico.</p>
            <Button variant="outline" size="sm" onClick={() => refetch()}>
              Tentar novamente
            </Button>
          </div>
        ) : (
          <>
            {slide === 0 ? (
              <div className="flex flex-wrap items-center gap-4 text-xs text-muted-foreground">
                <span className="flex items-center gap-1.5">
                  <span className="h-2 w-2 rounded-full bg-emerald-500" />
                  Receitas
                </span>
                <span className="flex items-center gap-1.5">
                  <span className="h-2 w-2 rounded-full bg-red-500" />
                  Despesas
                </span>
                <span className="flex items-center gap-1.5">
                  <span className="h-0.5 w-4 rounded bg-muted-foreground" />
                  Saldo
                </span>
              </div>
            ) : (
              <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                <span className="h-0.5 w-4 rounded bg-muted-foreground" />
                Saldo acumulado
              </div>
            )}

            <div className="h-[240px] w-full">
              <ResponsiveContainer width="100%" height="100%">
                {slide === 0 ? (
                  <ComposedChart data={chartData} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                    <CartesianGrid
                      vertical={false}
                      strokeDasharray="3 3"
                      stroke="hsl(var(--border))"
                    />
                    <XAxis
                      dataKey="label"
                      tickLine={false}
                      axisLine={false}
                      tick={{ fontSize: 12, fill: "hsl(var(--muted-foreground))" }}
                    />
                    <YAxis
                      tickLine={false}
                      axisLine={false}
                      width={56}
                      tickFormatter={compactBRL}
                      tick={{ fontSize: 12, fill: "hsl(var(--muted-foreground))" }}
                    />
                    <Tooltip
                      content={renderHealthTooltip}
                      cursor={{ fill: "hsl(var(--muted) / 0.4)" }}
                    />
                    <Bar
                      dataKey="income"
                      name="Receitas"
                      fill="hsl(var(--crm-success))"
                      barSize={10}
                      radius={[4, 4, 0, 0]}
                    />
                    <Bar
                      dataKey="expense"
                      name="Despesas"
                      fill="hsl(var(--crm-danger))"
                      barSize={10}
                      radius={[4, 4, 0, 0]}
                    />
                    <Line
                      type="monotone"
                      dataKey="balance"
                      name="Saldo"
                      stroke="hsl(var(--muted-foreground))"
                      strokeWidth={2}
                      dot={{ r: 3, fill: "hsl(var(--background))", strokeWidth: 2 }}
                      activeDot={{ r: 4 }}
                    />
                  </ComposedChart>
                ) : (
                  <AreaChart data={chartData} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                    <defs>
                      <linearGradient id="accumulatedFill" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stopColor="hsl(var(--primary))" stopOpacity={0.25} />
                        <stop offset="100%" stopColor="hsl(var(--primary))" stopOpacity={0} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid
                      vertical={false}
                      strokeDasharray="3 3"
                      stroke="hsl(var(--border))"
                    />
                    <XAxis
                      dataKey="label"
                      tickLine={false}
                      axisLine={false}
                      tick={{ fontSize: 12, fill: "hsl(var(--muted-foreground))" }}
                    />
                    <YAxis
                      tickLine={false}
                      axisLine={false}
                      width={56}
                      tickFormatter={compactBRL}
                      tick={{ fontSize: 12, fill: "hsl(var(--muted-foreground))" }}
                    />
                    <Tooltip content={renderAccumulatedTooltip} />
                    <Area
                      type="monotone"
                      dataKey="accumulated"
                      name="Saldo acumulado"
                      stroke="hsl(var(--primary))"
                      strokeWidth={2}
                      fill="url(#accumulatedFill)"
                    />
                  </AreaChart>
                )}
              </ResponsiveContainer>
            </div>

            <div className="flex items-center justify-center gap-3 pt-1">
              <button
                type="button"
                onClick={() => setSlide(0)}
                aria-label="Slide anterior"
                className="text-muted-foreground transition-colors hover:text-foreground"
              >
                <ChevronLeft className="h-4 w-4" />
              </button>
              <div className="flex items-center gap-1.5">
                {[0, 1].map((index) => (
                  <button
                    key={index}
                    type="button"
                    onClick={() => setSlide(index)}
                    aria-label={`Ir para o slide ${index + 1}`}
                    className={cn(
                      "h-1.5 rounded-full transition-all",
                      slide === index ? "w-4 bg-primary" : "w-1.5 bg-muted-foreground/40",
                    )}
                  />
                ))}
              </div>
              <button
                type="button"
                onClick={() => setSlide(1)}
                aria-label="Próximo slide"
                className="text-muted-foreground transition-colors hover:text-foreground"
              >
                <ChevronRight className="h-4 w-4" />
              </button>
            </div>
          </>
        )}
      </CardContent>
    </Card>
  );
}
