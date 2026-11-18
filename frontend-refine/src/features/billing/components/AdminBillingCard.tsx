"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import {
  useActivateBilling,
  useAdminBilling,
  useAdminInvoices,
  useCancelBilling,
  useChangeBillingPrice,
  useExemptBilling,
  useGrantBilling,
} from "../hooks";
import { formatCents, formatLocalDate } from "../format";
import { BillingStatusBadge } from "./BillingStatusBadge";
import { InvoiceTable } from "./InvoiceTable";
import { planLabel } from "./BillingSettingsView";

export function AdminBillingCard({ companyId }: { companyId: string }) {
  const { data, isLoading, isError } = useAdminBilling(companyId);
  const invoices = useAdminInvoices(companyId);
  const activate = useActivateBilling(companyId);
  const exempt = useExemptBilling(companyId);
  const price = useChangeBillingPrice(companyId);
  const grant = useGrantBilling(companyId);
  const cancel = useCancelBilling(companyId);
  const [editing, setEditing] = useState(false);
  const [reais, setReais] = useState("");
  const [days, setDays] = useState("7");

  if (isError && !data) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Cobrança</CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          Não foi possível carregar a cobrança desta empresa.
        </CardContent>
      </Card>
    );
  }

  if (isLoading || !data) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Cobrança</CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">Carregando…</CardContent>
      </Card>
    );
  }

  const cents = Math.round(Number(reais.replace(",", ".")) * 100);
  const daysNumber = Number(days);
  const validDays = Number.isInteger(daysNumber) && daysNumber >= 1 && daysNumber <= 30;
  const late = data.status === "PAST_DUE" || data.status === "BLOCKED";

  return (
    <Card className="lg:col-span-2">
      <CardHeader>
        <CardTitle className="text-base">Cobrança</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4 text-sm">
        <div className="grid gap-3 sm:grid-cols-2">
          <div className="flex items-center justify-between">
            <span className="text-muted-foreground">Situação</span>
            <BillingStatusBadge status={data.status} />
          </div>
          <Info label="Plano" value={planLabel(data.plan)} />
          <div className="flex items-center justify-between gap-2">
            <span className="text-muted-foreground">Valor mensal</span>
            {editing ? (
              <div className="flex items-center gap-2">
                <Label htmlFor="billing-price" className="sr-only">
                  Valor mensal (R$)
                </Label>
                <Input
                  id="billing-price"
                  aria-label="Valor mensal (R$)"
                  type="number"
                  step="0.01"
                  min="0.01"
                  max={100000}
                  className="h-8 w-28"
                  value={reais}
                  onChange={(e) => setReais(e.target.value)}
                />
                <Button
                  size="sm"
                  disabled={!(cents > 0) || cents > 10_000_000 || price.isPending}
                  onClick={() => price.mutate(cents, { onSuccess: () => setEditing(false) })}
                >
                  Salvar
                </Button>
                <Button size="sm" variant="ghost" onClick={() => setEditing(false)}>
                  Cancelar
                </Button>
              </div>
            ) : (
              <div className="flex items-center gap-2">
                <span className="font-medium">{formatCents(data.monthlyPriceCents)}</span>
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => {
                    setReais((data.monthlyPriceCents / 100).toFixed(2));
                    setEditing(true);
                  }}
                >
                  Editar
                </Button>
              </div>
            )}
          </div>
          <Info label="Próximo vencimento" value={formatLocalDate(data.nextDueDate)} />
          {data.overdueSince && (
            <Info label="Atrasada desde" value={formatLocalDate(data.overdueSince)} />
          )}
          {data.graceUntil && (
            <Info label="Liberada até" value={formatLocalDate(data.graceUntil)} />
          )}
        </div>
        {data.syncError && <p className="text-xs text-red-600">{data.syncError}</p>}

        <div className="flex flex-wrap items-end gap-2">
          {data.status === "NOT_STARTED" && (
            <Button onClick={() => activate.mutate()} disabled={activate.isPending}>
              Ativar cobrança
            </Button>
          )}
          {data.status === "SYNC_PENDING" && (
            <Button onClick={() => activate.mutate()} disabled={activate.isPending}>
              Tentar novamente
            </Button>
          )}
          {data.status !== "EXEMPT" && !data.hasSubscription && (
            <AlertDialog>
              <AlertDialogTrigger asChild>
                <Button variant="outline" disabled={exempt.isPending}>
                  Marcar como isenta
                </Button>
              </AlertDialogTrigger>
              <AlertDialogContent>
                <AlertDialogHeader>
                  <AlertDialogTitle>Marcar como isenta?</AlertDialogTitle>
                  <AlertDialogDescription>
                    A empresa deixa de ser cobrada e nunca é bloqueada.
                  </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                  <AlertDialogCancel>Cancelar</AlertDialogCancel>
                  <AlertDialogAction onClick={() => exempt.mutate()}>Isentar</AlertDialogAction>
                </AlertDialogFooter>
              </AlertDialogContent>
            </AlertDialog>
          )}
          {(data.hasSubscription || data.status === "SYNC_PENDING") && (
            <AlertDialog>
              <AlertDialogTrigger asChild>
                <Button variant="destructive" disabled={cancel.isPending}>
                  Encerrar cobrança
                </Button>
              </AlertDialogTrigger>
              <AlertDialogContent>
                <AlertDialogHeader>
                  <AlertDialogTitle>Encerrar a cobrança desta empresa?</AlertDialogTitle>
                  <AlertDialogDescription>
                    A assinatura é removida no Asaas, junto com as cobranças em aberto. A empresa
                    deixa de ser cobrada e de ser bloqueada. O histórico de faturas é mantido e a
                    cobrança pode ser ativada de novo depois (ou a empresa marcada como isenta).
                  </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                  <AlertDialogCancel>Voltar</AlertDialogCancel>
                  <AlertDialogAction onClick={() => cancel.mutate()}>
                    Encerrar cobrança
                  </AlertDialogAction>
                </AlertDialogFooter>
              </AlertDialogContent>
            </AlertDialog>
          )}
          {late && (
            <div className="flex items-end gap-2">
              <div className="space-y-1">
                <Label htmlFor="billing-grant-days">Dias</Label>
                <Input
                  id="billing-grant-days"
                  type="number"
                  min={1}
                  max={30}
                  className="h-9 w-20"
                  value={days}
                  onChange={(e) => setDays(e.target.value)}
                />
              </div>
              <Button
                variant="outline"
                disabled={!validDays || grant.isPending}
                onClick={() => grant.mutate(daysNumber)}
              >
                Liberar
              </Button>
            </div>
          )}
        </div>

        <Separator />
        <p className="font-medium">Histórico</p>
        <InvoiceTable
          invoices={invoices.data}
          isLoading={invoices.isLoading}
          isError={invoices.isError && !invoices.data}
        />
      </CardContent>
    </Card>
  );
}

function Info({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between">
      <span className="text-muted-foreground">{label}</span>
      <span className="font-medium">{value}</span>
    </div>
  );
}
