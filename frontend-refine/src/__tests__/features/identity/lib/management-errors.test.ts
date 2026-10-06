import { describe, expect, it } from "vitest";
import { describeManagementError } from "@/features/identity/lib/management-errors";

function httpError(body: Record<string, unknown>, status?: number, request = true): unknown {
  return {
    ...(status !== undefined ? { response: { status, data: body } } : {}),
    ...(request ? { request: {} } : {}),
  };
}

describe("management-errors (mapeamento de falhas das ações administrativas)", () => {
  it("401 sessão expirada → unauthorized, sem refetch", () => {
    const err = describeManagementError(httpError({}, 401));
    expect(err.kind).toBe("unauthorized");
    expect(err.shouldRefetch).toBe(false);
  });

  it("403 usa a mensagem do backend quando houver (CrmAccessDeniedException)", () => {
    const err = describeManagementError(
      httpError({ message: "Acesso negado para este recurso." }, 403),
    );
    expect(err.kind).toBe("forbidden");
    expect(err.message).toBe("Acesso negado para este recurso.");
    expect(err.shouldRefetch).toBe(false);
  });

  it("404 → not-found e sugere refetch da lista", () => {
    const err = describeManagementError(httpError({}, 404));
    expect(err.kind).toBe("not-found");
    expect(err.shouldRefetch).toBe(true);
  });

  it("409 (e-mail duplicado) mantém a mensagem do backend", () => {
    const err = describeManagementError(
      httpError({ message: "Já existe um convite pendente para este e-mail." }, 409),
    );
    expect(err.kind).toBe("conflict");
    expect(err.message).toContain("Já existe um convite");
  });

  it("410 (convite não está mais disponível) → gone", () => {
    const err = describeManagementError(httpError({ message: "Convite aceito." }, 410));
    expect(err.kind).toBe("gone");
  });

  it("422 QUOTA_EXCEEDED → quota", () => {
    const err = describeManagementError(
      httpError({ code: "QUOTA_EXCEEDED", message: "Limite de usuários atingido." }, 422),
    );
    expect(err.kind).toBe("quota");
    expect(err.message).toContain("Limite de usuários");
  });

  it("400 com mensagem genérica do backend (último ADMIN) → rejected", () => {
    const err = describeManagementError(
      httpError({ message: "Operação não pode ser concluída." }, 400),
    );
    expect(err.kind).toBe("rejected");
    expect(err.shouldRefetch).toBe(true);
  });

  it("429 → rejected sem expor detalhes", () => {
    const err = describeManagementError(httpError({ message: "Muitas requisições" }, 429));
    expect(err.kind).toBe("rejected");
  });

  it("erros 5xx → unexpected, sem mensagem interna", () => {
    const err = describeManagementError(
      httpError({ message: "NullPointerException em InvitationService", trace: "..." }, 500),
    );
    expect(err.kind).toBe("unexpected");
    expect(err.message).not.toContain("NullPointerException");
  });

  it("falha de rede (sem response, com request) → network", () => {
    const err = describeManagementError(httpError({}, undefined, true));
    expect(err.kind).toBe("network");
  });
});
