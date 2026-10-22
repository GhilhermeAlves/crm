import { describe, it, expect } from "vitest";
import { createPatientSchema } from "@/features/clinical/patients/schemas/patient.schema";

const base = {
  firstName: "Ana",
  mobile: "(11) 99999-9999",
  email: "ana@example.com",
  street: "Rua das Flores",
  addressNumber: "10",
  neighborhood: "Centro",
  city: "São Paulo",
  state: "SP",
};

function issuePaths(
  result: ReturnType<ReturnType<typeof createPatientSchema>["safeParse"]>,
): string[] {
  return result.success ? [] : result.error.issues.map((issue) => issue.path.join("."));
}

describe("createPatientSchema", () => {
  it("aceita cadastro sem CEP (opcional)", () => {
    const result = createPatientSchema().safeParse({ ...base });
    expect(result.success).toBe(true);
  });

  it("aceita CEP válido quando preenchido", () => {
    const result = createPatientSchema().safeParse({ ...base, cep: "01001-000" });
    expect(result.success).toBe(true);
  });

  it("rejeita CEP com formato inválido quando preenchido", () => {
    const result = createPatientSchema().safeParse({ ...base, cep: "123" });
    expect(result.success).toBe(false);
    expect(issuePaths(result)).toContain("cep");
  });

  it("exige nome e telefone do responsável quando 'Sim'", () => {
    const result = createPatientSchema().safeParse({ ...base, hasGuardian: true });
    expect(result.success).toBe(false);
    const paths = issuePaths(result);
    expect(paths).toContain("guardianName");
    expect(paths).toContain("guardianPhone");
  });

  it("aceita responsável preenchido quando 'Sim'", () => {
    const result = createPatientSchema().safeParse({
      ...base,
      hasGuardian: true,
      guardianName: "Maria",
      guardianPhone: "(11) 98888-8888",
    });
    expect(result.success).toBe(true);
  });

  it("aceita cadastro só com o nome (telefone, e-mail e endereço opcionais)", () => {
    const result = createPatientSchema().safeParse({ firstName: "Ana" });
    expect(result.success).toBe(true);
  });

  it("aceita campos opcionais enviados vazios", () => {
    const result = createPatientSchema().safeParse({
      firstName: "Ana",
      mobile: "",
      email: "",
      street: "",
      addressNumber: "",
      neighborhood: "",
      city: "",
      state: "",
    });
    expect(result.success).toBe(true);
  });

  it("rejeita e-mail inválido quando preenchido", () => {
    const result = createPatientSchema().safeParse({ ...base, email: "ana@" });
    expect(result.success).toBe(false);
    expect(issuePaths(result)).toContain("email");
  });

  it("rejeita UF inválida", () => {
    const result = createPatientSchema().safeParse({ ...base, state: "XX" });
    expect(result.success).toBe(false);
    expect(issuePaths(result)).toContain("state");
  });

  it("exige CPF quando a empresa exige", () => {
    const result = createPatientSchema({ requireCpf: true }).safeParse({ ...base });
    expect(result.success).toBe(false);
    expect(issuePaths(result)).toContain("cpf");
  });
});
