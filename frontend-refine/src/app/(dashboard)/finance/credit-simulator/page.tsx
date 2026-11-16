"use client";

import { useState } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { CreditSimulator } from "@/features/finance/credit/components/CreditSimulator";
import { parseBRLToCents } from "@/features/finance/utils/money";

export default function CreditSimulatorPage() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const [amount, setAmount] = useState("");
  const cents = parseBRLToCents(amount);
  const valid = cents != null && cents > 0;

  return (
    <div className="mx-auto w-full max-w-5xl space-y-6 p-4 sm:p-6">
      <div>
        <h1 className="text-2xl font-semibold">Simulador de crédito</h1>
        <p className="text-sm text-muted-foreground">
          Veja quanto fica cada parcelamento no cartão e quanto a clínica recebe.
        </p>
      </div>
      <Card>
        <CardContent className="space-y-6 pt-6">
          <div className="max-w-xs space-y-1">
            <Label htmlFor="credit-amount">Valor da venda (R$)</Label>
            <Input
              id="credit-amount"
              inputMode="decimal"
              placeholder="0,00"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
            />
          </div>
          {valid ? (
            <CreditSimulator amountCents={cents} companyId={companyId} />
          ) : (
            <p className="text-sm text-muted-foreground">Informe um valor para simular.</p>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
