"use client";

import { ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type { DataTablePaginationProps } from "../data-table";

type DataTablePaginationOwnProps = {
  pagination: DataTablePaginationProps;
  isLoading: boolean;
};

export function DataTablePagination({ pagination, isLoading }: DataTablePaginationOwnProps) {
  return (
    <div className="flex flex-col gap-3 px-1 py-2 text-xs text-muted-foreground sm:flex-row sm:items-center sm:justify-between">
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
            de <span className="font-medium text-foreground">{pagination.totalItems}</span>{" "}
            registro(s)
          </span>
        )}

        {pagination.onPageSizeChange && (
          <div className="ml-4 flex items-center gap-1.5">
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
            Página <span className="font-medium text-foreground">{pagination.currentPage}</span> de{" "}
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
  );
}
