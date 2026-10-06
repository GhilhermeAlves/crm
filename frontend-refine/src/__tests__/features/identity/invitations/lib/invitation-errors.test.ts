import { describe, it, expect } from "vitest";
import { describeInvitationError } from "@/features/identity/invitations/lib/invitation-errors";

const http = (status: number, data: Record<string, unknown> = {}) => ({
  response: { status, data },
});

describe("describeInvitationError", () => {
  it("410 usa o status do convite enviado pelo backend", () => {
    expect(describeInvitationError(http(410, { invitationStatus: "ACCEPTED" }))).toEqual({
      kind: "unusable",
      status: "ACCEPTED",
    });
    expect(describeInvitationError(http(410, { invitationStatus: "EXPIRED" }))).toEqual({
      kind: "unusable",
      status: "EXPIRED",
    });
    expect(describeInvitationError(http(410, { invitationStatus: "REVOKED" }))).toEqual({
      kind: "unusable",
      status: "REVOKED",
    });
  });

  it("410 sem status reconhecido vira erro inesperado", () => {
    expect(describeInvitationError(http(410, { invitationStatus: "PENDING" })).kind).toBe(
      "unexpected",
    );
  });

  it("404 é convite inválido e 409 é conta existente", () => {
    expect(describeInvitationError(http(404)).kind).toBe("not-found");
    expect(describeInvitationError(http(409)).kind).toBe("account-exists");
  });

  it("sem resposta é erro de rede", () => {
    expect(describeInvitationError({ request: {} }).kind).toBe("network");
  });

  it("400 genérico não repassa 'Operação não pode ser concluída.'", () => {
    const result = describeInvitationError(
      http(400, { message: "Operação não pode ser concluída." }),
    );
    expect(result.kind).toBe("rejected");
    expect(result.kind === "rejected" && result.message).not.toContain("Operação não pode");
  });

  it("422 repassa a mensagem do backend (ex.: limite de usuários)", () => {
    expect(
      describeInvitationError(
        http(422, { message: "Limite de usuários da empresa atingido (5)." }),
      ),
    ).toEqual({
      kind: "rejected",
      message: "Limite de usuários da empresa atingido (5).",
    });
  });

  it("5xx sem tratamento específico não expõe detalhes", () => {
    expect(
      describeInvitationError(http(500, { message: "NullPointerException at ..." })).kind,
    ).toBe("unexpected");
  });
});
