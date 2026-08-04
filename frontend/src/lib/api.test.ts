import { describe, it, expect, vi, beforeEach } from "vitest";
import axios from "axios";
import type { AxiosResponse, InternalAxiosRequestConfig } from "axios";
import api from "./api";

const { refreshMock } = vi.hoisted(() => ({ refreshMock: vi.fn() }));

vi.mock("./gateway-auth", async (importOriginal) => {
  const actual = await importOriginal<typeof import("./gateway-auth")>();
  return { ...actual, refreshGatewaySession: refreshMock };
});

type AdapterHandler = (
  config: InternalAxiosRequestConfig,
  callNumber: number,
) => Promise<AxiosResponse>;

let adapterHandler: AdapterHandler;
let requestCount: number;

function okResponse(config: InternalAxiosRequestConfig, data: unknown): AxiosResponse {
  return { data, status: 200, statusText: "OK", headers: {}, config };
}

function unauthorizedError(config: InternalAxiosRequestConfig) {
  return new axios.AxiosError(
    "Request failed with status code 401",
    "ERR_BAD_REQUEST",
    config,
    null,
    { status: 401, statusText: "Unauthorized", headers: {}, data: {}, config },
  );
}

function forbiddenError(config: InternalAxiosRequestConfig) {
  return new axios.AxiosError(
    "Request failed with status code 403",
    "ERR_BAD_REQUEST",
    config,
    null,
    { status: 403, statusText: "Forbidden", headers: {}, data: {}, config },
  );
}

function serverError(config: InternalAxiosRequestConfig) {
  return new axios.AxiosError("Server error", "ERR_BAD_RESPONSE", config, null, {
    status: 500,
    statusText: "Internal Server Error",
    headers: {},
    data: {},
    config,
  });
}

function authHeader(config: InternalAxiosRequestConfig): string | undefined {
  const headers = config.headers as unknown as Record<string, string | undefined>;
  return headers["Authorization"];
}

function setPathname(pathname: string) {
  const location = {
    pathname,
    href: `http://localhost:3000${pathname}`,
    assign: (url: string) => {
      location.href = url;
    },
  };
  Object.defineProperty(window, "location", {
    configurable: true,
    value: location,
  });
}

describe("api interceptors (cookie-based, Sprint 6.4)", () => {
  beforeEach(() => {
    localStorage.clear();
    refreshMock.mockReset();
    requestCount = 0;
    adapterHandler = async () => okResponse({} as InternalAxiosRequestConfig, {});
    api.defaults.adapter = async (config: InternalAxiosRequestConfig) => {
      requestCount += 1;
      return adapterHandler(config, requestCount);
    };
    setPathname("/dashboard");
  });

  it("never sends an Authorization header (BFF holds the token)", async () => {
    const sent: (string | undefined)[] = [];
    adapterHandler = async (config) => {
      sent.push(authHeader(config));
      return okResponse(config, { ok: true });
    };

    await api.get("/contacts");

    expect(sent).toEqual([undefined]);
    expect(refreshMock).not.toHaveBeenCalled();
  });

  it("refreshes once on a 401 and retries the same request", async () => {
    refreshMock.mockResolvedValue(true);
    adapterHandler = async (config, callNumber) => {
      if (callNumber === 1) {
        throw unauthorizedError(config);
      }
      return okResponse(config, { ok: true });
    };

    const res = await api.get("/contacts");

    expect(res.data).toEqual({ ok: true });
    expect(requestCount).toBe(2); // original + único retry
    expect(refreshMock).toHaveBeenCalledTimes(1);
  });

  it("does not loop when the backend keeps returning 401 after a refresh", async () => {
    refreshMock.mockResolvedValue(true);
    adapterHandler = async (config) => {
      throw unauthorizedError(config);
    };

    await expect(api.get("/contacts")).rejects.toBeTruthy();

    expect(requestCount).toBe(2); // original + único retry, nunca mais
    expect(refreshMock).toHaveBeenCalledTimes(1);
    expect(window.location.href).toBe("/login");
  });

  it("redirects to login when the refresh fails (non-public page)", async () => {
    refreshMock.mockResolvedValue(false);
    adapterHandler = async (config) => {
      throw unauthorizedError(config);
    };

    await expect(api.get("/contacts")).rejects.toBeTruthy();

    expect(refreshMock).toHaveBeenCalledTimes(1);
    expect(window.location.href).toBe("/login");
  });

  it("does not redirect when the refresh fails on a public page (no loop on /login)", async () => {
    setPathname("/login");
    refreshMock.mockResolvedValue(false);
    adapterHandler = async (config) => {
      throw unauthorizedError(config);
    };

    await expect(api.get("/auth/me")).rejects.toBeTruthy();

    // Em página pública não há assign — href permanece o mesmo (sem loop).
    expect(window.location.href).toBe("http://localhost:3000/login");
  });

  it("does not trigger a refresh on non-401 errors", async () => {
    adapterHandler = async (config) => {
      throw serverError(config);
    };

    await expect(api.get("/contacts")).rejects.toBeTruthy();
    expect(refreshMock).not.toHaveBeenCalled();
  });

  it("does not convert a 403 (CRM_ACCESS_DENIED) into a login/refresh", async () => {
    adapterHandler = async (config) => {
      throw forbiddenError(config);
    };

    await expect(api.get("/users")).rejects.toMatchObject({
      response: { status: 403 },
    });
    expect(refreshMock).not.toHaveBeenCalled();
    expect(window.location.href).toBe("http://localhost:3000/dashboard");
  });
});
