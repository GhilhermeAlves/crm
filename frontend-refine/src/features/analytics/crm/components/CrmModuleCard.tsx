"use client";

import Link from "next/link";
import { type ReactNode } from "react";
import { ArrowRight } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { cn } from "@/lib/utils";

export type CrmModule = {
  title: string;
  description: string;
  icon: ReactNode;
  href?: string;
  permission?: string;
  comingSoon?: boolean;
};

export function CrmModuleCard({ mod }: { mod: CrmModule }) {
  const comingSoon = mod.comingSoon || !mod.href;

  const content = (
    <>
      <div className="flex items-start justify-between gap-2">
        <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-muted">
          {mod.icon}
        </div>
        {comingSoon && (
          <span className="rounded-full bg-muted px-2 py-0.5 text-xs font-medium text-muted-foreground">
            Em breve
          </span>
        )}
      </div>
      <div className="mt-3 space-y-1">
        <h3 className="text-sm font-semibold">{mod.title}</h3>
        <p className="text-xs leading-snug text-muted-foreground">{mod.description}</p>
      </div>
      {!comingSoon && (
        <span className="mt-3 inline-flex items-center gap-1 text-xs font-medium text-primary">
          Acessar
          <ArrowRight className="h-3.5 w-3.5" />
        </span>
      )}
    </>
  );

  const cardClass = "flex h-full min-h-[108px] flex-col transition-colors";
  const contentWrapper = <CardContent className="flex h-full flex-col p-4">{content}</CardContent>;

  if (comingSoon) {
    return (
      <Card aria-disabled className={cn(cardClass, "border-border/60 opacity-60")}>
        {contentWrapper}
      </Card>
    );
  }

  return (
    <Link href={mod.href!} className="block h-full">
      <Card
        className={cn(
          cardClass,
          "group border-border/60 hover:border-primary/40 hover:bg-muted/30",
        )}
      >
        {contentWrapper}
      </Card>
    </Link>
  );
}
