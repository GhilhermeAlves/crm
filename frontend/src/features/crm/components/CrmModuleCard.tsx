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
        <div className="rounded-lg bg-muted p-3">{mod.icon}</div>
        {comingSoon && (
          <span className="rounded-full bg-muted px-2 py-0.5 text-xs font-medium text-muted-foreground">
            Em breve
          </span>
        )}
      </div>
      <div className="mt-4 space-y-1">
        <h3 className="font-semibold">{mod.title}</h3>
        <p className="text-sm text-muted-foreground">{mod.description}</p>
      </div>
      {!comingSoon && (
        <span className="mt-4 inline-flex items-center gap-1 text-sm font-medium text-primary">
          Acessar
          <ArrowRight className="h-4 w-4" />
        </span>
      )}
    </>
  );

  const cardClass = "flex h-full flex-col transition-shadow hover:shadow-md";
  const contentWrapper = <CardContent className="flex h-full flex-col p-6">{content}</CardContent>;

  if (comingSoon) {
    return (
      <Card aria-disabled className={cn(cardClass, "opacity-60")}>
        {contentWrapper}
      </Card>
    );
  }

  return (
    <Link href={mod.href!} className="block h-full">
      <Card className={cn(cardClass, "group")}>{contentWrapper}</Card>
    </Link>
  );
}
