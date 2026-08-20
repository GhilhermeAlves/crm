import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import * as React from "react";
import {
  useAiPermissions,
  useAiAnalyze,
  useAiChat,
  useAiConversations,
  useAiConversationMessages,
  useAiConversationActions,
  useAiConfirmAction,
  useAiCancelAction,
} from "./useAi";

const {
  canMock,
  chatMock,
  analyzeMock,
  listMock,
  messagesMock,
  listActionsMock,
  confirmMock,
  cancelMock,
  toastErrorMock,
  invalidateMock,
} = vi.hoisted(() => ({
  canMock: vi.fn(),
  chatMock: vi.fn(),
  analyzeMock: vi.fn(),
  listMock: vi.fn(),
  messagesMock: vi.fn(),
  listActionsMock: vi.fn(),
  confirmMock: vi.fn(),
  cancelMock: vi.fn(),
  toastErrorMock: vi.fn(),
  invalidateMock: vi.fn(),
}));

vi.mock("@/features/auth/hooks/useAuthorization", () => ({
  useAuthorization: () => ({ can: canMock }),
}));

vi.mock("../services/ai.service", () => ({
  AiService: {
    suggest: vi.fn(),
    chat: chatMock,
    analyze: analyzeMock,
    listConversations: listMock,
    getConversationMessages: messagesMock,
    listConversationActions: listActionsMock,
    confirmAction: confirmMock,
    cancelAction: cancelMock,
  },
  aiErrorMessage: (error: unknown) => "Mensagem amigável.",
  aiAnalysisErrorMessage: (error: unknown) => "Falha na análise.",
}));

vi.mock("sonner", () => ({ toast: { error: toastErrorMock } }));

const mockQueryClient = vi.hoisted(() => ({
  invalidateQueries: invalidateMock,
}));

vi.mock("@tanstack/react-query", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@tanstack/react-query")>();
  return {
    ...actual,
    useQueryClient: () => mockQueryClient,
  };
});

function makeClient() {
  return new QueryClient({ defaultOptions: { queries: { retry: false } } });
}

function renderHookWith<TResult>(render: () => TResult) {
  const client = makeClient();
  const wrapper = ({ children }: { children: React.ReactNode }) =>
    React.createElement(QueryClientProvider, { client }, children);
  return renderHook<TResult, unknown>(render, { wrapper });
}

describe("useAiPermissions (AI-04 §21)", () => {
  it("expõe canChat conforme a permissão ai:chat", () => {
    canMock.mockReturnValue(true);
    const { result } = renderHookWith(() => useAiPermissions());
    expect(result.current.canChat).toBe(true);

    canMock.mockReturnValue(false);
    const { result: denied } = renderHookWith(() => useAiPermissions());
    expect(denied.current.canChat).toBe(false);
  });

  it("mantém canSuggest existente", () => {
    canMock.mockReturnValue(true);
    const { result } = renderHookWith(() => useAiPermissions());
    expect(result.current.canSuggest).toBe(true);
  });
});

describe("useAiConversations (AI-04 §16)", () => {
  beforeEach(() => listMock.mockReset());

  it("busca conversas quando habilitado", async () => {
    listMock.mockResolvedValue([{ id: "conv-1", title: "Olá", screen: null, recordId: null }]);
    const { result } = renderHookWith(() => useAiConversations(true));
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(listMock).toHaveBeenCalled();
    expect(result.current.data?.[0].id).toBe("conv-1");
  });

  it("não busca quando desabilitado", () => {
    renderHookWith(() => useAiConversations(false));
    expect(listMock).not.toHaveBeenCalled();
  });
});

describe("useAiConversationMessages (AI-04 §16)", () => {
  beforeEach(() => messagesMock.mockReset());

  it("busca mensagens da conversa selecionada", async () => {
    messagesMock.mockResolvedValue([{ id: "m-1", role: "user", content: "Oi" }]);
    const { result } = renderHookWith(() => useAiConversationMessages("conv-1", true));
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(messagesMock).toHaveBeenCalledWith("conv-1");
  });

  it("não busca sem conversationId", () => {
    renderHookWith(() => useAiConversationMessages(null, true));
    expect(messagesMock).not.toHaveBeenCalled();
  });
});

