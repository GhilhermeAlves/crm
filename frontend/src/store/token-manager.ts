const ACCESS_TOKEN_KEY = "accessToken";
const REFRESH_TOKEN_KEY = "refreshToken";
const AUTH_COOKIE_KEY = "accessToken";
const KC_TOKEN_KEY = "kc_accessToken";
const KC_REFRESH_TOKEN_KEY = "kc_refreshToken";

function setCookie(name: string, value: string, days: number) {
  if (typeof document === "undefined") return;
  const expires = new Date(Date.now() + days * 864e5).toUTCString();
  document.cookie = `${name}=${encodeURIComponent(value)}; expires=${expires}; path=/; SameSite=Lax`;
}

function deleteCookie(name: string) {
  if (typeof document === "undefined") return;
  document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/`;
}

function parseJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const base64Url = token.split(".")[1];
    if (!base64Url) return null;
    const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split("")
        .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
        .join("")
    );
    return JSON.parse(jsonPayload);
  } catch {
    return null;
  }
}

export const TokenManager = {
  getAccessToken(): string | null {
    if (typeof window === "undefined") return null;
    return localStorage.getItem(KC_TOKEN_KEY) || localStorage.getItem(ACCESS_TOKEN_KEY);
  },

  getRefreshToken(): string | null {
    if (typeof window === "undefined") return null;
    return localStorage.getItem(KC_REFRESH_TOKEN_KEY) || localStorage.getItem(REFRESH_TOKEN_KEY);
  },

  setTokens(accessToken: string, refreshToken: string): void {
    if (typeof window === "undefined") return;
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
    setCookie(AUTH_COOKIE_KEY, accessToken, 7);
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
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
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

  getRoles(): string[] {
    const token = this.getAccessToken();
    if (!token) return [];
    const payload = parseJwtPayload(token);
    if (!payload) return [];
    const realmRoles = payload["realm_access"] as { roles?: string[] } | undefined;
    if (realmRoles?.roles) return realmRoles.roles;
    const roles = payload["roles"];
    return Array.isArray(roles) ? (roles as string[]) : [];
  },

  getPermissions(): string[] {
    const token = this.getAccessToken();
    if (!token) return [];
    const payload = parseJwtPayload(token);
    if (!payload) return [];
    const permissions = payload["permissions"];
    return Array.isArray(permissions) ? (permissions as string[]) : [];
  },

  getCompanyId(): string | null {
    const token = this.getAccessToken();
    if (!token) return null;
    const payload = parseJwtPayload(token);
    if (!payload) return null;
    const companyId = payload["company_id"];
    return typeof companyId === "string" ? companyId : null;
  },
};
