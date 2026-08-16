"use client";

import { ArrowRight, CheckCircle2, Sparkles, Zap } from "lucide-react";
import type { ReactNode } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { cn } from "@/lib/utils";
import type { NextAction } from "../types/contact.types";

const META: Record<
  NextAction["type"],
  { label: string; icon: ReactNode; className: string }
> = {
  FOLLOW_UP: {
    label: "Agendar follow-up",
    icon: <Zap className="h-5 w-5" />,
    className: "bg-amber-500/10 text-amber-600",
  },
  COMPLETE_TASK: {
    label: "Concluir tarefa",
    icon: <CheckCircle2 className="h-5 w-5" />,
    className: "bg-blue-500/10 text-blue-600",
  },
  REVIEW_CLOSING: {
    label: "Revisar fechamento",
    icon: <ArrowRight className="h-5 w-5" />,
    className: "bg-emerald-500/10 text-emerald-600",
  },
  FORMAL_PROPOSAL: {
    label: "Enviar proposta formal",
    icon: <Sparkles className="h-5 w-5" />,
    className: "bg-purple-500/10 text-purple-600",
  },
  NONE: {
    label: "Tudo em dia",
    icon: <CheckCircle2 className="h-5 w-5" />,
    className: "bg-muted text-muted-foreground",
  },
};

export function NextActionCard({ nextAction }: { nextAction: NextAction }) {
  const meta = META[nextAction.type] ?? META.NONE;
  return (
    <Card>
      <CardContent className="flex items-start gap-3 p-4">
        <span
          className={cn(
            "flex h-10 w-10 shrink-0 items-center justify-center rounded-lg",
            meta.className,
          )}
        >
          {meta.icon}
        </span>
        <div className="min-w-0 space-y-0.5">
          <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
            Próxima ação
          </p>
          <p className="text-sm font-semibold">{meta.label}</p>
          <p className="text-xs text-muted-foreground">
            {nextAction.description}
          </p>
        </div>
      </CardContent>
    </Card>
  );
}
