import { describe, it, expect } from "vitest";
import { resolveAuthRedirect, PUBLIC_PATHS, SESSION_COOKIE } from "@/lib/middleware-auth";

describe("resolveAuthRedirect (middleware, flag-only)", () => {
  it("redirects unauthenticated users (no session flag) to login with a redirect param", () => {
    const d = resolveAuthRedirect({
      pathname: "/dashboard",
      hasSession: false,
    });
    expect(d.redirectTo).toBe("/login?redirect=%2Fdashboard");
  });

  it("keeps the query string in the login redirect", () => {
    const d = resolveAuthRedirect({
      pathname: "/settings/company/users",
      search: "?tab=pending",
      hasSession: false,
    });
    expect(d.redirectTo).toBe("/login?redirect=%2Fsettings%2Fcompany%2Fusers%3Ftab%3Dpending");
  });

  it("redirects legacy /invitations/accept?token=X to /convite/X, with or without session", () => {
    for (const hasSession of [false, true]) {
      const d = resolveAuthRedirect({
        pathname: "/invitations/accept",
        search: "?token=abc123",
        hasSession,
      });
      expect(d.redirectTo).toBe("/convite/abc123");
    }
  });

  it("encodes the legacy token when building /convite/{token}", () => {
    const d = resolveAuthRedirect({
      pathname: "/invitations/accept",
      search: "?token=a%2Fb",
      hasSession: false,
    });
    expect(d.redirectTo).toBe("/convite/a%2Fb");
  });

  it("treats /convite/{token} as public (signup without a session)", () => {
    expect(
      resolveAuthRedirect({ pathname: "/convite/abc123", hasSession: false }).redirectTo,
    ).toBeUndefined();
  });

  it("allows public paths without a session flag", () => {
    expect(
      resolveAuthRedirect({ pathname: "/login", hasSession: false }).redirectTo,
    ).toBeUndefined();
    expect(
      resolveAuthRedirect({ pathname: "/register", hasSession: false }).redirectTo,
    ).toBeUndefined();
    expect(
      resolveAuthRedirect({ pathname: "/forgot-password", hasSession: false }).redirectTo,
    ).toBeUndefined();
    expect(
      resolveAuthRedirect({ pathname: "/reset-password", hasSession: false }).redirectTo,
    ).toBeUndefined();
  });

  it("lets protected paths through when the session flag exists", () => {
    const d = resolveAuthRedirect({ pathname: "/dashboard", hasSession: true });
    expect(d.redirectTo).toBeUndefined();
  });

  it("keeps the landing page '/' public regardless of the flag", () => {
    expect(resolveAuthRedirect({ pathname: "/", hasSession: false }).redirectTo).toBeUndefined();
    expect(resolveAuthRedirect({ pathname: "/", hasSession: true }).redirectTo).toBeUndefined();
  });

  it("keeps the auth callback path public", () => {
    expect(PUBLIC_PATHS).toContain("/auth/callback");
    expect(
      resolveAuthRedirect({ pathname: "/auth/callback", hasSession: false }).redirectTo,
    ).toBeUndefined();
  });

  it("keeps the link-account path public (Sprint 7.2)", () => {
    expect(PUBLIC_PATHS).toContain("/link-account");
    expect(
      resolveAuthRedirect({ pathname: "/link-account", hasSession: false }).redirectTo,
    ).toBeUndefined();
  });

  it("exposes the session cookie name used by the middleware", () => {
    expect(SESSION_COOKIE).toBe("crm_session");
  });

  it("never interprets the token: decision depends only on the flag", () => {
    // Independente de conteúdo de token — o middleware apenas lê a flag.
    const d = resolveAuthRedirect({ pathname: "/users", hasSession: true });
    expect(d.redirectTo).toBeUndefined();
  });
});
