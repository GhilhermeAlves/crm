import { describe, expect, it } from "vitest";

import {
  formatCompactMoney,
  formatMoneyLabel,
} from "@/features/finance/components/reports/report-format";

describe("formatCompactMoney", () => {
  it("abrevia milhares com 'k'", () => {
    expect(formatCompactMoney(9000)).toBe("R$9k");
  });

  it("mostra valores pequenos sem abreviação", () => {
    expect(formatCompactMoney(400)).toBe("R$400");
    expect(formatCompactMoney(0)).toBe("R$0");
  });

  it("abrevia milhões com 'mi'", () => {
    expect(formatCompactMoney(2_500_000)).toBe("R$2,5mi");
  });
});

describe("formatMoneyLabel", () => {
  it("usa separador de milhar sem decimais", () => {
    expect(formatMoneyLabel(6320)).toBe("R$6.320");
    expect(formatMoneyLabel(0)).toBe("R$0");
  });
});
