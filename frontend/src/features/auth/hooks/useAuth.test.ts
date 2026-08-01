import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderHook, act, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import * as React from "react";
import { AuthProvider, useAuth } from "./useAuth";
import { TokenManager } from "@/store/token-manager";
import { makeToken } from "@/lib/jwt.test";

const { kcState } = vi.hoisted(() => ({
  kcState: {
    keycloak: null,
    initialized: false,
    authenticated: false,
    token: null as string | null,
    login: vi.fn(),
    logout: vi.fn(),
  },
}));

const { meMock } = vi.hoisted(() => ({ meMock: vi.fn() }));

vi.mock("@/providers/KeycloakProvider", () => ({
  useKeycloak: () => kcState,
}));

vi.mock("@/features/auth/services/auth.service", () => ({
  AuthService: { me: meMock },
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
}));

const mockUser = {
  id: "user-1",
  email: "ana@example.com",
  name: "Ana Silva",
  companyId: "company-1",
  isActive: true,
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-01T00:00:00Z",
};

function makeClient() {
  return new QueryClient({ defaultOptions: { queries: { retry: false } } });
}

describe("AuthProvider", () => {
  beforeEach(() => {
    localStorage.clear();
    kcState.initialized = false;
    kcState.authenticated = false;
    kcState.token = null;
    kcState.logout.mockClear();
    meMock.mockReset();
  });

  function renderAuth() {
    const client = makeClient();
    const wrapper = ({ children }: { children: React.ReactNode }) =>
      React.createElement(
        QueryClientProvider,
        { client },
        React.createElement(AuthProvider, null, children),
      );
    return renderHook(() => useAuth(), { wrapper });
  }

  it("does not call /auth/me before keycloak is initialized (no race condition)", async () => {
    meMock.mockResolvedValue(mockUser);
    const { result, rerender } = renderAuth();

    await act(async () => {});
    expect(meMock).not.toHaveBeenCalled();
    expect(result.current.user).toBeNull();

    kcState.initialized = true;
    kcState.authenticated = true;
    await act(async () => {
      rerender();
    });

    await waitFor(() => expect(result.current.user?.id).toBe("user-1"));
    expect(meMock).toHaveBeenCalledTimes(1);
  });

  it("logout delegates to the keycloak provider", async () => {
    meMock.mockResolvedValue(mockUser);
    kcState.initialized = true;
    kcState.authenticated = true;
    const { result } = renderAuth();

    await waitFor(() => expect(result.current.isAuthenticated).toBe(true));

    act(() => {
      result.current.logout();
    });
    expect(kcState.logout).toHaveBeenCalled();
  });

  it("exposes keycloak realm roles (OIDC identity) and no business permissions yet", async () => {
    meMock.mockResolvedValue(mockUser);
    TokenManager.setTokens(makeToken({ realm_access: { roles: ["AGENT"] } }), null);
    kcState.initialized = true;
    kcState.authenticated = true;

    const { result } = renderAuth();
    await waitFor(() => expect(result.current.isAuthenticated).toBe(true));

    expect(result.current.roles).toEqual(["AGENT"]);
    // Permissões de negócio virão do CurrentUser (endpoint público, Sprint 4);
    // enquanto isso o frontend não as trata como autoridade.
    expect(result.current.permissions).toEqual([]);
  });
});
