"use client";

import { useCallback } from "react";
import { useAuth } from "./useAuth";

/**
 * Sprint 9 — autorização de UX orientada a permissões da EMPRESA ATIVA.
 *
 * Expõe `can(permission)`/`cannot(permission)` a partir das permissões efetivas
 * do CurrentUser (carregadas via /auth/me e re-derivadas a cada Company
 * Switcher). Super-admin e usuário ainda sem permissões carregadas são tratados
 * de forma conservadora:
 *
 * - sem permissões carregadas (ex.: carregando / onboarding): autoriza (évita
 *   "blink" de menus antes de o CurrentUser chegar) — o backend é a autoridade;
 * - nos demais casos, somente a permission em questão libera.
 *
 * Isto é APENAS proteção de interface. A autorização efetiva é SEMPRE no backend
 * (@PreAuthorize + RLS).
 */
export function useAuthorization() {
  const { permissions, roles } = useAuth();

  const can = useCallback(
    (permission?: string) => {
      if (!permission) return true;
      // Fallback conservador: backend continua sendo a autoridade final.
      if (!permissions || permissions.length === 0) return true;
      return permissions.includes(permission);
    },
    [permissions],
  );

  const cannot = useCallback(
    (permission?: string) => !can(permission),
    [can],
  );

  const hasRole = useCallback(
    (role?: string) => !role || roles.includes(role),
    [roles],
  );

  const isSuperAdmin = roles.includes("SUPER_ADMIN");

  return { can, cannot, hasRole, isSuperAdmin, permissions, roles };
}

export function usePermission() {
  return useAuthorization().can;
}