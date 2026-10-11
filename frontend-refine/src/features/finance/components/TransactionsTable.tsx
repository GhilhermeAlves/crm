"use client";

import { format, parseISO } from "date-fns";
import { MoreHorizontal } from "lucide-react";

import { ErrorCard } from "@/components/common/ErrorCard";
import { Button } from "@/components/ui/button";
import { DataTable, type ColumnDef } from "@/components/ui/data-table";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { StatusBadge } from "@/components/ui/status-badge";

import type { FinancialTransaction } from "../types/finance.types";
import { PAYMENT_METHOD_LABELS, TRANSACTION_CATEGORY_LABELS } from "../types/finance.types";
import { centsToBRL } from "../utils/money";
import { TransactionsEmptyState } from "./TransactionsEmptyState";

type TransactionsTableProps = {
  transactions: FinancialTransaction[];
  isLoading?: boolean;
  isError?: boolean;
  onRetry?: () => void;
  page: number;
  totalPages: number;
  totalElements: number;
  pageSize: number;
  onPageChange: (page: number) => void;
  onNew: () => void;
  onImport: () => void;
};

const formatDueDate = (dueDate: string): string => {
  const parsed = parseISO(dueDate);
  return Number.isNaN(parsed.getTime()) ? dueDate : format(parsed, "dd/MM/yyyy");
};

const statusBadge = (transaction: FinancialTransaction) => {
  if (transaction.status === "CANCELLED") {
    return (
      <StatusBadge intent="neutral" size="sm">
        Cancelado
      </StatusBadge>
    );
  }
  if (transaction.status === "PAID") {
    return (
      <StatusBadge intent="success" size="sm">
        Pago
      </StatusBadge>
    );
  }
  if (transaction.overdue) {
    return (
      <StatusBadge intent="danger" size="sm">
        Atrasado
      </StatusBadge>
    );
  }
  return (
    <StatusBadge intent="warning" size="sm">
      Pendente
    </StatusBadge>
  );
};

export function TransactionsTable({
  transactions,
  isLoading = false,
  isError = false,
  onRetry,
  page,
  totalPages,
  totalElements,
  pageSize,
  onPageChange,
  onNew,
  onImport,
}: TransactionsTableProps) {
  if (isError) {
    return (
      <ErrorCard
        message="Não foi possível carregar os lançamentos."
        onRetry={onRetry}
        className="border-border"
      />
    );
  }

  if (!isLoading && transactions.length === 0) {
    return <TransactionsEmptyState onNew={onNew} onImport={onImport} />;
  }

  const columns: ColumnDef<FinancialTransaction>[] = [
    {
      header: "Vencimento",
      className: "whitespace-nowrap",
      cell: (transaction) => formatDueDate(transaction.dueDate),
    },
    {
      header: "Descrição",
      cell: (transaction) => (
        <div className="min-w-0">
          <span className="text-sm font-medium">{transaction.description}</span>
          {transaction.contactName && (
            <p className="truncate text-xs text-muted-foreground">{transaction.contactName}</p>
          )}
        </div>
      ),
    },
    {
      header: "Categoria",
      className: "whitespace-nowrap",
      cell: (transaction) => TRANSACTION_CATEGORY_LABELS[transaction.category],
    },
    {
      header: "Forma de pagamento",
      className: "whitespace-nowrap",
      cell: (transaction) => PAYMENT_METHOD_LABELS[transaction.paymentMethod],
    },
    {
      header: "Profissional",
      className: "whitespace-nowrap",
      cell: (transaction) => transaction.professionalName ?? "—",
    },
    {
      header: "Parcela",
      className: "whitespace-nowrap",
      cell: (transaction) =>
        transaction.installmentNumber && transaction.installmentTotal
          ? `${transaction.installmentNumber}/${transaction.installmentTotal}`
          : "—",
    },
    {
      header: "Valor",
      headerClassName: "text-right",
      className: "whitespace-nowrap text-right font-medium tabular-nums",
      cell: (transaction) => {
        const isIncome = transaction.type === "INCOME";
        return (
          <span
            className={
              isIncome ? "text-emerald-600 dark:text-emerald-400" : "text-red-600 dark:text-red-400"
            }
          >
            {isIncome ? "+" : "−"}
            {centsToBRL(transaction.amountCents)}
          </span>
        );
      },
    },
    {
      header: "Status",
      className: "whitespace-nowrap",
      cell: (transaction) => statusBadge(transaction),
    },
    {
      header: <span className="sr-only">Ações</span>,
      className: "w-[52px] text-right",
      cell: (transaction) => (
        // etapa 6: abre o menu de ações da linha (editar, marcar como pago, cancelar, excluir)
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button
              variant="ghost"
              size="icon"
              className="h-8 w-8"
              aria-label={`Ações do lançamento ${transaction.description}`}
            >
              <MoreHorizontal className="h-4 w-4" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" />
        </DropdownMenu>
      ),
    },
  ];

  return (
    <DataTable
      data={transactions}
      columns={columns}
      keyExtractor={(transaction) => transaction.id}
      isLoading={isLoading}
      loadingRows={5}
      pagination={{
        currentPage: page,
        totalPages,
        totalItems: totalElements,
        pageSize,
        onPageChange,
      }}
    />
  );
}
