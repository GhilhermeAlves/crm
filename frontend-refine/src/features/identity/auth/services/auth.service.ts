import api from "@/lib/api";
import type {
  User,
  CompanyOption,
  RegisterRequest,
  ForgotPasswordRequest,
  ResetPasswordRequest,
} from "../types/auth.types";

/**
 * Serviços de autenticação. O login é exclusivo via Keycloak (OIDC + PKCE);
 * os métodos restantes são fluxos legados de gerenciamento de conta mantidos
 * no crm-backend (Sprint 1) até a decisão de migração (ver MIGRATION_PLAN.md).
 */
export const AuthService = {
  async register(data: RegisterRequest): Promise<void> {
    await api.post("/auth/register", {
      name: data.name,
      email: data.email,
      password: data.password,
      ...(data.companyId ? { companyId: data.companyId } : {}),
    });
  },

  async forgotPassword(_data: ForgotPasswordRequest): Promise<void> {
    await api.post("/auth/forgot-password", _data);
  },

  async resetPassword(data: ResetPasswordRequest): Promise<void> {
    await api.post("/auth/reset-password", {
      token: data.token,
      newPassword: data.password,
    });
  },

  /** Identidade de negócio vinda do crm-backend (Sprint 1). */
  async me(): Promise<User> {
    const response = await api.get<User>("/auth/me");
    return response.data;
  },

  /**
   * Empresas do usuário com membership ativa (Company Switcher, Sprint 8.4).
   * A autorização é feita no backend; o campo `active` identifica a ativa.
   */
  async myCompanies(): Promise<CompanyOption[]> {
    const response = await api.get<CompanyOption[]>("/me/companies");
    return response.data;
  },

  /** Alterna a empresa ativa do usuário (Sprint 8.4). */
  async switchCompany(companyId: string): Promise<CompanyOption> {
    const response = await api.post<CompanyOption>("/me/switch-company", {
      companyId,
    });
    return response.data;
  },
};
