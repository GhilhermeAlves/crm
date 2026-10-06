import type { InvitationStatus } from "../types/invitation.types";

/** Mensagem exibida para cada status em que o convite não pode mais ser usado. */
export const INVITATION_STATUS_MESSAGE: Record<Exclude<InvitationStatus, "PENDING">, string> = {
  ACCEPTED: "Este convite já foi utilizado.",
  EXPIRED: "Este convite expirou.",
  REVOKED: "Este convite foi revogado.",
};

export type InvitationError =
  /** 410: o backend informa o status do convite. */
  | { kind: "unusable"; status: Exclude<InvitationStatus, "PENDING"> }
  /** 404: token desconhecido. */
  | { kind: "not-found" }
  /** 409: o e-mail do convite já tem conta. */
  | { kind: "account-exists" }
  /** Sem resposta do servidor. */
  | { kind: "network" }
  /** Erro de validação/regra com mensagem própria do backend (4xx). */
  | { kind: "rejected"; message: string }
  | { kind: "unexpected" };

type HttpError = {
  response?: { status?: number; data?: { message?: unknown; invitationStatus?: unknown } };
  request?: unknown;
};

const UNUSABLE: ReadonlyArray<string> = ["ACCEPTED", "EXPIRED", "REVOKED"];

/** Traduz um erro HTTP do fluxo de convite, sem expor detalhes técnicos. */
export function describeInvitationError(error: unknown): InvitationError {
  const http = (error ?? {}) as HttpError;
  const response = http.response;
  if (!response) {
    return http.request ? { kind: "network" } : { kind: "unexpected" };
  }
  const status = response.status ?? 0;
  const invitationStatus = response.data?.invitationStatus;
  if (
    status === 410 &&
    typeof invitationStatus === "string" &&
    UNUSABLE.includes(invitationStatus)
  ) {
    return { kind: "unusable", status: invitationStatus as Exclude<InvitationStatus, "PENDING"> };
  }
  if (status === 404) return { kind: "not-found" };
  if (status === 409) return { kind: "account-exists" };
  if (status === 400 || status === 429) {
    // O 400 genérico do backend ("Operação não pode ser concluída.") cobre,
    // entre outros, o limite de tentativas — não ajuda o usuário.
    return {
      kind: "rejected",
      message: "Não foi possível concluir agora. Aguarde alguns minutos e tente novamente.",
    };
  }
  if (status === 503) {
    // Inclui o caso em que o cadastro pode ter sido concluído sem confirmação.
    return {
      kind: "rejected",
      message:
        "Não foi possível confirmar o cadastro agora. Tente entrar com o e-mail e a senha " +
        "informados; se não conseguir, tente novamente em alguns minutos.",
    };
  }
  const message = response.data?.message;
  if (status > 400 && status < 500 && typeof message === "string" && message) {
    return { kind: "rejected", message };
  }
  return { kind: "unexpected" };
}
