import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { StockMovementDialog } from "@/features/masterdata/inventory/components/StockMovementDialog";
import type { InventoryItem } from "@/features/masterdata/inventory/types/inventory.types";

const item: InventoryItem = {
  id: "item-1",
  name: "Luva",
  category: "EPI",
  sku: null,
  active: true,
  unit: "CAIXA",
  minStock: 5,
  quantity: 2,
};

describe("StockMovementDialog", () => {
  it("chama onSubmit com entrada após preencher e salvar", () => {
    const onSubmit = vi.fn();
    render(
      <StockMovementDialog
        open
        onOpenChange={() => {}}
        item={item}
        isSubmitting={false}
        onSubmit={onSubmit}
      />,
    );

    fireEvent.click(screen.getByLabelText("Tipo"));
    fireEvent.click(screen.getByRole("option", { name: "Entrada" }));
    fireEvent.change(screen.getByLabelText("Quantidade"), { target: { value: "10" } });
    fireEvent.change(screen.getByLabelText("Observação"), { target: { value: "Compra" } });
    fireEvent.click(screen.getByRole("button", { name: "Registrar" }));

    expect(onSubmit).toHaveBeenCalledWith({ type: "ENTRADA", quantity: 10, note: "Compra" });
  });
});
