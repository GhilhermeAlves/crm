/**
 * Paginação compartilhada (padrão do data-provider do Portal Praia).
 *
 * O backend devolve `Page<T>` em formatos diferentes conforme o endpoint
 * (DTO com `page` numérico, PageImpl do Spring com `number/size`, VIA_DTO com
 * `page: {...}` aninhado ou array simples). `normalizePage` converte tudo para
 * `PageResponse<T>`, com `page` 0-based como no Spring.
 */
export type PageResponse<T> = {
  content: T[];
  page: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
};

type PageMeta = {
  number?: number;
  size?: number;
  totalElements?: number;
  totalPages?: number;
};

type RawPage<T> = PageMeta & {
  content?: T[];
  page?: number | PageMeta;
  pageSize?: number;
};

export function normalizePage<T>(raw: RawPage<T> | T[] | null | undefined): PageResponse<T> {
  if (Array.isArray(raw)) {
    return {
      content: raw,
      page: 0,
      pageSize: raw.length,
      totalElements: raw.length,
      totalPages: raw.length ? 1 : 0,
    };
  }
  const content = raw?.content ?? [];
  const meta: PageMeta = typeof raw?.page === "object" && raw.page ? raw.page : (raw ?? {});
  const page = typeof raw?.page === "number" ? raw.page : (meta.number ?? 0);
  const pageSize = raw?.pageSize ?? meta.size ?? content.length;
  const totalElements = meta.totalElements ?? content.length;
  const totalPages =
    meta.totalPages ??
    (pageSize > 0 ? Math.ceil(totalElements / pageSize) : content.length ? 1 : 0);
  return { content, page, pageSize, totalElements, totalPages };
}

export type PageParams = {
  /** 0-based, como no Spring. */
  page?: number;
  size?: number;
  /** Ex.: [{ field: "name", order: "asc" }] → sort=name,asc */
  sort?: { field: string; order: "asc" | "desc" }[];
};

/** Monta params no formato Spring Data (`page`, `size`, `sort=campo,ordem`). */
export function toSpringParams({ page, size, sort }: PageParams): URLSearchParams {
  const params = new URLSearchParams();
  if (page !== undefined) params.set("page", String(page));
  if (size !== undefined) params.set("size", String(size));
  sort?.forEach(({ field, order }) => params.append("sort", `${field},${order}`));
  return params;
}

/** Linhas por página padrão das listagens. */
export const DEFAULT_PAGE_SIZE = 10;

/** Opções do seletor "Linhas por página". */
export const PAGE_SIZE_OPTIONS = [10, 20, 50, 100] as const;
