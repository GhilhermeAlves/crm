import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import {
  SESSION_COOKIE,
  CSRF_COOKIE,
  CSRF_HEADER,
  loginWithGateway,
  logoutWithGateway,
  getCsrfToken,
  refreshGatewaySession,
  getLinkStatus,
  linkAccountWithPassword,
  GatewayLinkError,
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

  it("loginWithGateway adds the provider hint preserving the redirect", () => {
    loginWithGateway("/leads", "google");
    expect(window.location.assign).toHaveBeenCalledWith(
      "/auth/authorize?redirect=%2Fleads&provider=google",
    );
  });

  it("loginWithGateway without redirect keeps the provider hint", () => {
    loginWithGateway(undefined, "microsoft");
    expect(window.location.assign).toHaveBeenCalledWith(
      "/auth/authorize?redirect=%2Fdashboard&provider=microsoft",
    );
  });

  it("loginWithGateway encodes the provider alias safely", () => {
    loginWithGateway("/leads", "apple");
    expect(window.location.assign).toHaveBeenCalledWith(
      "/auth/authorize?redirect=%2Fleads&provider=apple",
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

  it("getLinkStatus reports a pending link with the account email", async () => {
    vi.spyOn(window, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ pending: true, email: "ana@exemplo.com" }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );

    await expect(getLinkStatus()).resolves.toEqual({
      pending: true,
      email: "ana@exemplo.com",
    });

    const [url, init] = (window.fetch as ReturnType<typeof vi.fn>).mock
      .calls[0] as [string, RequestInit];
    expect(url).toBe("/auth/link-status");
    expect(init.credentials).toBe("include");
  });

  it("getLinkStatus returns pending=false when there is no pending link", async () => {
    vi.spyOn(window, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ pending: false }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );

    await expect(getLinkStatus()).resolves.toEqual({ pending: false });
  });

  it("getLinkStatus returns pending=false when the server errors", async () => {
    vi.spyOn(window, "fetch").mockResolvedValue(new Response(null, { status: 500 }));
    await expect(getLinkStatus()).resolves.toEqual({ pending: false });
  });

  it("linkAccountWithPassword posts /auth/link with cookie-to-header CSRF", async () => {
    document.cookie = `${CSRF_COOKIE}=csrf-link; path=/`;
    vi.spyOn(window, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ redirect: "/leads" }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );

    await expect(linkAccountWithPassword("segredo")).resolves.toEqual({
      redirect: "/leads",
    });

    const [url, init] = (window.fetch as ReturnType<typeof vi.fn>).mock
      .calls[0] as [string, RequestInit];
    expect(url).toBe("/auth/link");
    expect(init.method).toBe("POST");
    expect(init.credentials).toBe("include");
    const headers = init.headers as Record<string, string>;
    expect(headers[CSRF_HEADER]).toBe("csrf-link");
    expect(JSON.parse(init.body as string)).toEqual({ password: "segredo" });
  });

  it("linkAccountWithPassword surfaces INVALID_CREDENTIALS as a typed error", async () => {
    document.cookie = `${CSRF_COOKIE}=csrf-link; path=/`;
    vi.spyOn(window, "fetch").mockResolvedValue(
      new Response(
        JSON.stringify({
          status: 401,
          code: "INVALID_CREDENTIALS",
          message: "Senha da conta local inválida.",
        }),
        { status: 401, headers: { "Content-Type": "application/json" } },
      ),
    );

    const promise = linkAccountWithPassword("errada");
    await expect(promise).rejects.toBeInstanceOf(GatewayLinkError);
    await promise.catch((error) => {
      expect((error as GatewayLinkError).code).toBe("INVALID_CREDENTIALS");
    });
  });
});
