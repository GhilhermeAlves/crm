"use client";

import { TrendingUp, Trophy, XCircle, Target } from "lucide-react";
import type { PipelineMetrics } from "../types/pipeline.types";
import { formatCurrency, formatPercent } from "../schemas/pipeline.schema";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

interface PipelineMetricsStripProps {
  metrics: PipelineMetrics | undefined;
  isLoading?: boolean;
}

export function PipelineMetricsStrip({ metrics, isLoading }: PipelineMetricsStripProps) {
  if (isLoading || !metrics) {
    return (
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="h-24 animate-pulse rounded-lg bg-muted" />
        ))}
      </div>
    );
  }

  const stats = [
    {
      label: "Funil aberto",
      value: formatCurrency(metrics.totalValue),
      sub: `${metrics.openCount} oportunidades`,
      icon: Target,
    },
    {
      label: "Forecast",
      value: formatCurrency(metrics.forecast),
      sub: "ponderado pela probabilidade",
      icon: TrendingUp,
    },
    {
      label: "Ganhas",
      value: String(metrics.wonCount),
      sub: formatCurrency(metrics.wonValue),
      icon: Trophy,
    },
    {
      label: "Perdidas",
      value: String(metrics.lostCount),
      sub: `${formatCurrency(metrics.lostValue)} · win rate ${formatPercent(metrics.winRate)}`,
      icon: XCircle,
    },
  ];

  return (
    <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
      {stats.map((stat) => {
        const Icon = stat.icon;
        return (
          <Card key={stat.label}>
            <CardHeader className="pb-1">
              <CardTitle className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
                <Icon className="h-4 w-4" />
                {stat.label}
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold">{stat.value}</p>
              <p className="text-xs text-muted-foreground">{stat.sub}</p>
            </CardContent>
          </Card>
        );
      })}
    </div>
  );
}
