"use client";

import { type ReactNode } from "react";
import { cn } from "@/lib/utils";
import { Card, CardContent } from "@/components/ui/card";

type CardStatProps = {
  title: string;
  value: string | number;
  description?: string;
  icon: ReactNode;
  trend?: {
    value: number;
    isPositive: boolean;
  };
  className?: string;
};

export function CardStat({ title, value, description, icon, trend, className }: CardStatProps) {
  return (
    <Card className={cn("transition-shadow hover:shadow-md", className)}>
      <CardContent className="p-6">
        <div className="flex items-center justify-between">
          <div className="space-y-1">
            <p className="text-sm font-medium text-muted-foreground">{title}</p>
            <p className="text-2xl font-bold">{value}</p>
            <div className="flex items-center gap-2">
              {trend && (
                <span
                  className={cn(
                    "text-xs font-medium",
                    trend.isPositive
                      ? "text-emerald-600 dark:text-emerald-400"
                      : "text-red-600 dark:text-red-400",
                  )}
                >
                  {trend.isPositive ? "+" : ""}
                  {trend.value}%
                </span>
              )}
              {description && <p className="text-xs text-muted-foreground">{description}</p>}
            </div>
          </div>
          <div className="rounded-lg bg-muted p-3">{icon}</div>
        </div>
      </CardContent>
    </Card>
  );
}
