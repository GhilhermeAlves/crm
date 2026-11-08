"use client";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useMyBilling, useMyInvoices } from "../hooks";
import { formatCents, formatLocalDate } from "../format";
import { BillingStatusBadge } from "./BillingStatusBadge";
import { InvoiceTable } from "./InvoiceTable";

export function planLabel(plan: string): string {
  return plan.charAt(0) + plan.slice(1).toLowerCase();
}

export function BillingSettingsView() {
  const { data, isLoading, isError } = useMyBilling();
  const invoices = useMyInvoices();

  if (isError && !data) {
    return (
      <p className="text-sm text-muted-foreground">
        Não foi possível carregar os dados de cobrança.
      </p>
    );
  }
  if (isLoading || !data) return <p className="text-sm text-muted-foreground">Carregando…</p>;

  const inactive = data.status === "NOT_STARTED" || data.status === "EXEMPT";
  const late = data.status === "PAST_DUE" || data.status === "BLOCKED";

  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Plano e cobrança</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3 text-sm">
          <Row label="Plano" value={planLabel(data.plan)} />
          <Row label="Valor mensal" value={formatCents(data.monthlyPriceCents)} />
          <div className="flex items-center justify-between">
            <span className="text-muted-foreground">Situação</span>
            <BillingStatusBadge status={data.status} />
          </div>
          <Row label="Próximo vencimento" value={formatLocalDate(data.nextDueDate)} />
          {inactive && (
            <p className="text-muted-foreground">
              A cobrança automática não está ativa para esta empresa.
            </p>
          )}
          {late && data.openInvoiceUrl && (
            <Button asChild>
              <a href={data.openInvoiceUrl} target="_blank" rel="noopener noreferrer">
                Pagar com Pix
              </a>
            </Button>
          )}
        </CardContent>
      </Card>
      {!inactive && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Histórico</CardTitle>
          </CardHeader>
          <CardContent>
            <InvoiceTable
              invoices={invoices.data}
              isLoading={invoices.isLoading}
              isError={invoices.isError && !invoices.data}
            />
          </CardContent>
        </Card>
      )}
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between">
      <span className="text-muted-foreground">{label}</span>
      <span className="font-medium">{value}</span>
    </div>
  );
}
