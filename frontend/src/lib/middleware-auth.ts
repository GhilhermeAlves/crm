/**
 * Decisão de roteamento de autenticação para o middleware do Next.js.
 * O middleware NÃO interpreta/decodifica JWT: apenas verifica a existência da
 * flag de sessão no cookie (`kc_authenticated`), que indica uma sessão
 * potencialmente autenticada. A autoridade real é o Keycloak (SSO) e o backend.
 * Rotas protegidas sem a flag redirecionam para o login preservando o destino.
 */
export const SESSION_COOKIE = "kc_authenticated";

export const PUBLIC_PATHS = [
  "/login",
  "/register",
  "/forgot-password",
  "/reset-password",
  "/auth/callback",
] as const;

export type AuthDecision = {
  /** Path para redirecionar, se houver. */
  redirectTo?: string;
};

export function resolveAuthRedirect(input: {
  pathname: string;
  hasSession: boolean;
}): AuthDecision {
  const { pathname, hasSession } = input;

  if (pathname === "/") {
    return {};
  }

  const isPublicPath = PUBLIC_PATHS.some((path) => pathname.startsWith(path));

  if (!hasSession && !isPublicPath) {
    return { redirectTo: `/login?redirect=${encodeURIComponent(pathname)}` };
  }

  return {};
}
