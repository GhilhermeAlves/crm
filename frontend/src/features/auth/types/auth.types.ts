export type User = {
  id: string;
  email: string;
  name: string;
  /** null = usuário provisionado sem empresa (onboarding pendente, Sprint 8.3) */
  companyId: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
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
