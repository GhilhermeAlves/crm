import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { resolveAuthRedirect } from "@/lib/middleware-auth";

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const rawCookie = request.cookies.get("accessToken")?.value;
  let accessToken: string | null = rawCookie ?? null;
  if (accessToken) {
    try {
      accessToken = decodeURIComponent(accessToken);
    } catch {
      // mantém o valor cru se não estiver URL-encoded
    }
  }

  const decision = resolveAuthRedirect({ pathname, accessToken });

  const response = decision.redirectTo
    ? NextResponse.redirect(new URL(decision.redirectTo, request.url))
    : NextResponse.next();

  if (decision.clearCookie) {
    response.cookies.delete("accessToken");
  }

  return response;
}

export const config = {
  matcher: ["/((?!api|_next/static|_next/image|favicon.ico|docs|silent-check-sso\\.html).*)"],
};
