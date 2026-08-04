/**
 * Decisão de roteamento de autenticação para o middleware do Next.js.
 * O middleware NÃO interpreta/decodifica JWT: apenas verifica a existência do
 * cookie de sessão (`crm_session`, HttpOnly, setado pelo gateway no callback).
 * A autoridade real da autenticação é o Access Gateway (auth-service) + backend.
 * Rotas protegidas sem a flag redirecionam para o login preservando o destino.
 */
export const SESSION_COOKIE = "crm_session";

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

/** True quando o pathname é público (não exige sessão). */
export function isPublicPathname(pathname: string): boolean {
  if (pathname === "/") return true;
  return PUBLIC_PATHS.some((path) => pathname.startsWith(path));
}

export function resolveAuthRedirect(input: {
  pathname: string;
  hasSession: boolean;
}): AuthDecision {
  const { pathname, hasSession } = input;

  const isPublicPath = isPublicPathname(pathname);

  if (!hasSession && !isPublicPath) {
    return { redirectTo: `/login?redirect=${encodeURIComponent(pathname)}` };
  }

  return {};
}
