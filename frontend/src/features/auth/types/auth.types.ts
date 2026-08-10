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
