import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { resolveAuthRedirect, SESSION_COOKIE } from "@/lib/middleware-auth";
import {
  DEV_GATEWAY_TARGET,
  HOP_BY_HOP,
  buildUpstreamUrl,
  copySetCookieHeaders,
  forwardedHeaders,
  isDevProxyPath,
} from "@/lib/dev-proxy";

export async function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const target = DEV_GATEWAY_TARGET;

  if (target && isDevProxyPath(pathname)) {
    return await proxyToGateway(request, target);
  }

  const hasSession = !!request.cookies.get(SESSION_COOKIE)?.value;

  const decision = resolveAuthRedirect({ pathname, hasSession });

  return decision.redirectTo
    ? NextResponse.redirect(new URL(decision.redirectTo, request.url))
    : NextResponse.next();
}

/**
 * Repassa `/auth/*` e `/api/*` para o gateway/APIs da VPS em dev local. O
 * browser continua vendo apenas `http://localhost:3000` (cookies host-only e
 * `redirect_uri` derivado dessa origem), enquanto o Keycloak mantém o issuer do
 * domínio de produção (o browser segue o 302 do próprio Keycloak).
 */
async function proxyToGateway(request: NextRequest, target: string): Promise<NextResponse> {
  const browserHost = request.headers.get("host") ?? "localhost:3000";
  const proxyHeaders = forwardedHeaders(request.headers, browserHost);

  const cookies = request.cookies.getAll().map((c) => `${c.name}=${c.value}`).join("; ");
  if (cookies) proxyHeaders.set("cookie", cookies);

  let body: BodyInit | null = null;
  if (!["GET", "HEAD"].includes(request.method)) {
    try {
      const buffer = await request.arrayBuffer();
      if (buffer.byteLength > 0) body = buffer;
    } catch {
      body = null;
    }
  }

  const upstream = buildUpstreamUrl(target, request.nextUrl.pathname, request.nextUrl.search);

  const response = await fetch(upstream, {
    method: request.method,
    headers: proxyHeaders,
    body,
    redirect: "manual",
  });

  const out = new NextResponse(response.body, {
    status: response.status,
    statusText: response.statusText,
  });

  copySetCookieHeaders(response.headers, out.headers);
  response.headers.forEach((value, key) => {
    if (HOP_BY_HOP.has(key.toLowerCase()) || key.toLowerCase() === "set-cookie") return;
    if (!out.headers.has(key)) out.headers.set(key, value);
  });

  return out;
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico).*)"],
};