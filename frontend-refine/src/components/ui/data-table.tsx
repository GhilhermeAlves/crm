"use client";

import * as React from "react";
import { ArrowUpDown, ArrowUp, ArrowDown } from "lucide-react";
import { cn } from "@/lib/utils";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { DataTableEmpty } from "./data-table/DataTableEmpty";
import { DataTablePagination } from "./data-table/DataTablePagination";
import { DataTableRowActions } from "./data-table/DataTableRowActions";
import { DataTableSkeleton } from "./data-table/DataTableSkeleton";
import { DataTableColumnsMenu } from "./data-table/DataTableColumnsMenu";
import { useColumnPreferences } from "./data-table/useColumnPreferences";

export interface ColumnDef<T> {
  id?: string;
  header: React.ReactNode;
  /** Campo do objeto ou função acessora para extrair o valor */
  accessor?: keyof T | ((row: T, index: number) => React.ReactNode);
  /** Renderizador personalizado da célula */
  cell?: (row: T, index: number) => React.ReactNode;
  className?: string;
  headerClassName?: string;
  /** Habilita ordenação para a coluna */
  sortable?: boolean;
  sortKey?: string;
  /**
   * Ordenação no navegador (listas que já vêm inteiras): valor usado para
   * comparar. Ignorado quando o grid recebe `sort` (ordenação no servidor).
   */
  sortValue?: (row: T) => string | number | Date | null | undefined;
  /** Nome mostrado no botão "Colunas" (obrigatório quando o header não é texto) */
  label?: string;
  /** false = coluna sempre visível */
  hideable?: boolean;
  /** Largura inicial em px */
  width?: number;
}

export interface TableAction<T> {
  label: string;
  icon?: React.ComponentType<{ className?: string }>;
  onClick: (row: T) => void;
  destructive?: boolean;
  disabled?: boolean | ((row: T) => boolean);
  hidden?: boolean | ((row: T) => boolean);
}

export interface DataTablePaginationProps {
  currentPage: number;
  totalPages?: number;
  totalItems?: number;
  pageSize: number;
  onPageChange: (page: number) => void;
  onPageSizeChange?: (pageSize: number) => void;
  pageSizeOptions?: number[];
}

export interface DataTableProps<T> {
  data: T[];
  columns: ColumnDef<T>[];
  keyExtractor?: (row: T, index: number) => string | number;
  isLoading?: boolean;
  loadingRows?: number;
  emptyState?: {
    icon?: React.ReactNode;
    title: string;
    description?: string;
    action?: React.ReactNode;
  };
  actions?: TableAction<T>[] | ((row: T) => TableAction<T>[]);
  onRowClick?: (row: T) => void;
  /** Classe extra por linha (ex.: destacar registros) */
  rowClassName?: (row: T) => string | undefined;
  pagination?: DataTablePaginationProps;
  sort?: {
    column: string;
    direction: "asc" | "desc";
    onSort: (column: string) => void;
  };
  className?: string;
  tableClassName?: string;
  bordered?: boolean;
  /**
   * Identificador do grid. Quando informado, mostra os botões "Colunas"/"Redefinir"
   * e permite redimensionar colunas; as escolhas ficam no navegador.
   */
  tableId?: string;
  /** Conteúdo à esquerda da barra acima do grid (filtros, busca...) */
  toolbar?: React.ReactNode;
}

function columnId<T>(col: ColumnDef<T>, idx: number): string {
  return col.id ?? (typeof col.accessor === "string" ? col.accessor : `col-${idx}`);
}

function columnLabel<T>(col: ColumnDef<T>, id: string): string {
  if (col.label) return col.label;
  if (typeof col.header === "string" && col.header.trim()) return col.header;
  return id;
}

