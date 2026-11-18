"use client";

import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { useAuthorization } from "@/features/identity/auth/hooks/useAuthorization";
import { PresetPicker } from "@/features/finance/credit/components/PresetPicker";
import { RatesForm, draftsAreComplete } from "@/features/finance/credit/components/RatesForm";
import { RatesNotice } from "@/features/finance/credit/components/RatesNotice";
import {
  mapToRates,
  ratesToMap,
  useCardFees,
  useSaveCardFees,
  type CardFeePreset,
} from "@/features/finance/credit/hooks/useCardFees";
import {
  draftsFromRates,
  ratesFromDrafts,
  type RateDrafts,
} from "@/features/finance/credit/utils/rateInput";

export default function CardFeesPage() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const { can, isPlatformAdmin } = useAuthorization();
  const canEdit = isPlatformAdmin || can("finance:manage");

  const { data: saved, isLoading } = useCardFees(companyId);
  const save = useSaveCardFees(companyId);
  const [drafts, setDrafts] = useState<RateDrafts>({});
  const [preset, setPreset] = useState<CardFeePreset | null>(null);

  useEffect(() => {
    setDrafts(draftsFromRates(ratesToMap(saved)));
  }, [saved]);

  const complete = draftsAreComplete(drafts);

  const onSave = () => {
    const rates = mapToRates(ratesFromDrafts(drafts));
    if (!rates) return;
    save.mutate(rates, {
      onSuccess: () => {
        setPreset(null);
        toast.success("Taxas do cartão salvas");
      },
    });
  };

  return (
    <div className="mx-auto w-full max-w-5xl space-y-6 p-4 sm:p-6">
      <div>
        <h1 className="text-2xl font-semibold">Taxas do cartão</h1>
        <p className="text-sm text-muted-foreground">
          Taxa total que a maquininha desconta em cada número de parcelas (inclua a antecipação, se
          a clínica antecipa). O simulador de crédito usa estas taxas.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Crédito de 1x a 12x</CardTitle>
          <CardDescription>
            Comece por um modelo de maquininha e ajuste para as taxas que a clínica negociou.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {canEdit && (
            <PresetPicker
              onPick={(p) => {
                setDrafts(draftsFromRates(ratesToMap(p.rates)));
                setPreset(p);
              }}
            />
          )}
          {preset && <RatesNotice preset={preset} />}
          <RatesForm
            idPrefix="card-fee"
            drafts={drafts}
            onChange={setDrafts}
            disabled={!canEdit || isLoading}
          />
          {canEdit && (
            <div className="flex items-center justify-end gap-3">
              {!complete && (
                <span className="text-xs text-muted-foreground">
                  Preencha as 12 taxas (0 a 99,99, até 2 casas).
                </span>
              )}
              <Button onClick={onSave} disabled={!complete || save.isPending}>
                {save.isPending ? "Salvando…" : "Salvar"}
              </Button>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
