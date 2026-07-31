import { decodeJwtPayload } from "@/lib/jwt";

const KC_TOKEN_KEY = "kc_accessToken";
const KC_REFRESH_TOKEN_KEY = "kc_refreshToken";
const AUTH_COOKIE_KEY = "accessToken";

function setCookie(name: string, value: string, days: number) {
  if (typeof document === "undefined") return;
  const expires = new Date(Date.now() + days * 864e5).toUTCString();
  document.cookie = `${name}=${encodeURIComponent(value)}; expires=${expires}; path=/; SameSite=Lax`;
}

function deleteCookie(name: string) {
  if (typeof document === "undefined") return;
  document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/`;
}

/**
 * Armazenamento do token emitido pelo Keycloak (único emissor). O token e o
 * refresh token são guardados em localStorage e o access token também é
 * espelhado num cookie (mesma origem) para o middleware do Next.js decidir o
 * roteamento protegido no servidor.
 */
export const TokenManager = {
  getAccessToken(): string | null {
    if (typeof window === "undefined") return null;
    return localStorage.getItem(KC_TOKEN_KEY);
  },

  getRefreshToken(): string | null {
    if (typeof window === "undefined") return null;
    return localStorage.getItem(KC_REFRESH_TOKEN_KEY);
  },

  setKeycloakToken(token: string): void {
    if (typeof window === "undefined") return;
    localStorage.setItem(KC_TOKEN_KEY, token);
    setCookie(AUTH_COOKIE_KEY, token, 7);
  },

  setKeycloakRefreshToken(token: string): void {
    if (typeof window === "undefined") return;
    localStorage.setItem(KC_REFRESH_TOKEN_KEY, token);
  },

  clearTokens(): void {
    if (typeof window === "undefined") return;
    localStorage.removeItem(KC_TOKEN_KEY);
    localStorage.removeItem(KC_REFRESH_TOKEN_KEY);
    deleteCookie(AUTH_COOKIE_KEY);
  },

  hasTokens(): boolean {
    return !!this.getAccessToken();
  },

  isKeycloakAuth(): boolean {
    if (typeof window === "undefined") return false;
    return !!localStorage.getItem(KC_TOKEN_KEY);
  },

  /**
   * Roles do realm do Keycloak (claims OIDC de identidade). Usadas apenas
   * para UX (menus/badges). Autorização de negócio é resolvida no backend e,
   * futuramente, via CurrentUser (endpoint público do auth-service).
   */
  getRoles(): string[] {
    const token = this.getAccessToken();
    if (!token) return [];
    const payload = decodeJwtPayload(token);
    if (!payload) return [];
    const realmRoles = payload["realm_access"] as { roles?: string[] } | undefined;
    if (realmRoles?.roles) return realmRoles.roles;
    const roles = payload["roles"];
    return Array.isArray(roles) ? (roles as string[]) : [];
  },
};
