import api from "@/lib/api";
import type {
  OnboardingCompanyRequest,
  OnboardingCompanyResponse,
} from "../types/onboarding.types";

const BASE_PATH = "/onboarding/companies";

/**
 * Onboarding self-service (Sprint 8.3). Endpoint autenticado: qualquer usuário
 * com sessão (sem exigir company:create) cria a primeira empresa e vira OWNER.
 */
export const OnboardingService = {
  async createCompany(
    data: OnboardingCompanyRequest,
  ): Promise<OnboardingCompanyResponse> {
    const response = await api.post<OnboardingCompanyResponse>(BASE_PATH, {
      legalName: data.legalName,
      tradingName: data.tradingName,
      cnpj: data.cnpj,
      stateRegistration: data.stateRegistration ?? "",
      municipalRegistration: data.municipalRegistration ?? "",
      email: data.email,
      phone: data.phone,
      website: data.website ?? "",
      addressZipCode: data.address.zipCode,
      addressStreet: data.address.street,
      addressNumber: data.address.number,
      addressComplement: data.address.complement ?? "",
      addressNeighborhood: data.address.neighborhood,
      addressCity: data.address.city,
      addressState: data.address.state,
      addressCountry: data.address.country ?? "Brasil",
      plan: "STARTER",
    });
    return response.data;
  },
};
