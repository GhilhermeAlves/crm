import { describe, it, expect, vi, beforeEach } from "vitest";
import axios from "axios";
import type { AxiosResponse, InternalAxiosRequestConfig } from "axios";
import api from "./api";
import { TokenManager } from "@/store/token-manager";
import { refreshAccessToken } from "./keycloak";

const { refreshMock } = vi.hoisted(() => ({ refreshMock: vi.fn() }));

vi.mock("@/lib/keycloak", () => ({ refreshAccessToken: refreshMock }));

type AdapterHandler = (
  config: InternalAxiosRequestConfig,
  callNumber: number,
) => Promise<AxiosResponse>;

let adapterHandler: AdapterHandler;
let requestCount: number;

function okResponse(config: InternalAxiosRequestConfig, data: unknown): AxiosResponse {
  return { data, status: 200, statusText: "OK", headers: {}, config };
}

function authHeader(config: InternalAxiosRequestConfig): string | undefined {
  const headers = config.headers as unknown as Record<string, string | undefined>;
  return headers["Authorization"];
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

function serverError(config: InternalAxiosRequestConfig) {
  return new axios.AxiosError("Server error", "ERR_BAD_RESPONSE", config, null, {
    status: 500,
    statusText: "Internal Server Error",
    headers: {},
    data: {},
    config,
  });
}

describe("api interceptors", () => {
  beforeEach(() => {
    localStorage.clear();
    document.cookie = "kc_authenticated=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/";
    refreshMock.mockReset();
    requestCount = 0;
    adapterHandler = async () => okResponse({} as InternalAxiosRequestConfig, {});
    api.defaults.adapter = async (config: InternalAxiosRequestConfig) => {
      requestCount += 1;
      return adapterHandler(config, requestCount);
    };
    Object.defineProperty(window, "location", {
      configurable: true,
      value: { href: "http://localhost:3000/" },
    });
  });

  it("refreshes on a real 401 and retries with the new token", async () => {
    TokenManager.setTokens("old.token", "refresh");

    let refreshCalls = 0;
    refreshMock.mockImplementation(async () => {
      refreshCalls += 1;
      if (refreshCalls > 1) {
        TokenManager.setTokens("new.token", "refresh");
      }
      return true;
    });

    const sentTokens: (string | undefined)[] = [];
    adapterHandler = async (config, callNumber) => {
      sentTokens.push(authHeader(config));
      if (callNumber === 1) {
        throw unauthorizedError(config);
      }
      return okResponse(config, { ok: true });
    };

    const res = await api.get("/contacts");

    expect(res.data).toEqual({ ok: true });
    expect(sentTokens).toEqual(["Bearer old.token", "Bearer new.token"]);
    expect(localStorage.getItem("kc_accessToken")).toBe("new.token");
    // 1 = interceptor de request (proativo) · 2 = refresh no 401 · 3 = interceptor de request no retry
    expect(refreshCalls).toBe(3);
  });

  it("does not loop when the backend keeps rejecting after a refresh", async () => {
    TokenManager.setTokens("old.token", "refresh");

    let refreshCalls = 0;
    refreshMock.mockImplementation(async () => {
      refreshCalls += 1;
      if (refreshCalls === 2) {
        TokenManager.setTokens("new.token", "refresh");
      }
      return true;
    });

    adapterHandler = async (config) => {
      throw unauthorizedError(config);
    };

    await expect(api.get("/contacts")).rejects.toBeTruthy();

    expect(requestCount).toBe(2); // original + único retry, nunca mais
    expect(refreshCalls).toBe(3);
    expect(localStorage.getItem("kc_accessToken")).toBeNull(); // sessão inválida
    expect(window.location.href).toBe("/login");
  });

  it("clears tokens and redirects to login when the refresh fails", async () => {
    TokenManager.setTokens("old.token", "refresh");
    refreshMock.mockResolvedValue(false);

    adapterHandler = async (config) => {
      throw unauthorizedError(config);
    };

    await expect(api.get("/contacts")).rejects.toBeTruthy();

    expect(localStorage.getItem("kc_accessToken")).toBeNull();
    expect(window.location.href).toBe("/login");
  });

  it("does not trigger a refresh on non-401 errors", async () => {
    TokenManager.setTokens("old.token", "refresh");

    adapterHandler = async (config) => {
      throw serverError(config);
    };

    await expect(api.get("/contacts")).rejects.toBeTruthy();

    // apenas o refresh proativo do interceptor de request (token perto de expirar)
    expect(refreshMock).toHaveBeenCalledTimes(1);
    expect(localStorage.getItem("kc_accessToken")).toBe("old.token");
  });

  it("sends no Authorization header when there is no token", async () => {
    const sent: (string | undefined)[] = [];
    adapterHandler = async (config) => {
      sent.push(authHeader(config));
      return okResponse(config, { ok: true });
    };

    await api.get("/public");

    expect(sent).toEqual([undefined]);
    expect(refreshMock).not.toHaveBeenCalled();
  });
});
