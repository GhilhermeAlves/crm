import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import * as React from "react";
import { ProtectedRoute } from "./ProtectedRoute";
import { AuthProvider } from "@/features/auth/hooks/useAuth";

const { pathnameState } = vi.hoisted(() => ({
  pathnameState: { value: "/dashboard" },
}));

const { meMock } = vi.hoisted(() => ({ meMock: vi.fn() }));
const { pushMock } = vi.hoisted(() => ({ pushMock: vi.fn() }));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: pushMock, replace: vi.fn() }),
  usePathname: () => pathnameState.value,
}));

vi.mock("@/features/auth/services/auth.service", () => ({
  AuthService: { me: meMock },
}));

vi.mock("@/lib/gateway-auth", () => ({
  loginWithGateway: vi.fn(),
  logoutWithGateway: vi.fn(),
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

describe("ProtectedRoute (regression Sprint 6.9)", () => {
  beforeEach(() => {
    pathnameState.value = "/dashboard";
    meMock.mockReset();
    pushMock.mockReset();
  });

  it("does NOT navigate to /login after a successful /auth/me (login lands on dashboard)", async () => {
    meMock.mockResolvedValue(mockUser);
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });

    render(
      <QueryClientProvider client={client}>
        <AuthProvider>
          <ProtectedRoute>
            <div>DASHBOARD</div>
          </ProtectedRoute>
        </AuthProvider>
      </QueryClientProvider>,
    );

    await screen.findByText("DASHBOARD");

    await waitFor(() => expect(meMock).toHaveBeenCalledTimes(1));
    expect(pushMock).not.toHaveBeenCalled();
  });
});
