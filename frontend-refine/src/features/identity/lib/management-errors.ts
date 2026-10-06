/**
 * Tradução de erros HTTP das ações administrativas de membros e convites.
 *
 * Não substitui a autorização do backend: serve só para mostrar uma mensagem
 * amigável e para saber quando um refetch é apropriado. Nenhum stack trace ou
 * detalhe interno é exposto.
 */

type HttpError = {
  response?: { status?: number; data?: { message?: unknown; code?: unknown } };
  request?: unknown;
};

export type ManagementErrorKind =
  | "unauthorized"
  | "forbidden"
  | "not-found"
  | "conflict"
  | "gone"
  | "quota"
  | "rejected"
  | "network"
  | "unexpected";

export type ManagementError = {
  kind: ManagementErrorKind;
  message: string;
  /** O backend já resolveu o estado — convém refazer as listas. */
  shouldRefetch: boolean;
};

const MESSAGES: Record<ManagementErrorKind, string> = {
  unauthorized: "Sua sessão expirou. Entre novamente para continuar.",
  forbidden: "Você não tem permissão para executar esta ação.",
  "not-found": "Registro não encontrado. Atualize a lista e tente novamente.",
  conflict: "Já existe um registro com estes dados.",
  gone: "Este convite não está mais disponível.",
  quota: "Limite de usuários da empresa atingido.",
  rejected: "Não foi possível concluir a operação. Verifique os dados e tente novamente.",
  network: "Falha de conexão. Verifique sua rede e tente novamente.",
  unexpected: "Algo deu errado. Tente novamente em instantes.",
};

/**
 * 400 no backend cobre casos distintos com mensagem genérica ("Operação não
 * pode ser concluída."), inclusive o último ADMIN. Por isso a mensagem padrão
 * é deliberadamente neutra e a proteção do último ADMIN é feita na UI.
 */
export function describeManagementError(error: unknown): ManagementError {
  const http = (error ?? {}) as HttpError;
  const response = http.response;

  if (!response) {
    if (http.request) {
      return { kind: "network", message: MESSAGES.network, shouldRefetch: false };
    }
    const fallback = error instanceof Error ? error.message : "";
    return {
      kind: "unexpected",
      message: fallback || MESSAGES.unexpected,
      shouldRefetch: false,
    };
  }

  const status = response.status ?? 0;
  const serverMessage = response.data?.message;
  const code = response.data?.code;

  const build = (kind: ManagementErrorKind, message?: string): ManagementError => ({
    kind,
    message: message || MESSAGES[kind],
    shouldRefetch: kind !== "forbidden" && kind !== "unauthorized",
  });

  switch (status) {
    case 401:
      return build("unauthorized");
    case 403:
      // Mensagem própria do backend quando vem de CrmAccessDeniedException.
      return build(
        "forbidden",
        typeof serverMessage === "string" && serverMessage ? serverMessage : undefined,
      );
    case 404:
      return build("not-found");
    case 409:
      return build(
        "conflict",
        typeof serverMessage === "string" && serverMessage ? serverMessage : undefined,
      );
    case 410:
      return build(
        "gone",
        typeof serverMessage === "string" && serverMessage ? serverMessage : undefined,
      );
    case 422:
      return build(
        "quota",
        code === "QUOTA_EXCEEDED" && typeof serverMessage === "string" && serverMessage
          ? serverMessage
          : undefined,
      );
    case 400:
    case 429:
      return build("rejected");
    default:
      if (status >= 400 && status < 500) return build("rejected");
      return build("unexpected");
  }
}

/** Mensagem pronta para exibir em toast. */
export function toToastMessage(error: unknown): string {
  return describeManagementError(error).message;
}