export function DataTable<T>({
  data,
  columns,
  keyExtractor,
  isLoading = false,
  loadingRows = 5,
  emptyState,
  actions,
  onRowClick,
  pagination,
  sort,
  className,
  tableClassName,
  bordered = true,
  tableId,
  toolbar,
  rowClassName,
}: DataTableProps<T>) {
  const serverSort = sort;
  const allIds = columns.map((c, i) => columnId(c, i));
  const prefs = useColumnPreferences(tableId, allIds);
  const byId = new Map(columns.map((c, i) => [allIds[i], c]));
  const orderedIds = tableId ? prefs.order : allIds;
  const visibleIds = tableId ? orderedIds.filter((id) => !prefs.hidden.has(id)) : allIds;
  const visibleColumns = visibleIds.map((id) => ({ id, col: byId.get(id)! }));
  const resizable = Boolean(tableId);

  const [localSort, setLocalSort] = React.useState<{
    column: string;
    direction: "asc" | "desc";
  } | null>(null);
  const effectiveSort =
    sort ??
    (columns.some((c) => c.sortValue)
      ? {
          column: localSort?.column ?? "",
          direction: localSort?.direction ?? ("asc" as const),
          onSort: (column: string) =>
            setLocalSort((prev) =>
              prev?.column !== column
                ? { column, direction: "asc" }
                : prev.direction === "asc"
                  ? { column, direction: "desc" }
                  : null,
            ),
        }
      : undefined);
  const rows = React.useMemo(() => {
    if (sort || !localSort) return data;
    const col = byId.get(localSort.column);
    if (!col?.sortValue) return data;
    const get = col.sortValue;
    const factor = localSort.direction === "asc" ? 1 : -1;
    return [...data].sort((a, b) => {
      const va = get(a);
      const vb = get(b);
      if (va == null || va === "") return vb == null || vb === "" ? 0 : 1;
      if (vb == null || vb === "") return -1;
      if (typeof va === "string" && typeof vb === "string") {
        return va.localeCompare(vb, "pt-BR", { sensitivity: "base", numeric: true }) * factor;
      }
      return (Number(va) - Number(vb)) * factor;
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [data, sort, localSort, columns]);

  const startResize = (id: string, event: React.MouseEvent<HTMLDivElement>) => {
    event.preventDefault();
    event.stopPropagation();
    const th = event.currentTarget.parentElement;
    if (!th) return;
    const startX = event.clientX;
    const startWidth = th.getBoundingClientRect().width;
    const onMove = (e: MouseEvent) => prefs.setWidth(id, startWidth + e.clientX - startX);
    const onUp = () => {
      document.removeEventListener("mousemove", onMove);
      document.removeEventListener("mouseup", onUp);
      document.body.style.cursor = "";
      document.body.style.userSelect = "";
    };
    document.body.style.cursor = "col-resize";
    document.body.style.userSelect = "none";
    document.addEventListener("mousemove", onMove);
    document.addEventListener("mouseup", onUp);
  };

  const widthStyle = (id: string, col: ColumnDef<T>): React.CSSProperties | undefined => {
    const w = (resizable ? prefs.widths[id] : undefined) ?? col.width;
    return w ? { width: w, minWidth: w, maxWidth: w } : undefined;
  };
  const getKey = (row: T, index: number): string | number => {
    if (keyExtractor) return keyExtractor(row, index);
    if (row && typeof row === "object" && "id" in row) {
      return (row as { id: string | number }).id;
    }
    return index;
  };

  const hasActions = Boolean(actions);
  const totalColumns = visibleColumns.length + (hasActions ? 1 : 0);

  const renderCellContent = (col: ColumnDef<T>, row: T, index: number) => {
    if (col.cell) {
      return col.cell(row, index);
    }
    if (typeof col.accessor === "function") {
      return col.accessor(row, index);
    }
    if (col.accessor) {
      const val = row[col.accessor];
      return (val as React.ReactNode) ?? "—";
    }
    return "—";
  };

  return (
    <div className={cn("w-full space-y-3", className)}>
      {(tableId || toolbar) && (
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div className="flex flex-1 flex-wrap items-center gap-2">{toolbar}</div>
          {tableId && (
            <DataTableColumnsMenu
              columns={orderedIds.map((id) => {
                const col = byId.get(id)!;
                return { id, label: columnLabel(col, id), hideable: col.hideable !== false };
              })}
              hidden={prefs.hidden}
              isCustomized={prefs.isCustomized}
              onToggle={prefs.toggle}
              onMove={prefs.move}
              onReset={prefs.reset}
            />
          )}
        </div>
      )}
      <div
        className={cn(
          "relative overflow-hidden rounded-lg bg-card",
          bordered && "border border-border",
        )}
      >
        <div className="overflow-x-auto">
          <Table className={tableClassName}>
            <TableHeader className="bg-muted/40">
              <TableRow className="hover:bg-transparent">
                {visibleColumns.map(({ id: key, col }) => {
                  const sort = effectiveSort;
                  const canSort = col.sortable || (!serverSort && Boolean(col.sortValue));
                  const isSorted = sort && (sort.column === key || sort.column === col.sortKey);
                  return (
                    <TableHead
                      key={key}
                      style={widthStyle(key, col)}
                      className={cn(
                        "relative whitespace-nowrap text-xs font-semibold",
                        resizable && "overflow-hidden text-ellipsis",
                        col.headerClassName,
                      )}
                    >
                      {canSort && sort ? (
                        <Button
                          variant="ghost"
                          size="sm"
                          className="-ml-3 h-8 gap-1.5 text-xs font-semibold text-muted-foreground hover:text-foreground"
                          onClick={() => sort.onSort(col.sortKey ?? String(key))}
                        >
                          {col.header}
                          {isSorted ? (
                            sort.direction === "asc" ? (
                              <ArrowUp className="h-3.5 w-3.5 text-foreground" />
                            ) : (
                              <ArrowDown className="h-3.5 w-3.5 text-foreground" />
                            )
                          ) : (
                            <ArrowUpDown className="h-3.5 w-3.5 opacity-40" />
                          )}
                        </Button>
                      ) : (
                        col.header
                      )}
                      {resizable && (
                        <div
                          role="separator"
                          aria-orientation="vertical"
                          aria-label={`Redimensionar ${columnLabel(col, key)}`}
                          onMouseDown={(e) => startResize(key, e)}
                          onClick={(e) => e.stopPropagation()}
                          className="absolute right-0 top-0 h-full w-1.5 cursor-col-resize select-none hover:bg-primary/40"
                        />
                      )}
                    </TableHead>
                  );
                })}
                {hasActions && (
                  <TableHead className="w-[60px] text-right text-xs font-semibold">Ações</TableHead>
                )}
              </TableRow>
            </TableHeader>

            <TableBody>
              {isLoading ? (
                <DataTableSkeleton
                  columns={visibleColumns.length}
                  loadingRows={loadingRows}
                  hasActions={hasActions}
                />
              ) : rows.length === 0 ? (
                <DataTableEmpty totalColumns={totalColumns} emptyState={emptyState} />
              ) : (
                rows.map((row, rowIdx) => {
                  const rowActions = typeof actions === "function" ? actions(row) : (actions ?? []);

                  return (
                    <TableRow
                      key={getKey(row, rowIdx)}
                      onClick={() => onRowClick && onRowClick(row)}
                      className={cn(
                        onRowClick && "cursor-pointer hover:bg-muted/40",
                        "transition-colors",
                        rowClassName?.(row),
                      )}
                    >
                      {visibleColumns.map(({ id: key, col }) => (
                        <TableCell
                          key={key}
                          style={widthStyle(key, col)}
                          className={cn(
                            "p-3 text-sm",
                            resizable && prefs.widths[key] && "overflow-hidden text-ellipsis",
                            col.className,
                          )}
                        >
                          {renderCellContent(col, row, rowIdx)}
                        </TableCell>
                      ))}

                      {hasActions && <DataTableRowActions row={row} actions={rowActions} />}
                    </TableRow>
                  );
                })
              )}
            </TableBody>
          </Table>
        </div>
      </div>

      {pagination && <DataTablePagination pagination={pagination} isLoading={isLoading} />}
    </div>
  );
}
