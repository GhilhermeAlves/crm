import { describe, it, expect } from "vitest";
import {
  opportunityFormSchema,
  formatCurrency,
  formatPercent,
} from "./pipeline.schema";

const VALID_CONTACT = "3fa85f64-5717-4562-b3fc-2c963f66afa6";

function makeValues(overrides: Record<string, unknown> = {}) {
  return {
    title: "Contrato anual",
    value: "1500.00",
    contactId: VALID_CONTACT,
    expectedCloseDate: "",
    notes: "",
    ...overrides,
  };
}

describe("pipeline.schema (Sprint 11 — validação de oportunidade)", () => {
  it("aceita uma oportunidade válida", () => {
    const result = opportunityFormSchema.safeParse(makeValues());
    expect(result.success).toBe(true);
  });

  it("rejeita oportunidade sem título", () => {
    const result = opportunityFormSchema.safeParse(makeValues({ title: "" }));
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].path).toContain("title");
    }
  });

  it("rejeita valor zerado ou inválido", () => {
    expect(
      opportunityFormSchema.safeParse(makeValues({ value: "0" })).success,
    ).toBe(false);
    expect(
      opportunityFormSchema.safeParse(makeValues({ value: "abc" })).success,
    ).toBe(false);
    expect(
      opportunityFormSchema.safeParse(makeValues({ value: "-10" })).success,
    ).toBe(false);
  });

  it("rejeita contato inválido (UUID obrigatório)", () => {
    const result = opportunityFormSchema.safeParse(
      makeValues({ contactId: "não-uuid" }),
    );
    expect(result.success).toBe(false);
  });

  it("formata moeda e percentual em pt-BR", () => {
    expect(formatCurrency(1500.5)).toContain("1.500,50");
    expect(formatPercent(0.5)).toContain("50");
  });
});
