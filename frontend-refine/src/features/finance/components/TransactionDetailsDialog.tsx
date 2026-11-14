"use client";

import { useState } from "react";
import Link from "next/link";
import { format, parseISO } from "date-fns";
import { Ban, CheckCircle2, FileText, Pencil } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog";
import { StatusBadge } from "@/components/ui/status-badge";
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip";
import { usePermission } from "@/features/identity/auth/hooks/useAuthorization";
import { ROUTES } from "@/lib/constants";
import { cn } from "@/lib/utils";

import type { FinancialTransaction } from "../types/finance.types";
import { PAYMENT_METHOD_LABELS, TRANSACTION_CATEGORY_LABELS } from "../types/finance.types";
import { centsToBRL } from "../utils/money";
import { TransactionActionDialogs, type TransactionAction } from "./TransactionRowActions";

type TransactionDetailsDialogProps = {
  companyId: string | null;
  transaction: FinancialTransaction | null;
  onOpenChange: (open: boolean) => void;
  onEdit: (transaction: FinancialTransaction) => void;
};

const formatDate = (value: string | null): string => {
  if (!value) return "—";
  const parsed = parseISO(value);
  return Number.isNaN(parsed.getTime()) ? value : format(parsed, "dd/MM/yyyy");
};

function StatusLabel({ transaction }: { transaction: FinancialTransaction }) {
  if (transaction.status === "CANCELLED")
    return <StatusBadge intent="neutral">Cancelado</StatusBadge>;
  if (transaction.status === "PAID") return <StatusBadge intent="success">Pago</StatusBadge>;
  if (transaction.overdue) return <StatusBadge intent="danger">Atrasado</StatusBadge>;
  return <StatusBadge intent="warning">Pendente</StatusBadge>;
}

function IconAction({
  label,
  onClick,
  children,
}: {
  label: string;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <TooltipProvider delayDuration={200}>
      <Tooltip>
        <TooltipTrigger asChild>
          <Button
            variant="ghost"
            size="icon"
            className="h-8 w-8"
            aria-label={label}
            onClick={onClick}
          >
            {children}
          </Button>
        </TooltipTrigger>
        <TooltipContent>{label}</TooltipContent>
      </Tooltip>
    </TooltipProvider>
  );
}