describe("useAiChat (AI-04 §18-19)", () => {
  beforeEach(() => {
    chatMock.mockReset();
    toastErrorMock.mockReset();
  });

  it("post no /ai/chat e invalida a lista de conversas ao concluir", async () => {
    chatMock.mockResolvedValue({
      conversationId: "conv-1",
      message: "Resposta.",
      provider: "FAKE",
    });
    const { result } = renderHookWith(() => useAiChat());

    result.current.mutate({ message: "Oi", conversationId: null, context: null });

    await waitFor(() =>
      expect(chatMock).toHaveBeenCalledWith({ message: "Oi", conversationId: null, context: null }),
    );
    await waitFor(() =>
      expect(invalidateMock).toHaveBeenCalledWith({ queryKey: ["ai", "conversations"] }),
    );
  });

  it("exibe erro amigável em caso de falha", async () => {
    chatMock.mockRejectedValue(new Error("boom"));
    const { result } = renderHookWith(() => useAiChat());

    result.current.mutate({ message: "Oi", conversationId: null, context: null });

    await waitFor(() => expect(toastErrorMock).toHaveBeenCalledWith("Mensagem amigável."));
  });
});

describe("useAiAnalyze (AI-06)", () => {
  beforeEach(() => {
    analyzeMock.mockReset();
    toastErrorMock.mockReset();
  });

  it("chama POST /ai/analyze com pergunta e contexto", async () => {
    analyzeMock.mockResolvedValue({
      summary: "Resumo.",
      facts: [],
      inferences: [],
      recommendations: [],
    });
    const { result } = renderHookWith(() => useAiAnalyze());

    result.current.mutate({
      question: "Analise.",
      context: { screen: "opportunity", route: "/o/1", recordType: "OPPORTUNITY", recordId: "1" },
    });

    await waitFor(() =>
      expect(analyzeMock).toHaveBeenCalledWith({
        question: "Analise.",
        context: { screen: "opportunity", route: "/o/1", recordType: "OPPORTUNITY", recordId: "1" },
      }),
    );
    await waitFor(() => expect(result.current.data?.summary).toBe("Resumo."));
  });

  it("expõe o erro para tratamento inline (sem toast global)", async () => {
    analyzeMock.mockRejectedValue(new Error("boom"));
    const { result } = renderHookWith(() => useAiAnalyze());

    result.current.mutate({ question: "x", context: null });

    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(toastErrorMock).not.toHaveBeenCalled();
  });
});

describe("useAiConversationActions (AI-05)", () => {
  beforeEach(() => listActionsMock.mockReset());

  it("busca ações da conversa selecionada", async () => {
    listActionsMock.mockResolvedValue([{ id: "act-1", status: "PROPOSED" }]);
    const { result } = renderHookWith(() => useAiConversationActions("conv-1", true));
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(listActionsMock).toHaveBeenCalledWith("conv-1");
    expect(result.current.data?.[0].id).toBe("act-1");
  });

  it("não busca sem conversationId", () => {
    renderHookWith(() => useAiConversationActions(null, true));
    expect(listActionsMock).not.toHaveBeenCalled();
  });
});

describe("useAiConfirmAction / useAiCancelAction (AI-05)", () => {
  beforeEach(() => {
    confirmMock.mockReset();
    cancelMock.mockReset();
    invalidateMock.mockReset();
    toastErrorMock.mockReset();
  });

  it("confirma e invalida as ações da conversa", async () => {
    confirmMock.mockResolvedValue({ id: "act-1", status: "EXECUTED" });
    const { result } = renderHookWith(() => useAiConfirmAction("conv-1"));

    result.current.mutate("act-1");

    await waitFor(() => expect(confirmMock).toHaveBeenCalledWith("act-1"));
    await waitFor(() =>
      expect(invalidateMock).toHaveBeenCalledWith({
        queryKey: ["ai", "conversations", "conv-1", "actions"],
      }),
    );
  });

  it("cancela e invalida as ações da conversa", async () => {
    cancelMock.mockResolvedValue({ id: "act-1", status: "CANCELLED" });
    const { result } = renderHookWith(() => useAiCancelAction("conv-1"));

    result.current.mutate("act-1");

    await waitFor(() => expect(cancelMock).toHaveBeenCalledWith("act-1"));
    await waitFor(() =>
      expect(invalidateMock).toHaveBeenCalledWith({
        queryKey: ["ai", "conversations", "conv-1", "actions"],
      }),
    );
  });
});
