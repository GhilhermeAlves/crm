import { beforeEach, describe, expect, it, vi } from "vitest";

const { getMock, postMock } = vi.hoisted(() => ({
  getMock: vi.fn(),
  postMock: vi.fn(),
}));

vi.mock("@/lib/api", () => ({
  default: { get: getMock, post: postMock },
}));

import { InventoryService } from "@/features/masterdata/inventory/services/inventory.service";

describe("InventoryService", () => {
  const companyId = "comp-1";

  beforeEach(() => {
    getMock.mockReset();
    postMock.mockReset();
  });

  it("list passa filtros e paginação como query params", async () => {
    getMock.mockResolvedValue({
      data: { content: [], page: 0, pageSize: 20, totalElements: 0, totalPages: 0 },
    });
    await InventoryService.list(companyId, { q: "luva", category: "EPI", lowStock: true, page: 2 });
    expect(getMock).toHaveBeenCalledWith(`/companies/${companyId}/inventory`, {
      params: {
        q: "luva",
        category: "EPI",
        lowStock: true,
        active: undefined,
        page: 2,
        pageSize: 20,
      },
    });
  });

  it("listMovements busca na rota do item", async () => {
    getMock.mockResolvedValue({
      data: { content: [], page: 0, pageSize: 20, totalElements: 0, totalPages: 0 },
    });
    await InventoryService.listMovements(companyId, "item-1", 0, 20);
    expect(getMock).toHaveBeenCalledWith(`/companies/${companyId}/inventory/item-1/movements`, {
      params: { page: 0, pageSize: 20 },
    });
  });

  it("registerMovement faz POST na rota do item", async () => {
    postMock.mockResolvedValue({ data: { id: "m1" } });
    await InventoryService.registerMovement(companyId, "item-1", {
      type: "ENTRADA",
      quantity: 10,
      note: "Compra",
    });
    expect(postMock).toHaveBeenCalledWith(`/companies/${companyId}/inventory/item-1/movements`, {
      type: "ENTRADA",
      quantity: 10,
      note: "Compra",
    });
  });
});
