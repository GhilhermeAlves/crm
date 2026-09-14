"use client";

import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

export const statusBadgeVariants = cva(
  "inline-flex items-center gap-1.5 whitespace-nowrap rounded-full font-medium transition-colors border",
  {
    variants: {
      intent: {
        neutral:
          "bg-muted/70 text-muted-foreground border-border/70 dark:bg-muted/40 dark:text-muted-foreground",
        success:
          "bg-emerald-50 text-emerald-800 border-emerald-200/80 dark:bg-emerald-950/40 dark:text-emerald-300 dark:border-emerald-800/50",
        warning:
          "bg-amber-50 text-amber-900 border-amber-200/80 dark:bg-amber-950/40 dark:text-amber-300 dark:border-amber-800/50",
        danger:
          "bg-rose-50 text-rose-800 border-rose-200/80 dark:bg-rose-950/40 dark:text-rose-300 dark:border-rose-800/50",
        info:
          "bg-blue-50 text-blue-800 border-blue-200/80 dark:bg-blue-950/40 dark:text-blue-300 dark:border-blue-800/50",
        indigo:
          "bg-indigo-50 text-indigo-800 border-indigo-200/80 dark:bg-indigo-950/40 dark:text-indigo-300 dark:border-indigo-800/50",
        purple:
          "bg-purple-50 text-purple-800 border-purple-200/80 dark:bg-purple-950/40 dark:text-purple-300 dark:border-purple-800/50",
      },
      size: {
        sm: "text-[11px] px-2 py-0.5",
        default: "text-xs px-2.5 py-0.5",
        lg: "text-sm px-3 py-1",
      },
      appearance: {
        subtle: "",
        solid: "border-transparent text-white",
        outline: "bg-transparent",
      },
    },
    compoundVariants: [
      {
        intent: "success",
        appearance: "solid",
        className: "bg-emerald-600 text-white dark:bg-emerald-600",
      },
      {
        intent: "warning",
        appearance: "solid",
        className: "bg-amber-500 text-slate-950 dark:bg-amber-500 dark:text-slate-950",
      },
      {
        intent: "danger",
        appearance: "solid",
        className: "bg-rose-600 text-white dark:bg-rose-600",
      },
      {
        intent: "info",
        appearance: "solid",
        className: "bg-blue-600 text-white dark:bg-blue-600",
      },
      {
        intent: "indigo",
        appearance: "solid",
        className: "bg-indigo-600 text-white dark:bg-indigo-600",
      },
      {
        intent: "purple",
        appearance: "solid",
        className: "bg-purple-600 text-white dark:bg-purple-600",
      },
      {
        intent: "neutral",
        appearance: "solid",
        className: "bg-muted-foreground text-background",
      },
    ],
    defaultVariants: {
      intent: "neutral",
      size: "default",
      appearance: "subtle",
    },
  },
);

export type StatusIntent = NonNullable<VariantProps<typeof statusBadgeVariants>["intent"]>;

export interface StatusBadgeProps
  extends React.HTMLAttributes<HTMLSpanElement>,
    VariantProps<typeof statusBadgeVariants> {
  /** Adiciona um círculo indicador à esquerda do texto */
  withDot?: boolean;
  /** Faz o indicador de status pulsar suavemente (útil para canais online, SLA ou estados ativos) */
  pulseDot?: boolean;
  /** Ícone exibido antes do texto */
  icon?: React.ReactNode;
}

export const StatusBadge = React.forwardRef<HTMLSpanElement, StatusBadgeProps>(
  (
    {
      className,
      intent = "neutral",
      size = "default",
      appearance = "subtle",
      withDot = false,
      pulseDot = false,
      icon,
      children,
      ...props
    },
    ref,
  ) => {
    return (
      <span
        ref={ref}
        className={cn(statusBadgeVariants({ intent, size, appearance, className }))}
        {...props}
      >
        {withDot && (
          <span className="relative flex h-2 w-2 shrink-0 items-center justify-center">
            {pulseDot && (
              <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-current opacity-75" />
            )}
            <span className="relative inline-flex h-1.5 w-1.5 rounded-full bg-current" />
          </span>
        )}
        {icon && <span className="shrink-0">{icon}</span>}
        <span>{children}</span>
      </span>
    );
  },
);
StatusBadge.displayName = "StatusBadge";

