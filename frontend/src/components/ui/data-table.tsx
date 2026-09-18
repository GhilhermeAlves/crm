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
  pagination?: DataTablePaginationProps;
  sort?: {
    column: string;
    direction: "asc" | "desc";
    onSort: (column: string) => void;
  };
  className?: string;
  tableClassName?: string;
  bordered?: boolean;
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
}: DataTableProps<T>) {
  const getKey = (row: T, index: number): string | number => {
    if (keyExtractor) return keyExtractor(row, index);
    if (row && typeof row === "object" && "id" in row) {
      return (row as { id: string | number }).id;
    }
    return index;
  };

  const hasActions = Boolean(actions);
  const totalColumns = columns.length + (hasActions ? 1 : 0);

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
                {columns.map((col, idx) => {
                  const key = col.id ?? (typeof col.accessor === "string" ? col.accessor : idx);
                  const isSorted = sort && (sort.column === key || sort.column === col.sortKey);
                  return (
                    <TableHead
                      key={key}
                      className={cn("whitespace-nowrap text-xs font-semibold", col.headerClassName)}
                    >
                      {col.sortable && sort ? (
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
                  columns={columns.length}
                  loadingRows={loadingRows}
                  hasActions={hasActions}
                />
              ) : data.length === 0 ? (
                <DataTableEmpty totalColumns={totalColumns} emptyState={emptyState} />
              ) : (
                data.map((row, rowIdx) => {
                  const rowActions = typeof actions === "function" ? actions(row) : (actions ?? []);

                  return (
                    <TableRow
                      key={getKey(row, rowIdx)}
                      onClick={() => onRowClick && onRowClick(row)}
                      className={cn(
                        onRowClick && "cursor-pointer hover:bg-muted/40",
                        "transition-colors",
                      )}
                    >
                      {columns.map((col, cIdx) => (
                        <TableCell
                          key={col.id ?? (typeof col.accessor === "string" ? col.accessor : cIdx)}
                          className={cn("p-3 text-sm", col.className)}
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
