import { describe, it, expect, beforeEach } from "vitest";
import { TokenManager } from "./token-manager";
import { makeToken } from "@/lib/jwt.test";

describe("TokenManager", () => {
  beforeEach(() => {
    localStorage.clear();
    document.cookie = "accessToken=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/";
  });

  it("stores and reads the keycloak access/refresh tokens", () => {
    TokenManager.setKeycloakToken("access");
    TokenManager.setKeycloakRefreshToken("refresh");

    expect(TokenManager.getAccessToken()).toBe("access");
    expect(TokenManager.getRefreshToken()).toBe("refresh");
    expect(TokenManager.hasTokens()).toBe(true);
    expect(TokenManager.isKeycloakAuth()).toBe(true);
    expect(document.cookie).toContain("accessToken=");
  });

  it("clears tokens and the auth cookie", () => {
    TokenManager.setKeycloakToken("access");
    expect(document.cookie).toContain("accessToken=");

    TokenManager.clearTokens();

    expect(TokenManager.getAccessToken()).toBeNull();
    expect(TokenManager.getRefreshToken()).toBeNull();
    expect(TokenManager.hasTokens()).toBe(false);
    expect(TokenManager.isKeycloakAuth()).toBe(false);
    expect(document.cookie).not.toContain("accessToken=");
  });

  it("returns empty list before any token is stored", () => {
    expect(TokenManager.getRoles()).toEqual([]);
  });

  it("extracts realm roles from the keycloak token (OIDC identity)", () => {
    TokenManager.setKeycloakToken(makeToken({ realm_access: { roles: ["admin", "AGENT"] } }));
    expect(TokenManager.getRoles()).toEqual(["admin", "AGENT"]);
  });

  it("returns empty roles when the roles claim is absent", () => {
    TokenManager.setKeycloakToken(makeToken({ sub: "x" }));
    expect(TokenManager.getRoles()).toEqual([]);
  });
});
