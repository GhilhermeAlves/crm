import { describe, it, expect } from "vitest";
import { leadFormSchema, type LeadFormValues } from "./lead.schema";

const VALID_CONTACT = "3fa85f64-5717-4562-b3fc-2c963f66afa6";

function makeValues(overrides: Partial<LeadFormValues> = {}): LeadFormValues {
  return {
    contactId: VALID_CONTACT,
    status: "NEW",
    source: "MANUAL",
    score: "50",
    classification: "",
    assignedTo: "",
    notes: "",
    ...overrides,
  } as LeadFormValues;
}

describe("lead.schema (Sprint 10 — validação de lead)", () => {
  it("aceita um lead válido com origem e contato", () => {
    const result = leadFormSchema.safeParse(makeValues());
    expect(result.success).toBe(true);
  });

  it("rejeita lead sem contato (UUID obrigatório)", () => {
    const result = leadFormSchema.safeParse(makeValues({ contactId: "abc" }));
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].path).toContain("contactId");
    }
  });

  it("rejeita origem inválida (só aceita WHATSAPP/FORM/API/IMPORT/MANUAL)", () => {
    const result = leadFormSchema.safeParse(makeValues({ source: "TWITTER" as never }));
    expect(result.success).toBe(false);
  });

  it("valida score entre 0 e 100", () => {
    expect(leadFormSchema.safeParse(makeValues({ score: "150" })).success).toBe(false);
    expect(leadFormSchema.safeParse(makeValues({ score: "-5" })).success).toBe(false);
    expect(leadFormSchema.safeParse(makeValues({ score: "100" })).success).toBe(true);
  });

  it("aceita classificação opcional vazia ou válida", () => {
    expect(leadFormSchema.safeParse(makeValues({ classification: "HOT" })).success).toBe(true);
    expect(leadFormSchema.safeParse(makeValues({ classification: "" })).success).toBe(true);
  });
});