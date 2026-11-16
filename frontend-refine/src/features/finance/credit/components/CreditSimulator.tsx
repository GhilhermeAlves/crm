"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { ROUTES } from "@/lib/constants";
import { centsToBRL } from "../../utils/money";
import { ratesToMap, useCardFees, type CardFeePreset } from "../hooks/useCardFees";
import { simulateCredit } from "../utils/creditSimulation";
import { draftsFromRates, ratesFromDrafts, type RateDrafts } from "../utils/rateInput";
import { PresetPicker } from "./PresetPicker";
import { RatesNotice } from "./RatesNotice";

type Props = {
  amountCents: number;
  companyId: string | null;
};

const money = (cents: number | null) => (cents == null ? "—" : centsToBRL(cents));

/**
 * Tabela de 1x a 12x no crédito: parcela, total do paciente e quanto a clínica recebe.
 * As taxas vêm do cadastro da clínica e podem ser ajustadas só para esta simulação.
 */
export function CreditSimulator({ amountCents, companyId }: Props) {
  const { data: saved, isLoading } = useCardFees(companyId);
  const savedRates = useMemo(() => ratesToMap(saved), [saved]);
  const hasSaved = (saved?.length ?? 0) > 0;

  const [drafts, setDrafts] = useState<RateDrafts>(() => draftsFromRates(savedRates));
  const [passFee, setPassFee] = useState(false);
  const [preset, setPreset] = useState<CardFeePreset | null>(null);

  useEffect(() => {
    setDrafts(draftsFromRates(savedRates));
    setPreset(null);
  }, [savedRates]);

  const rows = useMemo(
    () => simulateCredit(amountCents, ratesFromDrafts(drafts), passFee),
    [amountCents, drafts, passFee],
  );

  const resetToSaved = () => {
    setDrafts(draftsFromRates(savedRates));
    setPreset(null);
  };

  return (
    <div className="space-y-4">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-2">
          <Switch id="credit-pass-fee" checked={passFee} onCheckedChange={setPassFee} />
          <Label htmlFor="credit-pass-fee">Repassar juros ao paciente</Label>
        </div>
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
          <PresetPicker
            onPick={(p) => {
              setDrafts(draftsFromRates(ratesToMap(p.rates)));
              setPreset(p);
            }}
          />
          <Button variant="outline" size="sm" onClick={resetToSaved} disabled={!hasSaved}>
            Voltar às taxas cadastradas
          </Button>
        </div>
      </div>

      {preset && <RatesNotice preset={preset} />}
      {!isLoading && !hasSaved && !preset && (
        <p className="text-sm text-muted-foreground">
          A clínica ainda não cadastrou as taxas da maquininha. Escolha um modelo, digite as taxas
          abaixo ou{" "}
          <Link href={ROUTES.FINANCE_CARD_FEES} className="underline">
            cadastre as taxas do cartão
          </Link>
          .
        </p>
      )}

      <div className="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Parcelas</TableHead>
              <TableHead>Valor da parcela</TableHead>
              <TableHead>Total do paciente</TableHead>
              <TableHead className="w-28">Taxa (%)</TableHead>
              <TableHead>Clínica recebe</TableHead>
              <TableHead>Custo da taxa</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {rows.map((row) => (
              <TableRow key={row.installments} data-testid={`credit-row-${row.installments}`}>
                <TableCell className="font-medium">{row.installments}x</TableCell>
                <TableCell>
                  {money(row.installmentCents)}
                  {row.firstInstallmentCents != null &&
                    row.firstInstallmentCents !== row.installmentCents && (
                      <span className="block text-xs text-muted-foreground">
                        1ª de {money(row.firstInstallmentCents)}
                      </span>
                    )}
                </TableCell>
                <TableCell>{money(row.patientTotalCents)}</TableCell>
                <TableCell>
                  <Input
                    aria-label={`Taxa de ${row.installments}x`}
                    inputMode="decimal"
                    className="h-8 w-24"
                    value={drafts[row.installments] ?? ""}
                    onChange={(e) =>
                      setDrafts((d) => ({ ...d, [row.installments]: e.target.value }))
                    }
                  />
                </TableCell>
                <TableCell>{money(row.netCents)}</TableCell>
                <TableCell>{money(row.costCents)}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
      <p className="text-xs text-muted-foreground">
        Simulação apenas para consulta; nada é gravado no orçamento.
      </p>
    </div>
  );
}
