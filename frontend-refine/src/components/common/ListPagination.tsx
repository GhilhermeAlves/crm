"use client";

import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { PAGE_SIZE_OPTIONS } from "@/types/page";

type ListPaginationProps = {
  /** Página atual, 0-based (como no Spring). */
  page: number;
  pageSize: number;
  totalPages: number;
  totalElements: number;
  onPageChange: (page: number) => void;
  /** Ao trocar o tamanho, a tela deve voltar para a primeira página. */
  onPageSizeChange: (pageSize: number) => void;
  disabled?: boolean;
};

/**
 * Rodapé de paginação das listagens: "Mostrando X a Y de N", seletor de
 * linhas por página e Anterior/Próxima.
 */
export function ListPagination({
  page,
  pageSize,
  totalPages,
  totalElements,
  onPageChange,
  onPageSizeChange,
  disabled = false,
}: ListPaginationProps) {
  if (totalElements <= 0) return null;

  const pages = Math.max(totalPages, 1);
  const from = page * pageSize + 1;
  const to = Math.min((page + 1) * pageSize, totalElements);

  return (
    <div className="flex flex-col gap-3 text-sm text-muted-foreground sm:flex-row sm:items-center sm:justify-between">
      <div className="flex flex-wrap items-center gap-x-4 gap-y-2">
        <span>
          Mostrando <span className="font-medium text-foreground">{from}</span> a{" "}
          <span className="font-medium text-foreground">{to}</span> de{" "}
          <span className="font-medium text-foreground">{totalElements}</span>
        </span>
        <div className="flex items-center gap-2">
          <span>Linhas por página</span>
          <Select
            value={String(pageSize)}
            onValueChange={(value) => onPageSizeChange(Number(value))}
            disabled={disabled}
          >
            <SelectTrigger className="h-8 w-[72px]" aria-label="Linhas por página">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {PAGE_SIZE_OPTIONS.map((option) => (
                <SelectItem key={option} value={String(option)}>
                  {option}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      <div className="flex items-center gap-2 self-end sm:self-auto">
        <span>
          Página {page + 1} de {pages}
        </span>
        <Button
          variant="outline"
          size="sm"
          disabled={disabled || page <= 0}
          onClick={() => onPageChange(page - 1)}
        >
          <ChevronLeft className="h-4 w-4" />
          Anterior
        </Button>
        <Button
          variant="outline"
          size="sm"
          disabled={disabled || page + 1 >= pages}
          onClick={() => onPageChange(page + 1)}
        >
          Próxima
          <ChevronRight className="h-4 w-4" />
        </Button>
      </div>
    </div>
  );
}
