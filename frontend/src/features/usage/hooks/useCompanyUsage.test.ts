import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import * as React from "react";
import { useCompanyUsage } from "./useCompanyUsage";

const { usageMock } = vi.hoisted(() => ({ usageMock: vi.fn() }));

vi.mock("../services/usage.service", () => ({
  UsageService: { companyUsage: usageMock },
}));

const usage = {
  users: { current: 4, limit: 10 },
  contacts: { current: 120, limit: 1000 },
  storage: { currentMb: 350, limitMb: 5000 },
};

function makeClient() {
  return new QueryClient({ defaultOptions: { queries: { retry: false } } });
}

function renderUsage(companyId?: string | null, enabled = true) {
  const client = makeClient();
  const wrapper = ({ children }: { children: React.ReactNode }) =>
    React.createElement(QueryClientProvider, { client }, children);
  return renderHook(() => useCompanyUsage(companyId, enabled), { wrapper });
}

describe("useCompanyUsage (Sprint 8.6)", () => {
  beforeEach(() => usageMock.mockReset());

  it("fetches usage when a company is active", async () => {
    usageMock.mockResolvedValue(usage);
    const { result } = renderUsage("company-1");

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(usageMock).toHaveBeenCalledWith("company-1");
    expect(result.current.data?.users.limit).toBe(10);
    expect(result.current.data?.storage.limitMb).toBe(5000);
  });

  it("does not fetch when there is no active company", async () => {
    renderUsage(null);

    expect(usageMock).not.toHaveBeenCalled();
  });
});