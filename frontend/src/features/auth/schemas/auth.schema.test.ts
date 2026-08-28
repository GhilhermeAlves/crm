import { describe, it, expect } from "vitest";
import {
  registerSchema,
  PASSWORD_PATTERN,
  PASSWORD_REQUIREMENTS,
  passwordStrength,
} from "./auth.schema";

function makeValues(overrides: Partial<Record<"name" | "email" | "password" | "confirmPassword", string>> = {}) {
  return {
    name: "Ana Silva",
    email: "ana@example.com",
    password: "Senha!123",
    confirmPassword: "Senha!123",
    ...overrides,
  };
}

describe("auth.schema (Sprint 6.9 — política de senha espelhada do backend)", () => {
  it("aceita uma senha que cumpre todos os requisitos (Senha!123)", () => {
    const result = registerSchema.safeParse(makeValues());
    expect(result.success).toBe(true);
  });

  it("aceita Kc!Valid1Aa", () => {
    const result = registerSchema.safeParse(makeValues({ password: "Kc!Valid1Aa", confirmPassword: "Kc!Valid1Aa" }));
    expect(result.success).toBe(true);
  });

  it.each([
    ["12345678"],
    ["senha1234"],
    ["Senha1234"],
    ["someminusculas1"],
    ["SEMMAIUSCULAS1"],
    ["SemNumero"],
    ["SemSimbolo1"],
    ["Curta!1"], // 7 caracteres
  ])("rejeita senha inválida %s", (password) => {
    const result = registerSchema.safeParse(makeValues({ password, confirmPassword: password }));
    expect(result.success).toBe(false);
    if (!result.success) {
      const issue = result.error.issues.find((i) => i.path.includes("password"));
      expect(issue?.message).toBe("Sua senha ainda não atende aos requisitos.");
    }
  });

  it("mantém o refine de confirmação de senha", () => {
    const result = registerSchema.safeParse(
      makeValues({ password: "Senha!123", confirmPassword: "Senha!124" }),
    );
    expect(result.success).toBe(false);
    if (!result.success) {
      const issue = result.error.issues.find((i) => i.path.includes("confirmPassword"));
      expect(issue?.message).toBe("Senhas não conferem");
    }
  });

  it("PASSWORD_PATTERN é idêntico à política do backend", () => {
    expect(PASSWORD_PATTERN.source).toBe("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$");
  });

  it("PASSWORD_REQUIREMENTS perfaz exatamente os 5 requisitos do backend", () => {
    const labels = PASSWORD_REQUIREMENTS.map((r) => r.label);
    expect(labels).toEqual([
      "Pelo menos 8 caracteres",
      "Uma letra maiúscula",
      "Uma letra minúscula",
      "Um número",
      "Um caractere especial",
    ]);
  });

  it("classifica a força conforme os requisitos satisfeitos", () => {
    expect(passwordStrength("")).toBe("Fraca");
    expect(passwordStrength("12345678")).toBe("Fraca");
    expect(passwordStrength("senha1234")).toBe("Média");
    expect(passwordStrength("Senha1234")).toBe("Média");
    expect(passwordStrength("Senha!123")).toBe("Forte");
  });
});