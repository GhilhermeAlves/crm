"use client";

import Link from "next/link";
import { type ReactNode } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { cn } from "@/lib/utils";

export type CrmRecentItem = {
  id: string;
  title: string;
  subtitle?: string | null;
  icon: ReactNode;
  timeLabel?: string | null;
  href?: string;
};

export function CrmRecentCard({ item }: { item: CrmRecentItem }) {
  const content = (
    <CardContent className="flex h-full flex-col gap-3 p-4">
      <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-muted">
        {item.icon}
      </div>
      <div className="min-w-0 flex-1 space-y-0.5">
        <p className="truncate text-sm font-medium">{item.title}</p>
        {item.subtitle && <p className="truncate text-xs text-muted-foreground">{item.subtitle}</p>}
        {item.timeLabel && <p className="text-xs text-muted-foreground/80">{item.timeLabel}</p>}
      </div>
    </CardContent>
  );

  const cardClass = "flex h-full min-h-[114px] transition-colors hover:bg-muted/30";

  if (item.href) {
    return (
      <Link href={item.href} className="block h-full">
        <Card className={cn(cardClass, "group border-border/60 hover:border-primary/40")}>
          {content}
        </Card>
      </Link>
    );
  }

  return <Card className={cn(cardClass, "border-border/60")}>{content}</Card>;
}
