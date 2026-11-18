import { describe, expect, it } from "vitest";
import {
  isValidRate,
  simulateCredit,
  type FeeRates,
} from "@/features/finance/credit/utils/creditSimulation";

const flat = (rate: number | null): FeeRates =>
  Object.fromEntries(Array.from({ length: 12 }, (_, i) => [i + 1, rate]));

describe("simulateCredit", () => {
  it("sempre devolve 12 linhas", () => {
    expect(simulateCredit(10000, {}, false)).toHaveLength(12);
  });

  it("sem repasse: paciente paga o valor e a clínica recebe menos a taxa", () => {
    const row = simulateCredit(100000, flat(10), false)[9];
    expect(row).toEqual({
      installments: 10,
      ratePercent: 10,
      patientTotalCents: 100000,
      installmentCents: 10000,
      firstInstallmentCents: 10000,
      netCents: 90000,
      costCents: 10000,
    });
  });

  it("com repasse: a clínica recebe o valor cheio", () => {
    const row = simulateCredit(100000, flat(10), true)[0];
    expect(row.patientTotalCents).toBe(111111);
    expect(row.netCents).toBe(100000);
    expect(row.costCents).toBe(11111);
  });

  it("resto da divisão vai para a primeira parcela", () => {
    const row = simulateCredit(10000, flat(0), false)[2];
    expect(row.installmentCents).toBe(3333);
    expect(row.firstInstallmentCents).toBe(3334);
    expect(row.netCents).toBe(10000);
    expect(row.costCents).toBe(0);
  });

  it("taxa ausente ou inválida deixa a linha sem cálculo", () => {
    const rates = flat(5);
    rates[2] = null;
    rates[3] = 100;
    const rows = simulateCredit(10000, rates, true);
    for (const row of [rows[1], rows[2]]) {
      expect(row.patientTotalCents).toBeNull();
      expect(row.netCents).toBeNull();
      expect(row.installmentCents).toBeNull();
    }
    expect(rows[0].netCents).toBe(10000);
  });
});

describe("isValidRate", () => {
  it("aceita de 0 a menos de 100", () => {
    expect(isValidRate(0)).toBe(true);
    expect(isValidRate(99.99)).toBe(true);
    expect(isValidRate(100)).toBe(false);
    expect(isValidRate(-1)).toBe(false);
    expect(isValidRate(null)).toBe(false);
    expect(isValidRate(Number.NaN)).toBe(false);
  });
});
