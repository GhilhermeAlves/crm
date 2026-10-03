import type { IdentityProviderId } from "@/lib/gateway-auth";

/** Provedor de identidade devolvido pelo catálogo do gateway (GET /auth/providers). */
export type IdentityProviderInfo = {
  alias: IdentityProviderId;
  label: string;
  available: boolean;
};
