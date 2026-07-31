import { describe, it, expect } from "vitest";
import { decodeJwtPayload, getJwtExpiration, isJwtExpired } from "./jwt";

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

  it("reads the expiration claim", () => {
    const now = Math.floor(Date.now() / 1000);
    expect(getJwtExpiration(makeToken({ exp: now + 600 }))).toBe(now + 600);
    expect(getJwtExpiration(null)).toBeNull();
  });

  it("detects expired and valid tokens", () => {
    const now = Math.floor(Date.now() / 1000);
    expect(isJwtExpired(makeToken({ exp: now + 600 }), now)).toBe(false);
    expect(isJwtExpired(makeToken({ exp: now - 10 }), now)).toBe(true);
    expect(isJwtExpired(makeToken({ exp: now }), now)).toBe(true);
  });

  it("treats a token without exp as expired (defensive)", () => {
    expect(isJwtExpired(makeToken({ sub: "x" }), 1000)).toBe(true);
    expect(isJwtExpired(null)).toBe(true);
  });
});
