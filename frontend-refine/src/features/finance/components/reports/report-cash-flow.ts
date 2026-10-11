import type { IncomeByMethod } from "../../types/finance.types";

export type PaymentMethodCard = {
  key: keyof IncomeByMethod;
  label: string;
};

/** Ordem fixa das formas de pagamento exibidas no detalhamento de entradas. */
export const PAYMENT_METHOD_CARDS: PaymentMethodCard[] = [
  { key: "boletoCents", label: "Boleto" },
  { key: "creditCardCents", label: "Cartão de crédito" },
  { key: "debitCardCents", label: "Cartão de débito" },
  { key: "pixCents", label: "Pix" },
  { key: "cashCents", label: "Dinheiro" },
  { key: "transferCents", label: "Transferência" },
  { key: "checkCents", label: "Cheque" },
  { key: "otherCents", label: "Outro" },
];

export type PaymentMethodValue = {
  label: string;
  cents: number;
};

/** Normaliza o resumo por forma de pagamento em sempre 8 cards (zero quando ausente). */
export function toPaymentMethodValues(
  incomeByMethod?: IncomeByMethod | null,
): PaymentMethodValue[] {
  return PAYMENT_METHOD_CARDS.map(({ key, label }) => ({
    label,
    cents: incomeByMethod?.[key] ?? 0,
  }));
}
