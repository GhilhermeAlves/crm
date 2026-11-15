import { describe, expect, it } from "vitest";
import { toHttpError } from "@/lib/http-error";

describe("toHttpError", () => {
  it("usa message do corpo e o status", () => {
    const e = { response: { status: 400, data: { message: "Inválido", code: "X" } } };
    expect(toHttpError(e)).toEqual({
      status: 400,
      message: "Inválido",
      code: "X",
      errors: undefined,
    });
  });

  it("cai em detail (ProblemDetail do Spring)", () => {
    const e = { response: { status: 404, data: { detail: "Não encontrado" } } };
    expect(toHttpError(e).message).toBe("Não encontrado");
  });

  it("normaliza erros de validação em mapa e em lista", () => {
    const map = { response: { status: 422, data: { errors: { nome: ["Obrigatório"] } } } };
    const list = {
      response: { status: 422, data: { fieldErrors: [{ field: "email", message: "Inválido" }] } },
    };
    expect(toHttpError(map).errors).toEqual({ nome: "Obrigatório" });
    expect(toHttpError(list).errors).toEqual({ email: "Inválido" });
  });

  it("usa o fallback quando o corpo não tem mensagem", () => {
    expect(toHttpError({ response: { status: 500, data: "" } }, "Erro ao salvar").message).toBe(
      "Erro ao salvar",
    );
  });

  it("identifica falha de rede (sem resposta)", () => {
    expect(toHttpError({ request: {} })).toMatchObject({ status: 0 });
  });

  it("usa a mensagem de um Error comum", () => {
    expect(toHttpError(new Error("boom")).message).toBe("boom");
  });
});
