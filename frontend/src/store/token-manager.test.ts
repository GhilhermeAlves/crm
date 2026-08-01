import { describe, it, expect, beforeEach } from "vitest";
import { TokenManager } from "./token-manager";

describe("TokenManager", () => {
  beforeEach(() => {
    localStorage.clear();
    document.cookie = "kc_authenticated=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/";
  });

  it("setTokens is the single writer: stores access/refresh tokens and the session flag", () => {
    TokenManager.setTokens("access", "refresh");

    expect(TokenManager.getAccessToken()).toBe("access");
    expect(TokenManager.getRefreshToken()).toBe("refresh");
    // O cookie carrega apenas a flag de sessão — nunca o JWT.
    expect(document.cookie).toContain("kc_authenticated=1");
    expect(document.cookie).not.toContain("access=");
  });

  it("sets the session flag without storing the token value in the cookie", () => {
    TokenManager.setTokens("access.token.value", "refresh");

    expect(document.cookie).toContain("kc_authenticated=1");
    expect(document.cookie).not.toContain("access.token.value");
  });

  it("ignores empty tokens", () => {
    TokenManager.setTokens("", null);

    expect(TokenManager.getAccessToken()).toBeNull();
    expect(document.cookie).not.toContain("kc_authenticated");
  });

  it("clearTokens removes tokens and the session flag", () => {
    TokenManager.setTokens("access", "refresh");
    expect(document.cookie).toContain("kc_authenticated=");

    TokenManager.clearTokens();

    expect(TokenManager.getAccessToken()).toBeNull();
    expect(TokenManager.getRefreshToken()).toBeNull();
    expect(document.cookie).not.toContain("kc_authenticated=");
  });

  it("returns null for both tokens before anything is stored", () => {
    expect(TokenManager.getAccessToken()).toBeNull();
    expect(TokenManager.getRefreshToken()).toBeNull();
  });
});
