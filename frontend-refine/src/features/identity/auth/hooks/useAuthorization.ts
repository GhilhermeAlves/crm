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
/**
 * Nome canônico da permissão a partir de (recurso, ação) — equivalente ao
 * `toPermissionName` do access-control do Portal Praia. Ex.: ("patient", "read")
 * → "patient:read".
 */
export function toPermissionName(resource: string, action: string): string {
  return `${resource}:${action}`;
}

export function useAuthorization() {
  const { permissions, roles } = useAuth();

  const can = useCallback(
    (permissionOrResource?: string, action?: string) => {
      if (!permissionOrResource) return true;
      const permission = action
        ? toPermissionName(permissionOrResource, action)
        : permissionOrResource;
      // Fallback conservador: backend continua sendo a autoridade final.
      if (!permissions || permissions.length === 0) return true;
      return permissions.includes(permission);
    },
    [permissions],
  );

  const cannot = useCallback(
    (permissionOrResource?: string, action?: string) => !can(permissionOrResource, action),
    [can],
  );

  const hasRole = useCallback((role?: string) => !role || roles.includes(role), [roles]);

  const isCompanyAdmin = roles.includes("CRM_ADMIN");
  const isPlatformAdmin = roles.includes("SUPER_ADMIN");

  return { can, cannot, hasRole, isCompanyAdmin, isPlatformAdmin, permissions, roles };
}

export function usePermission() {
  return useAuthorization().can;
}
