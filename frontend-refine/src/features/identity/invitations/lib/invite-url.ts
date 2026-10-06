/**
 * Torna clicável/copiável o link devolvido pelo backend.
 *
 * O backend devolve `/convite/{token}` (relativo) quando
 * `app.invitations.base-url` não está configurado. Resolver contra a origem da
 * página NÃO é montar token: o caminho vem inteiro do backend.
 */
export function resolveInviteUrl(url: string, origin?: string): string {
  if (!url) return url;
  if (/^https?:\/\//i.test(url)) return url;
  if (!url.startsWith("/")) return url;
  const base = origin ?? (typeof window !== "undefined" ? window.location.origin : "");
  return base ? `${base.replace(/\/+$/, "")}${url}` : url;
}
