"use client";

import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { MAX_INSTALLMENTS, isValidRate } from "../utils/creditSimulation";
import { parseRate, type RateDrafts } from "../utils/rateInput";

type Props = {
  drafts: RateDrafts;
  onChange: (drafts: RateDrafts) => void;
  disabled?: boolean;
  idPrefix: string;
};

/** Grade com os 12 campos de taxa (1x a 12x). */
export function RatesForm({ drafts, onChange, disabled, idPrefix }: Props) {
  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 lg:grid-cols-6">
      {Array.from({ length: MAX_INSTALLMENTS }, (_, i) => i + 1).map((n) => {
        const value = drafts[n] ?? "";
        const invalid = value.trim() !== "" && !isValidRate(parseRate(value));
        return (
          <div key={n} className="space-y-1">
            <Label htmlFor={`${idPrefix}-${n}`}>{n}x (%)</Label>
            <Input
              id={`${idPrefix}-${n}`}
              inputMode="decimal"
              value={value}
              disabled={disabled}
              aria-invalid={invalid}
              onChange={(e) => onChange({ ...drafts, [n]: e.target.value })}
            />
          </div>
        );
      })}
    </div>
  );
}

/** Todas as 12 taxas preenchidas e válidas, com no máximo 2 casas. */
export function draftsAreComplete(drafts: RateDrafts): boolean {
  for (let n = 1; n <= MAX_INSTALLMENTS; n++) {
    const rate = parseRate(drafts[n] ?? "");
    if (!isValidRate(rate) || Math.round(rate * 100) !== rate * 100) return false;
  }
  return true;
}
