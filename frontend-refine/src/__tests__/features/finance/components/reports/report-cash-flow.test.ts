import { describe, expect, it } from "vitest";

import {
  PAYMENT_METHOD_CARDS,
  toPaymentMethodValues,
} from "@/features/finance/components/reports/report-cash-flow";

const LABELS = [
  "Boleto",
  "Cartão de crédito",
  "Cartão de débito",
  "Pix",
  "Dinheiro",
  "Transferência",
  "Cheque",
  "Outro",
];

describe("toPaymentMethodValues", () => {
  it("define sempre as 8 formas de pagamento na ordem fixa", () => {
    expect(PAYMENT_METHOD_CARDS.map((card) => card.label)).toEqual(LABELS);
  });

  it("retorna as 8 formas com zero quando o resumo está ausente", () => {
    const values = toPaymentMethodValues(undefined);

    expect(values).toHaveLength(8);
    expect(values.map((value) => value.label)).toEqual(LABELS);
    expect(values.every((value) => value.cents === 0)).toBe(true);
  });

  it("mapeia os valores informados para cada forma", () => {
    const values = toPaymentMethodValues({
      boletoCents: 900000,
      creditCardCents: 0,
      debitCardCents: 0,
      pixCents: 40000,
      cashCents: 0,
      transferCents: 0,
      checkCents: 0,
      otherCents: 0,
    });

    expect(values).toHaveLength(8);
    expect(values.find((value) => value.label === "Boleto")?.cents).toBe(900000);
    expect(values.find((value) => value.label === "Pix")?.cents).toBe(40000);
    expect(values.find((value) => value.label === "Cheque")?.cents).toBe(0);
  });
});
