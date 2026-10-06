import { invitationPath } from "@/lib/constants";

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
  // Sprint 7.2: vínculo de conta local após login Google sem keycloak_sub.
  "/link-account",
  // Convite: prévia e cadastro sem sessão; quem já tem conta entra a partir dela.
  "/convite",
] as const;

/** Link legado de convite, redirecionado para /convite/{token}. */
export const LEGACY_INVITATION_PATH = "/invitations/accept";

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
  /** Query string original (ex.: "?token=abc"), preservada no redirect p/ login. */
  search?: string;
}): AuthDecision {
  const { pathname, hasSession, search = "" } = input;

  // Links antigos dos e-mails: /invitations/accept?token=X -> /convite/X,
  // com ou sem sessão (a página nova decide o resto).
  if (pathname === LEGACY_INVITATION_PATH) {
    const token = new URLSearchParams(search).get("token");
    if (token) {
      return { redirectTo: invitationPath(token) };
    }
  }

  const isPublicPath = isPublicPathname(pathname);

  if (!hasSession && !isPublicPath) {
    return { redirectTo: `/login?redirect=${encodeURIComponent(pathname + search)}` };
  }

  return {};
}
