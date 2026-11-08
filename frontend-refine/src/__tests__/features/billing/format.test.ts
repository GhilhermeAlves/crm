import { describe, expect, it } from "vitest";
import { formatCents, formatLocalDate } from "@/features/billing/format";

describe("billing format", () => {
  it("formata centavos em reais", () => {
    expect(formatCents(19700).replace(/\s/g, " ")).toBe("R$ 197,00");
    expect(formatCents(4990).replace(/\s/g, " ")).toBe("R$ 49,90");
  });

  it("formata LocalDate sem deslocar o dia", () => {
    expect(formatLocalDate("2026-11-15")).toBe("15/11/2026");
    expect(formatLocalDate("2026-11-01", "short")).toBe("01/11");
    expect(formatLocalDate(null)).toBe("—");
  });
});
