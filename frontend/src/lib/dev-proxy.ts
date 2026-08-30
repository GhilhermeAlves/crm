/**
 * Proxy de desenvolvimento (dev-only) para o Access Gateway + APIs que vivem na
 * VPS. Em produção o nginx faz esse roteamento; em dev local o Next.js não tem
 * rota para `/auth/*` e `/api/*`, então o middleware repassa essas chamadas para
 * o serviço remoto quando `DEV_GATEWAY_TARGET` está configurado.
 *
 * <p>Segurança / delimitação de escopo:
 * <ul>
 *   <li>ATIVO SOMENTE quando a env `DEV_GATEWAY_TARGET` existe (sem valor padrão
 *       no código). O build de produção (CI) não a define → comportamento nulo;</li>
 *   <li>env SEM prefixo {@code NEXT_PUBLIC_} (server-side; nunca vai ao browser);</li>
 *   <li>repassa {@code X-Forwarded-Host}/{X-Forwarded-Proto} para que o
 *       auth-service derive o {@code redirect_uri} como
 *       {@code http://localhost:3000/...} (origem do browser), preservando o
 *       issuer do Keycloak no domínio de produção;</li>
 *   <li>cookies (inclusive {@code Set-Cookie} do gateway) são repassados
 *       intactos, mantendo a sessão host-only em {@code localhost:3000}.</li>
 * </ul>
 */

/** Upstream remoto em dev local (ex.: {@code https://srv1348261.hstgr.cloud}). Vazio = proxy desligado. */
export const DEV_GATEWAY_TARGET = process.env.DEV_GATEWAY_TARGET ?? "";

/** Headers hop-by-hop que NÃO devem ser repassados entre proxies. */
export const HOP_BY_HOP = new Set([
  "connection",
  "keep-alive",
  "proxy-authenticate",
  "proxy-authorization",
  "te",
  "trailer",
  "transfer-encoding",
  "upgrade",
]);

/** Caminhos que só existem na infra (nginx no prod) e precisam de proxy em dev. */
export function isDevProxyPath(pathname: string): boolean {
  return pathname.startsWith("/auth/") || pathname.startsWith("/api/") || pathname === "/auth" || pathname === "/api";
}

/**
 * Monta a URL upstream preservando path e query string do request original.
 * Ex.: {@code /auth/authorize?redirect=/dashboard} →
 * {@code https://srv.../auth/authorize?redirect=/dashboard}.
 */
export function buildUpstreamUrl(target: string, pathname: string, search: string): string {
  const base = target.replace(/\/+$/, "");
  return `${base}${pathname}${search}`;
}

/**
 * Clona os headers do request removendo os hop-by-hop e adicionando os headers
 * de encaminhamento para o gateway derivar a origem pública do browser.
 */
export function forwardedHeaders(headers: Headers, browserHost: string): Headers {
  const out = new Headers();
  headers.forEach((value, key) => {
    if (HOP_BY_HOP.has(key.toLowerCase())) return;
    out.set(key, value);
  });
  out.set("x-forwarded-host", browserHost);
  out.set("x-forwarded-proto", "http");
  return out;
}

/**
 * Copia os {@code Set-Cookie} do upstream para a resposta do middleware. O
 * runtimes atuais expõem {@code getSetCookie()} (cada cookie em sua própria
 * linha); quando não disponível, usa o valor único combinado.
 */
export function copySetCookieHeaders(source: Headers, target: Headers): void {
  target.delete("set-cookie");
  const getSetCookie = (source as unknown as { getSetCookie?: () => string[] }).getSetCookie;
  if (typeof getSetCookie === "function") {
    for (const cookie of getSetCookie.call(source) as string[]) {
      target.append("set-cookie", cookie);
    }
  } else {
    const joined = source.get("set-cookie");
    if (joined) target.set("set-cookie", joined);
  }
}