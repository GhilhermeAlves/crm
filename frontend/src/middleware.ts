import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { resolveAuthRedirect, SESSION_COOKIE } from "@/lib/middleware-auth";

export async function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  const hasSession = !!request.cookies.get(SESSION_COOKIE)?.value;

  const decision = resolveAuthRedirect({ pathname, hasSession });

  return decision.redirectTo
    ? NextResponse.redirect(new URL(decision.redirectTo, request.url))
    : NextResponse.next();
}

export const config = {
  // Em produção o nginx encaminha /auth e /api direto ao auth-service, sem
  // passar pelo Next. Exclusão replicada no matcher (defensiva) para o dev
  // local: nessas rotas o proxy (rewrites) responde, não o middleware.
  matcher: ["/((?!auth|api|_next/static|_next/image|favicon.ico|docs).*)"],
};
