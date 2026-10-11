import type { ReactNode } from "react";
import { BarChart3 } from "lucide-react";

import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";

export type ReportLegendItem = {
  label: string;
  color: string;
};

type ReportCardProps = {
  title: string;
  legend?: ReportLegendItem[];
  isEmpty?: boolean;
  emptyHeight?: number;
  children?: ReactNode;
  className?: string;
  bodyClassName?: string;
};

export function ReportCard({
  title,
  legend,
  isEmpty,
  emptyHeight = 200,
  children,
  className,
  bodyClassName,
}: ReportCardProps) {
  return (
    <Card className={cn("flex flex-col", className)}>
      <div className="flex items-start justify-between gap-2 p-4 pb-0">
        <h3 className="text-sm font-semibold">{title}</h3>
        <span aria-hidden="true" className="text-muted-foreground">
          &#8943;
        </span>
      </div>

      {legend && legend.length > 0 && (
        <div className="flex flex-wrap items-center gap-3 px-4 pt-2 text-xs text-muted-foreground">
          {legend.map((item) => (
            <span key={item.label} className="flex items-center gap-1.5">
              <span
                className="h-2 w-2 shrink-0 rounded-full"
                style={{ backgroundColor: item.color }}
              />
              {item.label}
            </span>
          ))}
        </div>
      )}

      <div className={cn("flex-1 p-4", bodyClassName)}>
        {isEmpty ? (
          <div
            className="flex flex-col items-center justify-center text-center text-muted-foreground"
            style={{ minHeight: emptyHeight }}
          >
            <BarChart3 className="mb-2 h-8 w-8 opacity-40" />
            <p className="text-sm">Sem resultados</p>
          </div>
        ) : (
          children
        )}
      </div>
    </Card>
  );
}
