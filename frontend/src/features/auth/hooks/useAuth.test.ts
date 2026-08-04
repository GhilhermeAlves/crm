import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderHook, act, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import * as React from "react";
import { AuthProvider, useAuth } from "./useAuth";

const { pathnameState } = vi.hoisted(() => ({
  pathnameState: { value: "/dashboard" },
}));

const { logoutMock } = vi.hoisted(() => ({ logoutMock: vi.fn() }));
const { meMock } = vi.hoisted(() => ({ meMock: vi.fn() }));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
  usePathname: () => pathnameState.value,
}));

vi.mock("@/features/auth/services/auth.service", () => ({
  AuthService: { me: meMock },
}));

vi.mock("@/lib/gateway-auth", () => ({
  loginWithGateway: vi.fn(),
  logoutWithGateway: logoutMock,
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

describe("AuthProvider (Sprint 6.4, session via gateway)", () => {
  beforeEach(() => {
    localStorage.clear();
    pathnameState.value = "/dashboard";
    logoutMock.mockClear();
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

  it("fetches the business identity from /auth/me on a protected page", async () => {
    meMock.mockResolvedValue(mockUser);
    const { result } = renderAuth();

    await waitFor(() => expect(result.current.isAuthenticated).toBe(true));

    expect(meMock).toHaveBeenCalledTimes(1);
    expect(result.current.user?.email).toBe("ana@example.com");
  });

  it("does not call /auth/me on public pages (no session requirement)", async () => {
    pathnameState.value = "/login";
    meMock.mockResolvedValue(mockUser);
    const { result } = renderAuth();

    await act(async () => {});
    expect(meMock).not.toHaveBeenCalled();
    expect(result.current.user).toBeNull();
  });

  it("clears the user when /auth/me fails (session invalid)", async () => {
    meMock.mockRejectedValue(new Error("401"));
    const { result } = renderAuth();

    await waitFor(() => expect(result.current.isAuthenticated).toBe(false));
    expect(result.current.user).toBeNull();
  });

  it("logout delegates to the gateway end-session", () => {
    const { result } = renderAuth();

    act(() => {
      result.current.logout();
    });

    expect(logoutMock).toHaveBeenCalledTimes(1);
  });

  it("exposes no roles/permissions in the browser (BFF holds the claims)", async () => {
    meMock.mockResolvedValue(mockUser);
    const { result } = renderAuth();

    await waitFor(() => expect(result.current.isAuthenticated).toBe(true));

    expect(result.current.roles).toEqual([]);
    expect(result.current.permissions).toEqual([]);
  });
});
