export function decodeJwtPayload<T extends Record<string, unknown> = Record<string, unknown>>(
  token: string | null | undefined,
): T | null {
  if (!token) return null;
  try {
    const base64Url = token.split(".")[1];
    if (!base64Url) return null;
    const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split("")
        .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
        .join(""),
    );
    return JSON.parse(jsonPayload) as T;
  } catch {
    return null;
  }
}

/**
 * Roles do realm do Keycloak (claims OIDC de identidade). Uso exclusivo de UX
 * (menus/badges de exibição) — nunca é autorização de negócio, que é resolvida
 * no backend/CurrentUser.
 */
export function getRealmRoles(token: string | null | undefined): string[] {
  const payload = decodeJwtPayload(token);
  if (!payload) return [];
  const realmRoles = payload["realm_access"] as { roles?: string[] } | undefined;
  if (realmRoles?.roles) return realmRoles.roles;
  const roles = payload["roles"];
  return Array.isArray(roles) ? (roles as string[]) : [];
}
