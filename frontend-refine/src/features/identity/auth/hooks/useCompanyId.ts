"use client";

import { useAuth } from "./useAuth";

/** Empresa ativa do usuário logado (`null` enquanto o CurrentUser carrega). */
export function useCompanyId(): string | null {
  const { user } = useAuth();
  return user?.companyId ?? null;
}