/** Detalhes do lançamento, aberto ao clicar na descrição (paciente, orçamento e ações). */
export function TransactionDetailsDialog({
  companyId,
  transaction,
  onOpenChange,
  onEdit,
}: TransactionDetailsDialogProps) {
  const can = usePermission();
  const canManage = can("finance:manage");
  const [action, setAction] = useState<TransactionAction>(null);

  if (!transaction) return null;

  const isIncome = transaction.type === "INCOME";
  const canPay = canManage && transaction.status === "PENDING";
  const canCancel = canManage && transaction.status !== "CANCELLED";
  const patientHref = transaction.contactId ? `${ROUTES.PATIENTS}/${transaction.contactId}` : null;
  const budgetHref =
    transaction.budgetId && transaction.contactId
      ? `${ROUTES.PATIENTS}/${transaction.contactId}/orcamentos`
      : null;

  return (
    <>
      <Dialog open={action === null} onOpenChange={onOpenChange}>
        <DialogContent className="sm:max-w-md">
          <div className="flex items-start gap-3 pr-8">
            <div
              className={cn(
                "min-w-0 flex-1 border-l-4 pl-3",
                isIncome ? "border-emerald-600" : "border-red-600",
              )}
            >
              <p className="text-xs text-muted-foreground">{isIncome ? "Receita" : "Despesa"}</p>
              <DialogTitle className="text-base leading-snug">
                {transaction.description}
              </DialogTitle>
              <DialogDescription className="sr-only">Detalhes do lançamento</DialogDescription>
            </div>
            {canManage && (
              <div className="flex shrink-0 items-center">
                {canCancel && (
                  <IconAction label="Cancelar lançamento" onClick={() => setAction("cancel")}>
                    <Ban className="h-4 w-4" />
                  </IconAction>
                )}
                {canPay && (
                  <IconAction label="Marcar como pago" onClick={() => setAction("pay")}>
                    <CheckCircle2 className="h-4 w-4" />
                  </IconAction>
                )}
                <IconAction
                  label="Editar"
                  onClick={() => {
                    onOpenChange(false);
                    onEdit(transaction);
                  }}
                >
                  <Pencil className="h-4 w-4" />
                </IconAction>
              </div>
            )}
          </div>

          {transaction.contactName && (
            <div className="flex items-center gap-3">
              <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-primary/10 text-sm font-semibold text-primary">
                {transaction.contactName.charAt(0).toUpperCase()}
              </div>
              {patientHref ? (
                <Link
                  href={patientHref}
                  className="text-sm font-medium text-primary underline-offset-4 hover:underline"
                >
                  {transaction.contactName}
                </Link>
              ) : (
                <span className="text-sm font-medium">{transaction.contactName}</span>
              )}
            </div>
          )}

          {budgetHref && (
            <Link
              href={budgetHref}
              className="flex w-fit items-center gap-1.5 text-sm text-primary underline-offset-4 hover:underline"
            >
              <FileText className="h-4 w-4" />
              {transaction.budgetTitle ? `Orçamento: ${transaction.budgetTitle}` : "Ver orçamento"}
            </Link>
          )}

          <dl className="grid grid-cols-2 gap-x-4 gap-y-3 text-sm">
            <div>
              <dt className="text-xs text-muted-foreground">Data do vencimento</dt>
              <dd>{formatDate(transaction.dueDate)}</dd>
            </div>
            <div>
              <dt className="text-xs text-muted-foreground">Categoria</dt>
              <dd>{TRANSACTION_CATEGORY_LABELS[transaction.category]}</dd>
            </div>
            <div>
              <dt className="text-xs text-muted-foreground">Forma de pagamento</dt>
              <dd>{PAYMENT_METHOD_LABELS[transaction.paymentMethod]}</dd>
            </div>
            {transaction.installmentNumber && transaction.installmentTotal ? (
              <div>
                <dt className="text-xs text-muted-foreground">Parcela</dt>
                <dd>
                  {transaction.installmentNumber}/{transaction.installmentTotal}
                </dd>
              </div>
            ) : null}
            {transaction.paidAt && (
              <div>
                <dt className="text-xs text-muted-foreground">Data do pagamento</dt>
                <dd>{formatDate(transaction.paidAt)}</dd>
              </div>
            )}
            {transaction.professionalName && (
              <div>
                <dt className="text-xs text-muted-foreground">Profissional</dt>
                <dd>{transaction.professionalName}</dd>
              </div>
            )}
          </dl>

          <div className="flex items-center justify-between rounded-md border px-3 py-3">
            <span className="text-sm text-muted-foreground">Valor</span>
            <span
              className={cn(
                "font-semibold tabular-nums",
                isIncome
                  ? "text-emerald-600 dark:text-emerald-400"
                  : "text-red-600 dark:text-red-400",
              )}
            >
              {isIncome ? "+" : "−"}
              {centsToBRL(transaction.amountCents)}
            </span>
          </div>

          <div className="space-y-1">
            <p className="text-xs text-muted-foreground">Status</p>
            <StatusLabel transaction={transaction} />
          </div>

          {transaction.notes && (
            <div className="space-y-1">
              <p className="text-xs text-muted-foreground">Observações</p>
              <p className="whitespace-pre-wrap text-sm">{transaction.notes}</p>
            </div>
          )}

          {canManage && (
            <Button variant="outline" className="w-full" onClick={() => setAction("delete")}>
              Excluir lançamento
            </Button>
          )}
        </DialogContent>
      </Dialog>

      <TransactionActionDialogs
        companyId={companyId}
        transaction={transaction}
        action={action}
        onActionChange={setAction}
        onDone={() => onOpenChange(false)}
      />
    </>
  );
}
