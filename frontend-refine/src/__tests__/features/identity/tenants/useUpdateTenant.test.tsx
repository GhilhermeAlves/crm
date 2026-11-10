import { describe, it, expect, vi } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import * as React from "react";
import { useUpdateTenant } from "@/features/identity/tenants/hooks/useTenants";

vi.mock("sonner", () => ({ toast: { success: vi.fn(), error: vi.fn() } }));
vi.mock("@/features/identity/tenants/services/tenant.service", () => ({
  TenantService: { update: vi.fn().mockResolvedValue({ id: "c1" }) },
  CompanyPreferencesService: {},
}));

describe("useUpdateTenant", () => {
  it("recarrega empresas, módulos e cobrança (o plano muda módulos e valor)", async () => {
    const queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
    const spy = vi.spyOn(queryClient, "invalidateQueries");
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    const { result } = renderHook(() => useUpdateTenant(), { wrapper });
    result.current.mutate({ id: "c1", data: { plan: "business" } as never });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    const keys = spy.mock.calls.map((call) => JSON.stringify(call[0]?.queryKey));
    expect(keys).toContain(JSON.stringify(["company-modules"]));
    expect(keys).toContain(JSON.stringify(["billing"]));
  });
});
