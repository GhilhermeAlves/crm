"use client";

import { useEffect, useState } from "react";
import { format, parseISO } from "date-fns";
import { Ban, CalendarIcon, CheckCircle2, MoreHorizontal, Pencil, Trash2 } from "lucide-react";
import { toast } from "sonner";

import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { usePermission } from "@/features/identity/auth/hooks/useAuthorization";

import { useCancelTransaction, useDeleteTransaction, usePayTransaction } from "../hooks/useFinance";
import type { FinancialTransaction } from "../types/finance.types";

type TransactionRowActionsProps = {
  companyId: string | null;
  transaction: FinancialTransaction;
  onEdit: (transaction: FinancialTransaction) => void;
};

export function TransactionRowActions({
  companyId,
  transaction,
  onEdit,
}: TransactionRowActionsProps) {
  const can = usePermission();
  const canManage = can("finance:manage");

  const [payOpen, setPayOpen] = useState(false);
  const [paidAt, setPaidAt] = useState(() => format(new Date(), "yyyy-MM-dd"));
  const [cancelOpen, setCancelOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);

  const payTransaction = usePayTransaction(companyId);
  const cancelTransaction = useCancelTransaction(companyId);
  const deleteTransaction = useDeleteTransaction(companyId);

  useEffect(() => {
    if (payOpen) setPaidAt(format(new Date(), "yyyy-MM-dd"));
  }, [payOpen]);

  if (!canManage) return null;

  const canPay = transaction.status === "PENDING";
  const canCancel = transaction.status !== "CANCELLED";

  return (
    <>
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
        <DropdownMenuContent align="end">
          {canPay && (
            <DropdownMenuItem onSelect={() => setPayOpen(true)}>
              <CheckCircle2 />
              Marcar como pago
            </DropdownMenuItem>
          )}
          <DropdownMenuItem onSelect={() => onEdit(transaction)}>
            <Pencil />
            Editar
          </DropdownMenuItem>
          {canCancel && (
            <DropdownMenuItem onSelect={() => setCancelOpen(true)}>
              <Ban />
              Cancelar lançamento
            </DropdownMenuItem>
          )}
          <DropdownMenuSeparator />
          <DropdownMenuItem
            onSelect={() => setDeleteOpen(true)}
            className="text-destructive focus:text-destructive"
          >
            <Trash2 />
            Excluir
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>

      <Dialog open={payOpen} onOpenChange={setPayOpen}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>Marcar como pago</DialogTitle>
          </DialogHeader>
          <div className="space-y-2">
            <label className="text-sm font-medium">Data do pagamento</label>
            <Popover>
              <PopoverTrigger asChild>
                <Button
                  type="button"
                  variant="outline"
                  className="w-full justify-start text-left font-normal"
                >
                  <CalendarIcon className="mr-2 h-4 w-4" />
                  {format(parseISO(paidAt), "dd/MM/yyyy")}
                </Button>
              </PopoverTrigger>
              <PopoverContent align="start" className="w-auto p-0">
                <Calendar
                  mode="single"
                  selected={parseISO(paidAt)}
                  disabled={{ after: new Date() }}
                  onSelect={(day) => day && setPaidAt(format(day, "yyyy-MM-dd"))}
                />
              </PopoverContent>
            </Popover>
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => setPayOpen(false)}>
              Cancelar
            </Button>
            <Button
              type="button"
              disabled={payTransaction.isPending}
              onClick={() =>
                payTransaction.mutate(
                  { id: transaction.id, paidAt },
                  {
                    onSuccess: () => {
                      toast.success("Lançamento salvo");
                      setPayOpen(false);
                    },
                  },
                )
              }
            >
              {payTransaction.isPending ? "Salvando…" : "Confirmar"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <AlertDialog open={cancelOpen} onOpenChange={setCancelOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Cancelar lançamento</AlertDialogTitle>
            <AlertDialogDescription>
              O lançamento <strong>{transaction.description}</strong> será marcado como cancelado.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Voltar</AlertDialogCancel>
            <AlertDialogAction
              onClick={(event) => {
                event.preventDefault();
                cancelTransaction.mutate(transaction.id, {
                  onSuccess: () => {
                    toast.success("Lançamento cancelado");
                    setCancelOpen(false);
                  },
                });
              }}
              disabled={cancelTransaction.isPending}
            >
              {cancelTransaction.isPending ? "Cancelando…" : "Cancelar lançamento"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <AlertDialog open={deleteOpen} onOpenChange={setDeleteOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Excluir lançamento</AlertDialogTitle>
            <AlertDialogDescription>
              Esta ação remove permanentemente o lançamento{" "}
              <strong>{transaction.description}</strong>.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Voltar</AlertDialogCancel>
            <AlertDialogAction
              onClick={(event) => {
                event.preventDefault();
                deleteTransaction.mutate(transaction.id, {
                  onSuccess: () => {
                    toast.success("Lançamento excluído");
                    setDeleteOpen(false);
                  },
                });
              }}
              disabled={deleteTransaction.isPending}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              {deleteTransaction.isPending ? "Excluindo…" : "Excluir"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
