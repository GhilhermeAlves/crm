import { describe, it, expect } from "vitest";
import { centsToBRL, parseBRLToCents } from "@/features/finance/utils/money";

/** O Intl usa espaço não separável (U+00A0) entre "R$" e o número. */
const normalize = (value: string) => value.replace(/\u00A0/g, " ");

describe("centsToBRL", () => {
  it("formata zero", () => {
    expect(normalize(centsToBRL(0))).toBe("R$ 0,00");
  });

  it("formata 5 centavos", () => {
    expect(normalize(centsToBRL(5))).toBe("R$ 0,05");
  });

  it("formata 123456 com separador de milhar", () => {
    expect(normalize(centsToBRL(123456))).toBe("R$ 1.234,56");
  });

  it("formata valores negativos", () => {
    expect(normalize(centsToBRL(-123456))).toBe("-R$ 1.234,56");
  });
});

describe("parseBRLToCents", () => {
  it("converte o formato pt-BR com separador de milhar", () => {
    expect(parseBRLToCents("1.234,56")).toBe(123456);
  });

  it("converte o formato decimal com ponto", () => {
    expect(parseBRLToCents("1234.56")).toBe(123456);
  });

  it("converte centavos sem parte inteira", () => {
    expect(parseBRLToCents("0,05")).toBe(5);
  });

  it("ignora o símbolo da moeda", () => {
    expect(parseBRLToCents("R$ 10,00")).toBe(1000);
  });

  it("retorna null para entrada vazia", () => {
    expect(parseBRLToCents("")).toBeNull();
  });

  it("retorna null para entrada inválida", () => {
    expect(parseBRLToCents("abc")).toBeNull();
  });
});
