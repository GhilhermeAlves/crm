import { describe, it, expect } from "vitest";
import { buildHolidayLookup } from "@/features/sales/holiday/utils/holidayLookup";

describe("buildHolidayLookup", () => {
  const lookup = buildHolidayLookup([
    { date: "2026-10-22", name: "Aniversário da clínica" },
    { date: "2026-12-25", name: "Recesso de Natal" },
  ]);

  it("mostra o feriado da clínica no dia", () => {
    expect(lookup(new Date(2026, 9, 22))).toBe("Aniversário da clínica");
  });

  it("o da clínica tem prioridade sobre o nacional no mesmo dia", () => {
    expect(lookup(new Date(2026, 11, 25))).toBe("Recesso de Natal");
  });

  it("sem feriado da clínica, usa o nacional", () => {
    expect(lookup(new Date(2026, 8, 7))).toBeTruthy(); // 7 de setembro
    expect(lookup(new Date(2026, 9, 21))).toBeUndefined();
  });
});
