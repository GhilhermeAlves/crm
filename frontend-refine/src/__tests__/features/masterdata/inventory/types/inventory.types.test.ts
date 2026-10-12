import { describe, expect, it } from "vitest";
import {
  STOCK_MOVEMENT_TYPES,
  isLowStock,
  STOCK_UNITS,
} from "@/features/masterdata/inventory/types/inventory.types";

describe("inventory.types helpers", () => {
  it("isLowStock considera minStock > 0", () => {
    expect(isLowStock({ quantity: 0, minStock: 0 })).toBe(false);
    expect(isLowStock({ quantity: 5, minStock: 5 })).toBe(true);
    expect(isLowStock({ quantity: 6, minStock: 5 })).toBe(false);
  });

  it("expõe as constants dos enums", () => {
    expect(STOCK_MOVEMENT_TYPES).toEqual(["ENTRADA", "SAIDA", "AJUSTE"]);
    expect(STOCK_UNITS).toContain("CAIXA");
  });
});
