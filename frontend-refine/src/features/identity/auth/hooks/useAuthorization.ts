"use client";

import { useCallback } from "react";
import { useAuth } from "./useAuth";

/**
 * Sprint 9 — autorização de UX orientada a permissões da EMPRESA ATIVA.
 *
 * Expõe `can(permission)`/`cannot(permission)` a partir das permissões efetivas
 * do CurrentUser (carregadas via /auth/me e re-derivadas a cada Company
 * Switcher). CRM_ADMIN/SUPER_ADMIN passam sempre; sem a permissão efetiva a UX
 * fica oculta até o /auth/me carregar (fail-closed). A autorização efetiva é
 * SEMPRE no backend (@PreAuthorize + RLS).
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
      if (roles.includes("CRM_ADMIN") || roles.includes("SUPER_ADMIN")) return true;
      const permission = action
        ? toPermissionName(permissionOrResource, action)
        : permissionOrResource;
      return (permissions ?? []).includes(permission);
    },
    [permissions, roles],
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
