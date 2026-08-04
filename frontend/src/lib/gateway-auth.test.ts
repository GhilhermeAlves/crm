import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import {
  SESSION_COOKIE,
  CSRF_COOKIE,
  CSRF_HEADER,
  loginWithGateway,
  logoutWithGateway,
  getCsrfToken,
  refreshGatewaySession,
} from "./gateway-auth";

describe("gateway-auth (BFF session)", () => {
  beforeEach(() => {
    Object.defineProperty(window, "location", {
      configurable: true,
      value: { assign: vi.fn() },
    });
    document.cookie = `${CSRF_COOKIE}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/`;
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("exposes the HttpOnly session cookie name used by the middleware", () => {
    expect(SESSION_COOKIE).toBe("crm_session");
  });

  it("loginWithGateway navigates to /auth/authorize preserving the redirect", () => {
    loginWithGateway("/leads");
    expect(window.location.assign).toHaveBeenCalledWith(
      "/auth/authorize?redirect=%2Fleads",
    );
  });

  it("loginWithGateway defaults to /dashboard when no redirect is given", () => {
    loginWithGateway();
    expect(window.location.assign).toHaveBeenCalledWith(
      "/auth/authorize?redirect=%2Fdashboard",
    );
  });

  it("logoutWithGateway navigates to /auth/logout", () => {
    logoutWithGateway();
    expect(window.location.assign).toHaveBeenCalledWith("/auth/logout");
  });

  it("getCsrfToken reads the XSRF-TOKEN cookie", () => {
    document.cookie = `${CSRF_COOKIE}=abc123; path=/`;
    expect(getCsrfToken()).toBe("abc123");
  });

  it("getCsrfToken returns null when the cookie is absent", () => {
    expect(getCsrfToken()).toBeNull();
  });

  it("refreshGatewaySession posts /auth/refresh with cookie-to-header CSRF", async () => {
    document.cookie = `${CSRF_COOKIE}=csrf-1; path=/`;
    const fetchMock = vi.spyOn(window, "fetch").mockResolvedValue(
      new Response(null, { status: 204 }),
    );

    await expect(refreshGatewaySession()).resolves.toBe(true);

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("/auth/refresh");
    expect(init.method).toBe("POST");
    expect(init.credentials).toBe("include");
    expect((init.headers as Record<string, string>)[CSRF_HEADER]).toBe("csrf-1");
  });

  it("refreshGatewaySession deduplicates concurrent refreshes", async () => {
    document.cookie = `${CSRF_COOKIE}=csrf-1; path=/`;
    vi.spyOn(window, "fetch").mockResolvedValue(
      new Response(null, { status: 204 }),
    );

    const [a, b] = await Promise.all([refreshGatewaySession(), refreshGatewaySession()]);
    expect(a).toBe(true);
    expect(b).toBe(true);
    expect(window.fetch).toHaveBeenCalledTimes(1);
  });

  it("refreshGatewaySession returns false when the server rejects", async () => {
    document.cookie = `${CSRF_COOKIE}=csrf-1; path=/`;
    vi.spyOn(window, "fetch").mockResolvedValue(
      new Response(null, { status: 401 }),
    );

    await expect(refreshGatewaySession()).resolves.toBe(false);
  });
});
