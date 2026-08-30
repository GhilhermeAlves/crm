import { describe, expect, it } from "vitest";
import {
  DEV_GATEWAY_TARGET,
  buildUpstreamUrl,
  copySetCookieHeaders,
  forwardedHeaders,
  isDevProxyPath,
} from "./dev-proxy";

describe("dev-proxy", () => {
  it("desliga por padrão quando DEV_GATEWAY_TARGET não existe", () => {
    expect(DEV_GATEWAY_TARGET).toBe("");
  });

  it("reconhece apenas os caminhos do gateway/API", () => {
    expect(isDevProxyPath("/auth/authorize")).toBe(true);
    expect(isDevProxyPath("/auth")).toBe(true);
    expect(isDevProxyPath("/api/v1/leads")).toBe(true);
    expect(isDevProxyPath("/api")).toBe(true);
    expect(isDevProxyPath("/login")).toBe(false);
    expect(isDevProxyPath("/dashboard")).toBe(false);
  });

  it("monta a URL upstream preservando path e query", () => {
    expect(buildUpstreamUrl("https://srv1348261.hstgr.cloud/", "/auth/authorize", "?redirect=%2Fdashboard"))
      .toBe("https://srv1348261.hstgr.cloud/auth/authorize?redirect=%2Fdashboard");
    expect(buildUpstreamUrl("https://srv1348261.hstgr.cloud", "/api/v1/leads", ""))
      .toBe("https://srv1348261.hstgr.cloud/api/v1/leads");
  });

  it("adiciona X-Forwarded-Host/Proto removendo hop-by-hop", () => {
    const headers = new Headers({
      host: "localhost:3000",
      cookie: "crm_session=abc",
      connection: "keep-alive",
      "x-forwarded-proto": "https",
    });
    headers.set("x-forwarded-host", "evil.example");

    const out = forwardedHeaders(headers, "localhost:3000");

    expect(out.get("x-forwarded-host")).toBe("localhost:3000");
    expect(out.get("x-forwarded-proto")).toBe("http");
    expect(out.has("connection")).toBe(false);
    expect(out.get("cookie")).toBe("crm_session=abc");
  });

  it("copia Set-Cookie preservando cookies individuais quando getSetCookie está disponível", () => {
    const source = new Headers();
    const setCookieFn = () => ["crm_session=token; Path=/; HttpOnly", "XSRF-TOKEN=csrf; Path=/"];
    Object.defineProperty(source, "getSetCookie", { value: setCookieFn });
    source.set("content-type", "application/json");

    const target = new Headers({ "set-cookie": "stale=value" });
    copySetCookieHeaders(source, target);

    expect(target.get("set-cookie")).toBe(
      "crm_session=token; Path=/; HttpOnly, XSRF-TOKEN=csrf; Path=/",
    );
    expect(target.get("content-type")).toBe(null);
  });
});