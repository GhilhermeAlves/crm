import type { StockUnit } from "@/features/masterdata/inventory/types/inventory.types";

export type CatalogItemType = "PRODUCT" | "SERVICE";

export type CatalogCategory = "EPI" | "INSTRUMENTO" | "MATERIAL_DE_CONSUMO" | "MEDICAMENTO" | "OUTROS";

export const CATALOG_CATEGORY_LABELS: Record<CatalogCategory, string> = {
  EPI: "EPI",
  INSTRUMENTO: "Instrumento",
  MATERIAL_DE_CONSUMO: "Material de consumo",
  MEDICAMENTO: "Medicamento",
  OUTROS: "Outros",
};

export const CATALOG_CATEGORIES = Object.keys(CATALOG_CATEGORY_LABELS) as CatalogCategory[];

export type CatalogItem = {
  id: string;
  companyId: string;
  type: CatalogItemType;
  name: string;
  description: string | null;
  category: CatalogCategory | null;
  price: number | null;
  currency: string;
  sku: string | null;
  unit: StockUnit | null;
  minStock: number;
  quantity: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type CatalogItemRequest = {
  type: CatalogItemType;
  name: string;
  description?: string;
  category?: CatalogCategory;
  price?: number | null;
  currency?: string;
  sku?: string;
  unit?: StockUnit | null;
  minStock?: number;
};

export type CatalogFilters = {
  q?: string;
  type?: CatalogItemType;
  active?: boolean;
  page?: number;
  pageSize?: number;
};

export type CatalogPage = {
  content: CatalogItem[];
  page: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
};

export const CATALOG_TYPE_LABELS: Record<CatalogItemType, string> = {
  PRODUCT: "Produto",
  SERVICE: "Serviço",
};

export function formatPrice(item: Pick<CatalogItem, "price" | "currency">): string {
  if (item.price == null) return "Sob consulta";
  return new Intl.NumberFormat("pt-BR", { style: "currency", currency: item.currency }).format(
    item.price,
  );
}
