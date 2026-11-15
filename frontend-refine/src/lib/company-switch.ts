/**
 * Para onde ir depois de trocar a empresa ativa.
 *
 * Páginas de detalhe (`/patients/<uuid>/...`, `/leads/<uuid>` etc.) mostram um
 * registro da empresa anterior, que a nova empresa não enxerga (o backend
 * responde 404). Nesses casos volta para a lista do mesmo recurso; nas demais
 * páginas, fica onde está (retorna `null`).
 */
const DETAIL_PATH =
  /^(\/[\w-]+(?:\/[\w-]+)*?)\/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}(?:\/|$)/i;

export function pathAfterCompanySwitch(pathname: string | null | undefined): string | null {
  if (!pathname) return null;
  const match = DETAIL_PATH.exec(pathname);
  return match ? match[1] : null;
}
