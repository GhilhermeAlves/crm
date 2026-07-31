import { isJwtExpired } from "@/lib/jwt";

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
  /** True quando o cookie de token existe mas está expirado/inválido. */
  clearCookie: boolean;
};

/**
 * Decisão de roteamento de autenticação para o middleware do Next.js.
 * O cookie `accessToken` espelha o JWT do Keycloak para proteção SSR; a
 * validade é checada apenas pelo `exp` (decodificação stateless, sem JWKS) —
 * a assinatura continua sendo verificada pelo backend. Isso evita o redirect
 * loop entre `/login` e `/dashboard` quando o token já expirou.
 */
export function resolveAuthRedirect(input: {
  pathname: string;
  accessToken: string | null;
  nowSeconds?: number;
}): AuthDecision {
  const { pathname, accessToken } = input;
  const nowSeconds = input.nowSeconds ?? Math.floor(Date.now() / 1000);

  const isPublicPath = PUBLIC_PATHS.some((path) => pathname.startsWith(path));

  if (pathname === "/") {
    return {
      clearCookie: !!accessToken && isJwtExpired(accessToken, nowSeconds),
    };
  }

  const tokenValid = !!accessToken && !isJwtExpired(accessToken, nowSeconds);

  if (!tokenValid) {
    if (isPublicPath) {
      return { clearCookie: !!accessToken };
    }
    return {
      redirectTo: `/login?redirect=${encodeURIComponent(pathname)}`,
      clearCookie: !!accessToken,
    };
  }

  if (isPublicPath) {
    return { redirectTo: "/dashboard", clearCookie: false };
  }

  return { clearCookie: false };
}
