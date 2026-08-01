import { describe, it, expect, vi, beforeEach } from "vitest";
import {
  initKeycloak,
  loginKeycloak,
  logoutKeycloak,
  refreshAccessToken,
} from "./keycloak";

const { mockKc } = vi.hoisted(() => ({
  mockKc: {
    authenticated: false,
    token: "access.token",
    refreshToken: "refresh.token",
    init: vi.fn().mockResolvedValue(true),
    login: vi.fn().mockResolvedValue(undefined),
    logout: vi.fn().mockResolvedValue(undefined),
    updateToken: vi.fn().mockResolvedValue(false),
  },
}));

vi.mock("keycloak-js", () => ({
  default: vi.fn(function () {
    return mockKc;
  }),
}));

describe("keycloak lib", () => {
  beforeEach(() => {
    mockKc.authenticated = false;
    vi.clearAllMocks();
    localStorage.clear();
    document.cookie = "kc_authenticated=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/";
  });

  it("initializes with PKCE S256 and silent check-sso", async () => {
    await initKeycloak();
    expect(mockKc.init).toHaveBeenCalledWith(
      expect.objectContaining({
        pkceMethod: "S256",
        onLoad: "check-sso",
        checkLoginIframe: false,
      }),
    );
  });

  it("does not re-init when already authenticated", async () => {
    mockKc.authenticated = true;
    await initKeycloak();
    expect(mockKc.init).not.toHaveBeenCalled();
  });

  it("login redirects to the auth callback (PKCE handled by keycloak-js)", async () => {
    await loginKeycloak("/leads");
    expect(mockKc.login).toHaveBeenCalledWith(
      expect.objectContaining({
        redirectUri: expect.stringContaining("/auth/callback?redirect="),
      }),
    );
  });

  it("logout ends the keycloak session with a redirect back to /login", async () => {
    await logoutKeycloak();
    expect(mockKc.logout).toHaveBeenCalledWith(
      expect.objectContaining({ redirectUri: expect.stringContaining("/login") }),
    );
  });

  it("refresh returns false when not authenticated", async () => {
    mockKc.authenticated = false;
    await expect(refreshAccessToken(30)).resolves.toBe(false);
    expect(mockKc.updateToken).not.toHaveBeenCalled();
  });

  it("refresh syncs the updated token to the TokenManager (single writer)", async () => {
    mockKc.authenticated = true;
    mockKc.updateToken.mockResolvedValue(true);
    await expect(refreshAccessToken(30)).resolves.toBe(true);
    expect(mockKc.updateToken).toHaveBeenCalledWith(30);
    expect(localStorage.getItem("kc_accessToken")).toBe("access.token");
    expect(localStorage.getItem("kc_refreshToken")).toBe("refresh.token");
    expect(document.cookie).toContain("kc_authenticated=1");
    expect(document.cookie).not.toContain("access.token");
  });

  it("refresh returns false when the token cannot be refreshed", async () => {
    mockKc.authenticated = true;
    mockKc.updateToken.mockRejectedValue(new Error("refresh failed"));
    await expect(refreshAccessToken(30)).resolves.toBe(false);
  });
});
