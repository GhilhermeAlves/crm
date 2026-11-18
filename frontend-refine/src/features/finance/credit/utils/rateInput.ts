import { MAX_INSTALLMENTS, type FeeRates } from "./creditSimulation";

/** Taxas como texto digitado (aceita vírgula), por número de parcelas. */
export type RateDrafts = Record<number, string>;

export function parseRate(value: string): number | null {
  const text = value.trim().replace("%", "").replace(",", ".");
  if (!text) return null;
  const n = Number(text);
  return Number.isFinite(n) ? n : null;
}

export function formatRate(rate: number | null | undefined): string {
  return rate == null ? "" : String(rate).replace(".", ",");
}

export function draftsFromRates(rates: FeeRates): RateDrafts {
  const drafts: RateDrafts = {};
  for (let n = 1; n <= MAX_INSTALLMENTS; n++) drafts[n] = formatRate(rates[n]);
  return drafts;
}

export function ratesFromDrafts(drafts: RateDrafts): FeeRates {
  const rates: FeeRates = {};
  for (let n = 1; n <= MAX_INSTALLMENTS; n++) rates[n] = parseRate(drafts[n] ?? "");
  return rates;
}
