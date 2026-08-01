const KC_TOKEN_KEY = "kc_accessToken";
const KC_REFRESH_TOKEN_KEY = "kc_refreshToken";

/**
 * Flag de sessão no cookie (mesma origem). Nunca carrega o JWT: indica apenas
 * "existe uma sessão potencialmente autenticada" para o middleware do Next.js
 * decidir roteamento SSR. A autoridade da autenticação é o Keycloak/backend.
 */
const SESSION_COOKIE = "kc_authenticated";
const SESSION_COOKIE_MAX_AGE = 7 * 24 * 60 * 60;

function setSessionCookie() {
  if (typeof document === "undefined") return;
  document.cookie = `${SESSION_COOKIE}=1; max-age=${SESSION_COOKIE_MAX_AGE}; path=/; SameSite=Lax`;
}

function deleteSessionCookie() {
  if (typeof document === "undefined") return;
  document.cookie = `${SESSION_COOKIE}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/`;
}

/**
 * Estado de token da sessão. Responsabilidades limitadas a: atualizar tokens,
 * remover tokens e fornecer acesso ao estado. Nenhuma decisão de autenticação
 * ou regra de autorização vive aqui.
 */
export const TokenManager = {
  /**
   * Único ponto de escrita do estado de token: grava access/refresh token no
   * localStorage e a flag de sessão no cookie. Todo componente que precisar
   * persistir tokens deve chamar este método.
   */
  setTokens(accessToken: string, refreshToken: string | null): void {
    if (typeof window === "undefined") return;
    if (!accessToken) return;
    localStorage.setItem(KC_TOKEN_KEY, accessToken);
    if (refreshToken) {
      localStorage.setItem(KC_REFRESH_TOKEN_KEY, refreshToken);
    }
    setSessionCookie();
  },

  clearTokens(): void {
    if (typeof window === "undefined") return;
    localStorage.removeItem(KC_TOKEN_KEY);
    localStorage.removeItem(KC_REFRESH_TOKEN_KEY);
    deleteSessionCookie();
  },

  getAccessToken(): string | null {
    if (typeof window === "undefined") return null;
    return localStorage.getItem(KC_TOKEN_KEY);
  },

  getRefreshToken(): string | null {
    if (typeof window === "undefined") return null;
    return localStorage.getItem(KC_REFRESH_TOKEN_KEY);
  },
};