// =============================================================================
// PRESETS POLIMÓRFICOS DE NEGÓCIO (LEADS, DEALS, USUÁRIOS, CANAIS)
// =============================================================================

// Preset de Leads
const LEAD_STATUS_CONFIG: Record<string, { label: string; intent: StatusIntent }> = {
  NEW: { label: "Novo", intent: "info" },
  CONTACTED: { label: "Contatado", intent: "indigo" },
  QUALIFIED: { label: "Qualificado", intent: "success" },
  UNQUALIFIED: { label: "Não qualificado", intent: "warning" },
  CONVERTED: { label: "Convertido", intent: "success" },
  LOST: { label: "Perdido", intent: "danger" },
};

export interface LeadStatusBadgePresetProps extends Omit<StatusBadgeProps, "intent" | "children"> {
  status: string;
}

export function LeadStatusBadgePreset({
  status,
  withDot = true,
  ...props
}: LeadStatusBadgePresetProps) {
  const config = LEAD_STATUS_CONFIG[status] ?? { label: status || "Novo", intent: "info" };
  return (
    <StatusBadge intent={config.intent} withDot={withDot} {...props}>
      {config.label}
    </StatusBadge>
  );
}

// Preset de Pipeline / Oportunidades (Deals)
const DEAL_STAGE_CONFIG: Record<string, { label: string; intent: StatusIntent }> = {
  Novo: { label: "Novo", intent: "info" },
  Descoberta: { label: "Descoberta", intent: "indigo" },
  Proposta: { label: "Proposta", intent: "warning" },
  Negociação: { label: "Negociação", intent: "purple" },
  "Fechado/Ganho": { label: "Fechado/Ganho", intent: "success" },
  Perdido: { label: "Perdido", intent: "danger" },
};

export interface DealStageBadgePresetProps extends Omit<StatusBadgeProps, "intent" | "children"> {
  stage: string;
}

export function DealStageBadgePreset({
  stage,
  withDot = true,
  ...props
}: DealStageBadgePresetProps) {
  const config = DEAL_STAGE_CONFIG[stage] ?? { label: stage || "Novo", intent: "info" };
  return (
    <StatusBadge intent={config.intent} withDot={withDot} {...props}>
      {config.label}
    </StatusBadge>
  );
}

// Preset de Usuários
const USER_STATUS_CONFIG: Record<string, { label: string; intent: StatusIntent }> = {
  active: { label: "Ativo", intent: "success" },
  inactive: { label: "Inativo", intent: "neutral" },
  locked: { label: "Bloqueado", intent: "danger" },
  pending: { label: "Pendente", intent: "warning" },
};

export interface UserStatusBadgePresetProps extends Omit<StatusBadgeProps, "intent" | "children"> {
  status: string;
}

export function UserStatusBadgePreset({
  status,
  withDot = true,
  ...props
}: UserStatusBadgePresetProps) {
  const config = USER_STATUS_CONFIG[status] ?? { label: status || "Ativo", intent: "neutral" };
  return (
    <StatusBadge intent={config.intent} withDot={withDot} {...props}>
      {config.label}
    </StatusBadge>
  );
}

// Preset de Canais Omnichannel
const CHANNEL_STATUS_CONFIG: Record<string, { label: string; intent: StatusIntent; pulse?: boolean }> = {
  ACTIVE: { label: "Conectado", intent: "success", pulse: true },
  CONNECTED: { label: "Conectado", intent: "success", pulse: true },
  INACTIVE: { label: "Desconectado", intent: "neutral" },
  DISCONNECTED: { label: "Desconectado", intent: "neutral" },
  CONNECTING: { label: "Conectando...", intent: "warning", pulse: true },
  ERROR: { label: "Falha de Conexão", intent: "danger" },
};

export interface ChannelStatusBadgePresetProps extends Omit<StatusBadgeProps, "intent" | "children"> {
  status: string;
}

export function ChannelStatusBadgePreset({
  status,
  withDot = true,
  pulseDot,
  ...props
}: ChannelStatusBadgePresetProps) {
  const config = CHANNEL_STATUS_CONFIG[status] ?? { label: status || "Inativo", intent: "neutral" };
  return (
    <StatusBadge
      intent={config.intent}
      withDot={withDot}
      pulseDot={pulseDot ?? config.pulse}
      {...props}
    >
      {config.label}
    </StatusBadge>
  );
}
