export function decodeJwtPayload<T extends Record<string, unknown> = Record<string, unknown>>(
  token: string | null | undefined,
): T | null {
  if (!token) return null;
  try {
    const base64Url = token.split(".")[1];
    if (!base64Url) return null;
    const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split("")
        .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
        .join(""),
    );
    return JSON.parse(jsonPayload) as T;
  } catch {
    return null;
  }
}

export function getJwtExpiration(token: string | null | undefined): number | null {
  const payload = decodeJwtPayload(token);
  const exp = payload?.exp;
  return typeof exp === "number" ? exp : null;
}

export function isJwtExpired(token: string | null | undefined, nowSeconds = Math.floor(Date.now() / 1000)): boolean {
  const exp = getJwtExpiration(token);
  if (exp === null) return true;
  return exp <= nowSeconds;
}
