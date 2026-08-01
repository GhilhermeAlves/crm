import { describe, it, expect } from "vitest";
import { decodeJwtPayload, getRealmRoles } from "./jwt";

export function makeToken(payload: Record<string, unknown>): string {
  const enc = (value: Record<string, unknown>) =>
    btoa(JSON.stringify(value))
      .replace(/\+/g, "-")
      .replace(/\//g, "_")
      .replace(/=+$/, "");
  return `header.${enc(payload)}.signature`;
}

describe("jwt helpers", () => {
  it("decodes the JWT payload", () => {
    const payload = { sub: "abc", realm_access: { roles: ["AGENT"] } };
    expect(decodeJwtPayload(makeToken(payload))).toEqual(payload);
  });

  it("returns null for null/invalid tokens", () => {
    expect(decodeJwtPayload(null)).toBeNull();
    expect(decodeJwtPayload(undefined)).toBeNull();
    expect(decodeJwtPayload("not-a-jwt")).toBeNull();
    expect(decodeJwtPayload("a..b")).toBeNull();
  });

  it("extracts realm roles from the token (OIDC identity, UX only)", () => {
    expect(getRealmRoles(makeToken({ realm_access: { roles: ["admin", "AGENT"] } }))).toEqual([
      "admin",
      "AGENT",
    ]);
  });

  it("returns empty roles when the roles claim is absent", () => {
    expect(getRealmRoles(makeToken({ sub: "x" }))).toEqual([]);
    expect(getRealmRoles(null)).toEqual([]);
    expect(getRealmRoles("garbage")).toEqual([]);
  });
});
