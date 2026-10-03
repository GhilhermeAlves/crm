export type User = {
  id: string;
  email: string;
  name: string;
  /** null = usuário provisionado sem empresa (onboarding pendente, Sprint 8.3) */
  companyId: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  /** Roles da EMPRESA ATIVA (Sprint 9) — já scoped por companyId no backend. */
  roles?: string[];
  /**
   * Papel efetivo (membership) da empresa ativa. Ex.: Empresa A → "ADMIN",
   * Empresa B → "VIEWER". Re-derivado a cada Company Switcher.
   */
  membershipRole?: string | null;
  /**
   * Permissões efetivas da empresa ativa (resource:action). Carregadas do
   * CurrentUser via /auth/me. Autorização de UX; backend é a autoridade.
   */
  permissions?: string[];
};

/**
 * Empresa disponível para o Company Switcher (Sprint 8.4). Somente empresas do
 * usuário com membership ativa (validadas no backend).
 */
export type CompanyOption = {
  companyId: string;
  name: string;
  logo: string | null;
  /** empresa ativa corrente — resolvida pelo backend (não confia em input). */
  active: boolean;
};

export type RegisterRequest = {
  name: string;
  email: string;
  password: string;
  companyId?: string;
};

export type ForgotPasswordRequest = {
  email: string;
};

export type ResetPasswordRequest = {
  token: string;
  password: string;
};

export type ApiErrorResponse = {
  status: number;
  error: string;
  message: string;
  timestamp: string;
};
