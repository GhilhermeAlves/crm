"use client";

import { useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import {
  IDENTITY_PROVIDERS,
  loginWithGateway,
  type IdentityProviderId,
} from "@/lib/gateway-auth";
import { useIdentityProviders } from "../hooks/useIdentityProviders";
import { IdentityProviderButton } from "./IdentityProviderButton";
import { PhoneLoginForm } from "./PhoneLoginForm";
import type { IdentityProviderInfo } from "../types/identity-provider";

/**
 * Lista de provedores de identidade da tela de login (Sprint 7.0).
 *
 * <p>O catálogo exibido é fixo (Google, Telefone) e a
 * disponibilidade de cada um vem do servidor ({@code GET /auth/providers}):
 * <ul>
 *   <li>enquanto o catálogo não chega (ou falha), todos aparecem desabilitados
 *       (seguro — nenhuma escolha incorreta possível);</li>
 *   <li>o rótulo exibido é o do servidor quando disponível (fallback local
 *       apenas durante o carregamento);</li>
 *   <li>Meta/Facebook não existe nesta tela.</li>
 * </ul>
 * O clique em um provedor disponível navega para `/auth/authorize` com o
 * parâmetro `provider`, preservando o `redirect` original — EXCETO o Telefone
 * (Sprint 7.4): telefone NÃO é um IdP do Keycloak; o clique abre o fluxo local
 * de OTP ({@link PhoneLoginForm}) e, após confirmar a posse, segue para o
 * fluxo de senha do Keycloak.
 */
const KNOWN_ORDER: IdentityProviderId[] = [
  IDENTITY_PROVIDERS.GOOGLE,
  IDENTITY_PROVIDERS.PHONE,
];

const FALLBACK_LABELS: Record<IdentityProviderId, string> = {
  google: "Google",
  microsoft: "Microsoft",
  apple: "Apple",
  phone: "Telefone",
};

export function ProviderList() {
  const { data, isLoading, isError } = useIdentityProviders();
  const searchParams = useSearchParams();
  const redirect = searchParams.get("redirect") ?? undefined;
  const [phoneMode, setPhoneMode] = useState(false);

  const byAlias = useMemo(() => {
    const map = new Map<string, IdentityProviderInfo>();
    for (const provider of data ?? []) map.set(provider.alias, provider);
    return map;
  }, [data]);

  const providers: IdentityProviderInfo[] = KNOWN_ORDER.map((alias) => {
    const server = byAlias.get(alias);
    return {
      alias,
      label: server?.label ?? FALLBACK_LABELS[alias],
      available: server ? server.available : false,
    };
  });

  const phone = providers.find(
    (provider) => provider.alias === IDENTITY_PROVIDERS.PHONE,
  );

  function handleSelect(provider: IdentityProviderInfo) {
    if (!provider.available) return;
    if (provider.alias === IDENTITY_PROVIDERS.PHONE) {
      setPhoneMode(true);
      return;
    }
    loginWithGateway(redirect, provider.alias);
  }

  if (phoneMode && phone?.available) {
    return (
      <PhoneLoginForm redirect={redirect} onBack={() => setPhoneMode(false)} />
    );
  }

  return (
    <div className="space-y-2">
      {providers.map((provider) => (
        <IdentityProviderButton
          key={provider.alias}
          provider={provider}
          onSelect={handleSelect}
        />
      ))}
      {isLoading && (
        <p
          role="status"
          data-testid="providers-loading"
          className="px-1 pt-1 text-center text-xs text-crm-text-secondary"
        >
          Carregando provedores de identidade…
        </p>
      )}
      {isError && (
        <p
          role="alert"
          data-testid="providers-error"
          className="px-1 pt-1 text-center text-xs text-crm-text-secondary"
        >
          Não foi possível carregar os provedores de identidade no momento.
        </p>
      )}
    </div>
  );
}
