import { StatusBadge } from "@/components/ui/status-badge";
import { isLowStock, type InventoryItem } from "../types/inventory.types";

export function LowStockBadge({ item }: { item: Pick<InventoryItem, "quantity" | "minStock"> }) {
  if (!isLowStock(item)) return null;
  return <StatusBadge intent="warning">Baixo estoque</StatusBadge>;
}