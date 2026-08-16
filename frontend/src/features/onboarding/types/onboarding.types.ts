import type {
  Tenant,
  TenantAddress,
} from "@/features/tenants/types/tenant.types";

/**
 * Onboarding self-service (Sprint 8.3): usuário provisionado sem empresa cria
 * a primeira empresa e vira OWNER. O backend aplica os defaults de plano
 * (STARTER), limites e status (ACTIVE) — aqui só os dados do formulário.
 */
export type OnboardingCompanyRequest = {
  legalName: string;
  tradingName: string;
  cnpj: string;
  stateRegistration?: string;
  municipalRegistration?: string;
  email: string;
  phone: string;
  website?: string;
  address: TenantAddress;
};

export type { Tenant as OnboardingCompanyResponse };
