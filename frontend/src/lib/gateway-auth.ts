/**
 * Acesso ao Access Gateway OIDC (Sprints 6.1–6.4).
 *
 * <p>O browser não detém tokens: a sessão vive no servidor (auth-service) e é
 * referenciada pelo cookie HttpOnly `crm_session`. Este módulo apenas:
 * <ul>
 *   <li>inicia o login ({@link loginWithGateway}) navegando para
 *       `/auth/authorize` (o gateway redireciona ao Keycloak e, após o CRM
 *       Access, volta com o cookie de sessão);</li>
 *   <li>encerra a sessão ({@link logoutWithGateway}) via `/auth/logout`;</li>
 *   <li>renova a sessão ({@link refreshGatewaySession}) via `POST /auth/refresh`
 *       com proteção CSRF cookie-to-header (XSRF-TOKEN → X-XSRF-TOKEN).</li>
 * </ul>
 */
export const SESSION_COOKIE = "crm_session";
export const CSRF_COOKIE = "XSRF-TOKEN";
export const CSRF_HEADER = "X-XSRF-TOKEN";
export const PENDING_LINK_COOKIE = "crm_pending_link";

/**
 * Provedores de identidade suportados (Sprint 7.0). Meta/Facebook está fora de
 * escopo. O alias casa com o identity provider (Identity Brokering) do Keycloak
 * e é encaminhado ao gateway como `kc_idp_hint`.
 */
export const IDENTITY_PROVIDERS = {
  GOOGLE: "google",
  MICROSOFT: "microsoft",
  APPLE: "apple",
  PHONE: "phone",
} as const;

export type IdentityProviderId = (typeof IDENTITY_PROVIDERS)[keyof typeof IDENTITY_PROVIDERS];

/**
 * Inicia o login via Access Gateway (`/auth/authorize`). O parâmetro
 * {@code provider} é opcional: quando informado, o gateway adiciona
 * {@code kc_idp_hint} na autorização do Keycloak para encaminhar o usuário ao
 * Identity Provider escolhido. Nenhum token de provedor externo transita pelo
 * browser — a sessão continua server-side (cookie HttpOnly).
 */
export function loginWithGateway(
  redirectPath?: string,
  provider?: IdentityProviderId,
): void {
  const params = new URLSearchParams({ redirect: redirectPath || "/dashboard" });
  if (provider) params.set("provider", provider);
  window.location.assign(`/auth/authorize?${params.toString()}`);
}

export function logoutWithGateway(): void {
  window.location.assign("/auth/logout");
}

/** Lê o token CSRF do cookie não-HttpOnly (padrão cookie-to-header). */
export function getCsrfToken(): string | null {
  if (typeof document === "undefined") return null;
  const match = document.cookie.match(
    new RegExp(`(?:^|;\\s*)${CSRF_COOKIE}=([^;]*)`),
  );
  if (!match) return null;
  try {
    return decodeURIComponent(match[1]);
  } catch {
    return match[1];
  }
}

let refreshInFlight: Promise<boolean> | null = null;

/**
 * Renova a sessão no servidor (rotação de tokens lá) e devolve true se OK.
 * Refreshes concorrentes são deduplicados (uma única chamada em voo).
 */
export async function refreshGatewaySession(): Promise<boolean> {
  if (typeof window === "undefined") return false;
  if (refreshInFlight) return refreshInFlight;

  refreshInFlight = (async () => {
    try {
      const response = await fetch("/auth/refresh", {
        method: "POST",
        credentials: "include",
        headers: { [CSRF_HEADER]: getCsrfToken() || "" },
      });
      return response.ok;
    } catch {
      return false;
    } finally {
      refreshInFlight = null;
    }
  })();

  return refreshInFlight;
}

/**
 * Sprint 7.2 (Caso B) — vínculo de conta local.
 *
 * <p>Após o login Google de um e-mail que já possui conta local (sem
 * `keycloak_sub`), o gateway redireciona para `/link-account` com o cookie
 * HttpOnly efêmero `crm_pending_link`. A página consulta {@link getLinkStatus}
 * (exibe o e-mail da conta local) e, ao digitar a senha, chama
 * {@link linkAccountWithPassword}. Sucesso → o gateway cria a sessão real
 * (`crm_session`) e devolve o redirect original.
 */

export type GatewayLinkStatus = { pending: boolean; email?: string };

export class GatewayLinkError extends Error {
  readonly code: string;

  constructor(code: string, message: string) {
    super(message);
    this.name = "GatewayLinkError";
    this.code = code;
  }
}

/** Estado do vínculo pendente via cookie `crm_pending_link`. */
export async function getLinkStatus(): Promise<GatewayLinkStatus> {
  if (typeof window === "undefined") return { pending: false };
  try {
    const response = await fetch("/auth/link-status", {
      credentials: "include",
    });
    if (!response.ok) return { pending: false };
    const body = await response.json();
    return {
      pending: !!body.pending,
      email: typeof body.email === "string" ? body.email : undefined,
    };
  } catch {
    return { pending: false };
  }
}

/**
 * Conclui o vínculo pendente verificando a senha da conta local no servidor.
 * Devolve o redirect original em caso de sucesso (a sessão `crm_session` é
 * setada pelo gateway via Set-Cookie). Erros: `GatewayLinkError` com `code`
 * (`INVALID_CREDENTIALS`, `LINK_PENDING_NOT_FOUND`, `LINK_NOT_FOUND`,
 * `RATE_LIMIT_EXCEEDED`, ...).
 */
export async function linkAccountWithPassword(
  password: string,
): Promise<{ redirect: string }> {
  const response = await fetch("/auth/link", {
    method: "POST",
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      [CSRF_HEADER]: getCsrfToken() || "",
    },
    body: JSON.stringify({ password }),
  });

  if (response.ok) {
    const body = await response.json();
    return { redirect: body.redirect || "/dashboard" };
  }

  let code = "LINK_FAILED";
  let message = "Falha ao vincular a conta.";
  try {
    const body = await response.json();
    if (typeof body.code === "string" && body.code) code = body.code;
    if (typeof body.message === "string" && body.message) message = body.message;
  } catch {
    // corpo não-JSON
  }
  throw new GatewayLinkError(code, message);
}
