import { describe, expect, it, vi, beforeEach } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { PropsWithChildren } from "react";
import { useRegisterMovement } from "@/features/masterdata/inventory/hooks/useInventory";
import { InventoryService } from "@/features/masterdata/inventory/services/inventory.service";

vi.mock("@/features/masterdata/inventory/services/inventory.service", () => ({
  InventoryService: {
    list: vi.fn(),
    listMovements: vi.fn(),
    registerMovement: vi.fn(),
  },
}));

vi.mock("sonner", () => ({ toast: { success: vi.fn(), error: vi.fn() } }));

const wrapper = ({ children }: PropsWithChildren) => (
  <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
    {children}
  </QueryClientProvider>
);

describe("useRegisterMovement", () => {
  beforeEach(() => vi.clearAllMocks());

  it("envia o movimento e invalida as queries de inventory", async () => {
    (InventoryService.registerMovement as ReturnType<typeof vi.fn>).mockResolvedValue({ id: "m1" });
    const { result } = renderHook(() => useRegisterMovement("comp-1"), { wrapper });

    result.current.mutate({
      itemId: "item-1",
      request: { type: "ENTRADA", quantity: 5 },
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(InventoryService.registerMovement).toHaveBeenCalledWith("comp-1", "item-1", {
      type: "ENTRADA",
      quantity: 5,
    });
  });
});
