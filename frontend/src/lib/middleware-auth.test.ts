import { describe, it, expect } from "vitest";
import { resolveAuthRedirect, PUBLIC_PATHS } from "./middleware-auth";
import { makeToken } from "./jwt.test";

const NOW = 1_700_000_000;

describe("resolveAuthRedirect (middleware)", () => {
  it("redirects unauthenticated users to login with a redirect param", () => {
    const d = resolveAuthRedirect({ pathname: "/dashboard", accessToken: null, nowSeconds: NOW });
    expect(d.redirectTo).toBe("/login?redirect=%2Fdashboard");
    expect(d.clearCookie).toBe(false);
  });

  it("allows public paths without a token", () => {
    const d = resolveAuthRedirect({ pathname: "/login", accessToken: null, nowSeconds: NOW });
    expect(d.redirectTo).toBeUndefined();
    expect(d.clearCookie).toBe(false);
  });

  it("bounces authenticated users away from public paths", () => {
    const d = resolveAuthRedirect({
      pathname: "/login",
      accessToken: makeToken({ exp: NOW + 600 }),
      nowSeconds: NOW,
    });
    expect(d.redirectTo).toBe("/dashboard");
  });

  it("lets valid tokens through to protected paths", () => {
    const d = resolveAuthRedirect({
      pathname: "/dashboard",
      accessToken: makeToken({ exp: NOW + 600 }),
      nowSeconds: NOW,
    });
    expect(d.redirectTo).toBeUndefined();
    expect(d.clearCookie).toBe(false);
  });

  it("clears an expired token cookie on public paths (kills the redirect loop)", () => {
    const d = resolveAuthRedirect({
      pathname: "/login",
      accessToken: makeToken({ exp: NOW - 10 }),
      nowSeconds: NOW,
    });
    expect(d.redirectTo).toBeUndefined();
    expect(d.clearCookie).toBe(true);
  });

  it("clears expired token and redirects to login on protected paths", () => {
    const d = resolveAuthRedirect({
      pathname: "/users",
      accessToken: makeToken({ exp: NOW - 10 }),
      nowSeconds: NOW,
    });
    expect(d.redirectTo).toBe("/login?redirect=%2Fusers");
    expect(d.clearCookie).toBe(true);
  });

  it("treats a malformed token as expired (clear it)", () => {
    const d = resolveAuthRedirect({ pathname: "/login", accessToken: "garbage", nowSeconds: NOW });
    expect(d.clearCookie).toBe(true);
  });

  it("treats the landing page '/' as public", () => {
    expect(resolveAuthRedirect({ pathname: "/", accessToken: null, nowSeconds: NOW }).redirectTo).toBeUndefined();
    const d = resolveAuthRedirect({ pathname: "/", accessToken: makeToken({ exp: NOW - 5 }), nowSeconds: NOW });
    expect(d.clearCookie).toBe(true);
  });

  it("keeps the auth callback path public", () => {
    expect(PUBLIC_PATHS).toContain("/auth/callback");
    const d = resolveAuthRedirect({ pathname: "/auth/callback", accessToken: null, nowSeconds: NOW });
    expect(d.redirectTo).toBeUndefined();
  });
});
