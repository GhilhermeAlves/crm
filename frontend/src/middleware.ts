import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { resolveAuthRedirect, SESSION_COOKIE } from "@/lib/middleware-auth";

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const hasSession = !!request.cookies.get(SESSION_COOKIE)?.value;

  const decision = resolveAuthRedirect({ pathname, hasSession });

  return decision.redirectTo
    ? NextResponse.redirect(new URL(decision.redirectTo, request.url))
    : NextResponse.next();
}

export const config = {
  matcher: ["/((?!api|_next/static|_next/image|favicon.ico|docs|silent-check-sso\\.html).*)"],
};
