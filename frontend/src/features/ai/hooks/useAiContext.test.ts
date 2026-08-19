import { describe, it, expect } from "vitest";
import { resolveAiContext } from "./useAiContext";

describe("resolveAiContext (AI-04 §8-11)", () => {
  it("resolves Customer 360 em /customers/{id}", () => {
    const id = "3fa85f64-5717-4562-b3fc-2c963f66afa6";
    expect(resolveAiContext(`/customers/${id}`)).toEqual({
      screen: "customer360",
      route: `/customers/${id}`,
      recordType: "CUSTOMER",
      recordId: id,
    });
  });

  it("resolves Opportunity em /opportunities/{id}", () => {
    const id = "4fa85f64-5717-4562-b3fc-2c963f66afa6";
    expect(resolveAiContext(`/opportunities/${id}`)).toEqual({
      screen: "opportunity",
      route: `/opportunities/${id}`,
      recordType: "OPPORTUNITY",
      recordId: id,
    });
  });

  it("resolves Contact em /contacts/{id}", () => {
    const id = "5fa85f64-5717-4562-b3fc-2c963f66afa6";
    expect(resolveAiContext(`/contacts/${id}`)).toEqual({
      screen: "contact",
      route: `/contacts/${id}`,
      recordType: "CONTACT",
      recordId: id,
    });
  });

  it("retorna null sem contexto (tela sem registro em foco)", () => {
    expect(resolveAiContext("/dashboard")).toBeNull();
    expect(resolveAiContext("/assistant")).toBeNull();
    expect(resolveAiContext("/")).toBeNull();
  });

  it("retorna null para rota inválida (id não-UUID)", () => {
    expect(resolveAiContext("/customers/abc")).toBeNull();
    expect(resolveAiContext("/customers/123")).toBeNull();
  });

  it("retorna null para segmento desconhecido", () => {
    expect(resolveAiContext("/unknown/3fa85f64-5717-4562-b3fc-2c963f66afa6")).toBeNull();
  });
});
