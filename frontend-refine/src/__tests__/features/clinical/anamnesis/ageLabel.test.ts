import { describe, expect, it } from "vitest";
import { ageLabel } from "@/features/clinical/anamnesis/components/AnamnesisFields";

describe("ageLabel", () => {
  const today = new Date(2026, 9, 29); // 29/10/2026

  it("mostra anos e meses", () => {
    expect(ageLabel("1988-06-15", today)).toBe("38 anos e 4 meses");
  });

  it("não conta o mês antes do dia do aniversário", () => {
    expect(ageLabel("2000-10-30", today)).toBe("25 anos e 11 meses");
  });

  it("singular e anos redondos", () => {
    expect(ageLabel("2025-09-01", today)).toBe("1 ano e 1 mês");
    expect(ageLabel("2006-10-29", today)).toBe("20 anos");
  });

  it("data futura ou inválida não mostra idade", () => {
    expect(ageLabel("2027-01-01", today)).toBeNull();
    expect(ageLabel("abc", today)).toBeNull();
  });
});
