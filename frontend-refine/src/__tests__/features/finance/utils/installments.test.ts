import { describe, expect, it } from "vitest";
import { describeInstallments } from "@/features/finance/utils/installments";

const normalize = (value: string | null): string | null => value?.replace(/\u00A0/g, " ") ?? null;

describe("describeInstallments", () => {
  it("returns null for a single installment", () => {
    expect(describeInstallments(10000, 1)).toBeNull();
  });

  it("splits evenly when the amount divides exactly", () => {
    expect(normalize(describeInstallments(30000, 2))).toBe("2x de R$ 150,00");
  });

  it("adds the remainder cents to the first installment", () => {
    expect(normalize(describeInstallments(10000, 3))).toBe(
      "3x (1ª de R$ 33,34, demais de R$ 33,33)",
    );
  });
});
