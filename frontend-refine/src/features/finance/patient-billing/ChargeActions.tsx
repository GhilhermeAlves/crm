"use client";

import { Copy, Link2, MessageCircle, RotateCcw } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { StatusBadge } from "@/components/ui/status-badge";
import type { FinancialTransaction } from "@/features/finance/types/finance.types";
import { useChargeTransaction, useResendWhatsApp } from "./hooks";
import type { ChargeView } from "./service";

type Props = {
  companyId: string;
  transaction: FinancialTransaction;
  charge: ChargeView | undefined;
};

const LABELS: Record<ChargeView["status"], string> = {
  PENDING: "Link enviado",
  RECEIVED: "Pago pelo link",
  RECEIVED_IN_CASH: "Baixado na clínica",
  CANCELLED: "Cobrança cancelada",
  FAILED: "Falhou",
};

/** Coluna "Asaas" do Financeiro: estado da cobrança e ações (cobrar, copiar link, reenviar). */
export function ChargeActions({ companyId, transaction, charge }: Props) {
  const create = useChargeTransaction(companyId);
  const resend = useResendWhatsApp(companyId);
  const busy = create.isPending || resend.isPending;
  const chargeable =
    transaction.type === "INCOME" &&
    transaction.status === "PENDING" &&
    Boolean(transaction.contactId);

  const copy = async (url: string) => {
    try {
      await navigator.clipboard.writeText(url);
      toast.success("Link copiado");
    } catch {
      toast.error("Não foi possível copiar o link.");
    }
  };

  if (charge?.status === "PENDING" && charge.invoiceUrl) {
    const url = charge.invoiceUrl;
    return (
      <div className="flex items-center gap-1">
        <StatusBadge intent="info" size="sm">
          {LABELS.PENDING}
        </StatusBadge>
        <Button
          variant="ghost"
          size="icon"
          className="h-7 w-7"
          aria-label="Copiar link de pagamento"
          title="Copiar link de pagamento"
          onClick={() => copy(url)}
        >
          <Copy className="h-3.5 w-3.5" />
        </Button>
        <Button
          variant="ghost"
          size="icon"
          className="h-7 w-7"
          aria-label="Reenviar pelo WhatsApp"
          title="Reenviar pelo WhatsApp"
          disabled={busy}
          onClick={() => resend.mutate(transaction.id)}
        >
          <MessageCircle className="h-3.5 w-3.5" />
        </Button>
      </div>
    );
  }

  if (charge && charge.status !== "FAILED" && charge.status !== "CANCELLED") {
    return (
      <StatusBadge intent="success" size="sm">
        {LABELS[charge.status]}
      </StatusBadge>
    );
  }

  if (!chargeable) return <span className="text-muted-foreground">—</span>;

  return (
    <Button
      variant="outline"
      size="sm"
      className="h-7 gap-1 text-xs"
      disabled={busy}
      title={charge?.status === "FAILED" ? (charge.error ?? undefined) : undefined}
      onClick={() => create.mutate(transaction.id)}
    >
      {charge?.status === "FAILED" ? (
        <RotateCcw className="h-3.5 w-3.5" />
      ) : (
        <Link2 className="h-3.5 w-3.5" />
      )}
      {charge?.status === "FAILED" ? "Tentar de novo" : "Cobrar pelo Asaas"}
    </Button>
  );
}
