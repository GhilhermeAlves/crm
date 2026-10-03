export type CatalogItemType = "PRODUCT" | "SERVICE";

export type CatalogItem = {
  id: string;
  companyId: string;
  type: CatalogItemType;
  name: string;
  description: string | null;
  category: string | null;
  price: number | null;
  currency: string;
  sku: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type CatalogItemRequest = {
  type: CatalogItemType;
  name: string;
  description?: string;
  category?: string;
  price?: number | null;
  currency?: string;
  sku?: string;
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
