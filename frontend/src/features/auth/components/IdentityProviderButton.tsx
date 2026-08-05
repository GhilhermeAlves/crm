"use client";

import { Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";
import { IDENTITY_PROVIDER_ICONS } from "./identity-provider-icons";
import type { IdentityProviderInfo } from "../types/identity-provider";

/**
 * Botão único de provedor de identidade (Sprint 7.0). Abstrai o estado
 * compartilhado por todos os provedores — ícone, label, loading, disabled,
 * erro e processamento — para que nenhuma lógica seja duplicada por botão.
 * Meta/Facebook não existe nesta tela.
 */
export type IdentityProviderButtonProps = {
  provider: IdentityProviderInfo;
  loading?: boolean;
  disabled?: boolean;
  error?: string | null;
  onSelect: (provider: IdentityProviderInfo) => void;
};

export function IdentityProviderButton({
  provider,
  loading = false,
  disabled = false,
  error,
  onSelect,
}: IdentityProviderButtonProps) {
  const Icon = IDENTITY_PROVIDER_ICONS[provider.alias];
  const isDisabled = disabled || loading || !provider.available;

  return (
    <div className="w-full space-y-1.5">
      <button
        type="button"
        onClick={() => onSelect(provider)}
        disabled={isDisabled}
        aria-label={`Entrar com ${provider.label}`}
        data-provider={provider.alias}
        data-testid={`provider-${provider.alias}`}
        className={cn(
          "relative flex h-11 w-full items-center justify-center gap-3 rounded-lg border border-crm-border bg-crm-surface px-4 text-sm font-medium text-crm-text",
          "transition-colors hover:bg-crm-secondary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-crm-primary focus-visible:ring-offset-2",
          "disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:bg-crm-surface",
        )}
      >
        {loading ? (
          <Loader2
            className="h-5 w-5 animate-spin text-crm-text-secondary"
            data-testid="provider-loading"
          />
        ) : (
          <Icon className="h-5 w-5 shrink-0" data-testid="provider-icon" />
        )}
        <span>{provider.label}</span>
        {!provider.available && (
          <span className="ml-auto rounded-full bg-crm-secondary px-2 py-0.5 text-[11px] font-medium text-crm-text-secondary">
            Em breve
          </span>
        )}
      </button>
      {error && (
        <p role="alert" className="px-1 text-xs text-crm-danger">
          {error}
        </p>
      )}
    </div>
  );
}
