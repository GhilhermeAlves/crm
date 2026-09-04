"use client";

import { Badge } from "@/components/ui/badge";

export function DealForecastBadge({ category }: { category: string | null }) {
  if (!category) {
    return <span className="text-sm text-muted-foreground">—</span>;
  }
  return (
    <Badge variant="secondary" className="whitespace-nowrap font-medium">
      {category}
    </Badge>
  );
}
