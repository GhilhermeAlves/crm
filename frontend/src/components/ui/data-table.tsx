"use client";

import * as React from "react";
import {
  MoreHorizontal,
  ChevronLeft,
  ChevronRight,
  ChevronsLeft,
  ChevronsRight,
  ArrowUpDown,
  ArrowUp,
  ArrowDown,
  Inbox,
} from "lucide-react";
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
import { Skeleton } from "@/components/ui/skeleton";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { EmptyState } from "@/components/common/EmptyState";

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
                  <TableHead className="w-[60px] text-right font-semibold text-xs">Ações</TableHead>
                )}
              </TableRow>
            </TableHeader>

            <TableBody>
              {isLoading ? (
                Array.from({ length: loadingRows }).map((_, rIdx) => (
                  <TableRow key={`skeleton-row-${rIdx}`} className="hover:bg-transparent">
                    {columns.map((col, cIdx) => (
                      <TableCell key={`skeleton-cell-${cIdx}`} className="p-3">
                        <Skeleton className="h-5 w-full max-w-[140px] rounded" />
                      </TableCell>
                    ))}
                    {hasActions && (
                      <TableCell className="p-3 text-right">
                        <Skeleton className="ml-auto h-7 w-7 rounded-md" />
                      </TableCell>
                    )}
                  </TableRow>
                ))
              ) : data.length === 0 ? (
                <TableRow className="hover:bg-transparent">
                  <TableCell colSpan={totalColumns} className="h-48 text-center p-6">
                    <EmptyState
                      icon={emptyState?.icon || <Inbox className="h-8 w-8 text-muted-foreground" />}
                      title={emptyState?.title || "Nenhum dado encontrado"}
                      description={
                        emptyState?.description || "Não há registros correspondentes cadastrados."
                      }
                      action={emptyState?.action}
                    />
                  </TableCell>
                </TableRow>
              ) : (
                data.map((row, rowIdx) => {
                  const rowActions =
                    typeof actions === "function" ? actions(row) : (actions ?? []);
                  const visibleActions = rowActions.filter((a) =>
                    typeof a.hidden === "function" ? !a.hidden(row) : !a.hidden,
                  );

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

                      {hasActions && (
                        <TableCell
                          className="p-3 text-right"
                          onClick={(e) => e.stopPropagation()}
                        >
                          {visibleActions.length > 0 && (
                            <DropdownMenu>
                              <DropdownMenuTrigger asChild>
                                <Button
                                  variant="ghost"
                                  size="icon"
                                  className="h-8 w-8 text-muted-foreground hover:text-foreground"
                                  aria-label="Abrir menu de ações"
                                >
                                  <MoreHorizontal className="h-4 w-4" />
                                </Button>
                              </DropdownMenuTrigger>
                              <DropdownMenuContent align="end" className="w-44">
                                {visibleActions.map((act, aIdx) => {
                                  const isDisabled =
                                    typeof act.disabled === "function"
                                      ? act.disabled(row)
                                      : Boolean(act.disabled);
                                  const Icon = act.icon;

                                  return (
                                    <React.Fragment key={`${act.label}-${aIdx}`}>
                                      {act.destructive && aIdx > 0 && <DropdownMenuSeparator />}
                                      <DropdownMenuItem
                                        onClick={() => act.onClick(row)}
                                        disabled={isDisabled}
                                        className={cn(
                                          act.destructive &&
                                            "text-destructive focus:text-destructive",
                                        )}
                                      >
                                        {Icon && <Icon className="mr-2 h-4 w-4" />}
                                        <span>{act.label}</span>
                                      </DropdownMenuItem>
                                    </React.Fragment>
                                  );
                                })}
                              </DropdownMenuContent>
                            </DropdownMenu>
                          )}
                        </TableCell>
                      )}
                    </TableRow>
                  );
                })
              )}
            </TableBody>
          </Table>
        </div>
      </div>

      {/* ======================================================================= */}
      {/* BARRA DE PAGINAÇÃO                                                      */}
      {/* ======================================================================= */}
      {pagination && (
        <div className="flex flex-col gap-3 px-1 py-2 sm:flex-row sm:items-center sm:justify-between text-xs text-muted-foreground">
          <div className="flex items-center gap-2">
            {pagination.totalItems !== undefined && (
              <span>
                Mostrando{" "}
                <span className="font-medium text-foreground">
                  {Math.min(
                    (pagination.currentPage - 1) * pagination.pageSize + 1,
                    pagination.totalItems,
                  )}
                </span>{" "}
                a{" "}
                <span className="font-medium text-foreground">
                  {Math.min(pagination.currentPage * pagination.pageSize, pagination.totalItems)}
                </span>{" "}
                de{" "}
                <span className="font-medium text-foreground">{pagination.totalItems}</span>{" "}
                registro(s)
              </span>
            )}

            {pagination.onPageSizeChange && (
              <div className="flex items-center gap-1.5 ml-4">
                <span>Itens por pág:</span>
                <Select
                  value={String(pagination.pageSize)}
                  onValueChange={(v) => pagination.onPageSizeChange?.(Number(v))}
                >
                  <SelectTrigger className="h-7 w-16 text-xs">
                    <SelectValue placeholder={String(pagination.pageSize)} />
                  </SelectTrigger>
                  <SelectContent>
                    {(pagination.pageSizeOptions ?? [10, 20, 50, 100]).map((opt) => (
                      <SelectItem key={opt} value={String(opt)} className="text-xs">
                        {opt}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            )}
          </div>

          <div className="flex items-center gap-1 self-end sm:self-auto">
            {pagination.totalPages && (
              <span className="mr-2">
                Página{" "}
                <span className="font-medium text-foreground">{pagination.currentPage}</span> de{" "}
                <span className="font-medium text-foreground">{pagination.totalPages}</span>
              </span>
            )}

            <Button
              variant="outline"
              size="icon"
              className="h-7 w-7"
              onClick={() => pagination.onPageChange(1)}
              disabled={pagination.currentPage <= 1 || isLoading}
              title="Primeira página"
            >
              <ChevronsLeft className="h-3.5 w-3.5" />
            </Button>
            <Button
              variant="outline"
              size="icon"
              className="h-7 w-7"
              onClick={() => pagination.onPageChange(pagination.currentPage - 1)}
              disabled={pagination.currentPage <= 1 || isLoading}
              title="Página anterior"
            >
              <ChevronLeft className="h-3.5 w-3.5" />
            </Button>

            <Button
              variant="outline"
              size="icon"
              className="h-7 w-7"
              onClick={() => pagination.onPageChange(pagination.currentPage + 1)}
              disabled={
                Boolean(pagination.totalPages && pagination.currentPage >= pagination.totalPages) ||
                isLoading
              }
              title="Próxima página"
            >
              <ChevronRight className="h-3.5 w-3.5" />
            </Button>
            {pagination.totalPages && (
              <Button
                variant="outline"
                size="icon"
                className="h-7 w-7"
                onClick={() => pagination.onPageChange(pagination.totalPages!)}
                disabled={pagination.currentPage >= pagination.totalPages || isLoading}
                title="Última página"
              >
                <ChevronsRight className="h-3.5 w-3.5" />
              </Button>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
