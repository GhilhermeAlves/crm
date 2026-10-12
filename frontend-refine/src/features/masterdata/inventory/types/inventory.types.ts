import type { CatalogItem } from "@/features/masterdata/catalog/types/catalog.types";

export type StockUnit = "CAIXA" | "CARTELA" | "MILILITROS" | "PACOTE" | "ROLO" | "UNIDADE";

export const UNIT_LABELS: Record<StockUnit, string> = {
  CAIXA: "Caixa",
  CARTELA: "Cartela",
  MILILITROS: "Mililitros",
  PACOTE: "Pacote",
  ROLO: "Rolo",
  UNIDADE: "Unidade",
};

export const STOCK_UNITS = Object.keys(UNIT_LABELS) as StockUnit[];

export type StockMovementType = "ENTRADA" | "SAIDA" | "AJUSTE";

export const STOCK_MOVEMENT_TYPE_LABELS: Record<StockMovementType, string> = {
  ENTRADA: "Entrada",
  SAIDA: "Saída",
  AJUSTE: "Ajuste",
};

export const STOCK_MOVEMENT_TYPES = Object.keys(STOCK_MOVEMENT_TYPE_LABELS) as StockMovementType[];

export type InventoryItem = Pick<
  CatalogItem,
  "id" | "name" | "category" | "sku" | "active" | "unit" | "minStock" | "quantity"
>;

export type InventoryFilters = {
  q?: string;
  category?: string;
  lowStock?: boolean;
  active?: boolean;
  page?: number;
  pageSize?: number;
};

export type InventoryPage = {
  content: InventoryItem[];
  page: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
};

export type StockMovement = {
  id: string;
  catalogItemId: string;
  type: StockMovementType;
  delta: number;
  note: string | null;
  /** Só vem preenchido na resposta do registro; no histórico é null. */
  balanceAfter: number | null;
  createdAt: string;
};

export type StockMovementPage = {
  content: StockMovement[];
  page: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
};

export type RegisterMovementRequest = {
  type: StockMovementType;
  quantity: number;
  note?: string;
};

export function isLowStock(item: Pick<InventoryItem, "quantity" | "minStock">): boolean {
  return item.minStock > 0 && item.quantity <= item.minStock;
}