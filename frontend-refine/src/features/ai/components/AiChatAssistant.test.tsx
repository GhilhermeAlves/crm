import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { AiChatAssistant } from "./AiChatAssistant";
import type { AiContextPayload } from "../types/ai.types";

const {
  chatMutateMock,
  analyzeMutateMock,
  conversationsQuery,
  messagesQuery,
  actionsQuery,
  canChatMock,
  invalidateMock,
  useAiContextMock,
  state,
} = vi.hoisted(() => {
  const state = {
    isPending: false,
  };
  return {
    chatMutateMock: vi.fn(),
    analyzeMutateMock: vi.fn(),
    conversationsQuery: vi.fn(),
    messagesQuery: vi.fn(),
    actionsQuery: vi.fn(),
    canChatMock: vi.fn(() => true),
    invalidateMock: vi.fn(),
    useAiContextMock: vi.fn<() => AiContextPayload | null>(() => null),
    state,
  };
});

vi.mock("../hooks/useAi", () => ({
  useAiPermissions: () => ({ canSuggest: true, canChat: canChatMock() }),
  useAiChat: () => ({ mutate: chatMutateMock, isPending: state.isPending }),
  useAiAnalyze: () => ({ mutate: analyzeMutateMock, isPending: state.isPending }),
  useAiConversations: (enabled: boolean) => conversationsQuery(enabled),
  useAiConversationMessages: (id: string | null, enabled: boolean) => messagesQuery(id, enabled),
  useAiConversationActions: (id: string | null, enabled: boolean) => actionsQuery(id, enabled),
  useAiConfirmAction: () => ({ mutate: vi.fn(), isPending: false }),
  useAiCancelAction: () => ({ mutate: vi.fn(), isPending: false }),
}));

vi.mock("@tanstack/react-query", () => ({
  useQueryClient: () => ({ invalidateQueries: invalidateMock }),
}));

vi.mock("../services/ai.service", () => ({
  aiErrorMessage: () => "Não foi possível obter uma resposta da IA. Tente novamente.",
  aiAnalysisErrorMessage: () => "Não foi possível realizar a análise. Tente novamente.",
}));

vi.mock("../hooks/useAiContext", () => ({
  useAiContext: () => useAiContextMock(),
}));

vi.mock("@/features/auth/hooks/useAuth", () => ({
  useAuth: () => ({ user: { id: "u-1", companyId: "c-1" } }),
}));

beforeEach(() => {
  chatMutateMock.mockReset();
  analyzeMutateMock.mockReset();
  conversationsQuery.mockReset();
  messagesQuery.mockReset();
  actionsQuery.mockReset();
  canChatMock.mockReset();
  invalidateMock.mockReset();
  useAiContextMock.mockReset();
  useAiContextMock.mockReturnValue(null);
  canChatMock.mockReturnValue(true);
  state.isPending = false;
  conversationsQuery.mockReturnValue({ data: [], isLoading: false });
  messagesQuery.mockReturnValue({ data: undefined, isLoading: false });
  actionsQuery.mockReturnValue({ data: [] });
});

describe("AiChatAssistant (AI-04)", () => {
  it("renderiza o assistente com estado vazio (idle)", () => {
    render(<AiChatAssistant />);
    expect(screen.getByText("Léo")).toBeTruthy();
    expect(screen.getByText(/Pergunte sobre seus clientes/)).toBeTruthy();
  });

  it("envia mensagem e exibe a resposta da IA", async () => {
    let options: { onSuccess?: (res: unknown) => void } = {};
    chatMutateMock.mockImplementation((_req: unknown, opts: typeof options) => {
      options = opts;
    });

    render(<AiChatAssistant />);

    const input = screen.getByLabelText("Mensagem para o assistente");
    fireEvent.change(input, { target: { value: "Como está esse cliente?" } });
    fireEvent.click(screen.getByLabelText("Enviar mensagem"));

    expect(chatMutateMock).toHaveBeenCalledWith(
      { message: "Como está esse cliente?", conversationId: null, context: null },
      expect.any(Object),
    );

    // resposta do backend chega
    options.onSuccess?.({
      conversationId: "conv-1",
      message: "O cliente está ativo.",
      provider: "FAKE",
    });

    await waitFor(() => expect(screen.getByText("O cliente está ativo.")).toBeTruthy());
  });

  it("mantém o conversationId nas mensagens seguintes", async () => {
    let options: { onSuccess?: (res: unknown) => void } = {};
    chatMutateMock.mockImplementation((_req: unknown, opts: typeof options) => {
      options = opts;
    });

    render(<AiChatAssistant />);

    const input = screen.getByLabelText("Mensagem para o assistente");
    fireEvent.change(input, { target: { value: "primeira" } });
    fireEvent.click(screen.getByLabelText("Enviar mensagem"));
    options.onSuccess?.({ conversationId: "conv-1", message: "ok", provider: "FAKE" });

    await waitFor(() => expect(screen.getByText("ok")).toBeTruthy());

    fireEvent.change(input, { target: { value: "segunda" } });
    fireEvent.click(screen.getByLabelText("Enviar mensagem"));

    expect(chatMutateMock).toHaveBeenLastCalledWith(
      { message: "segunda", conversationId: "conv-1", context: null },
      expect.any(Object),
    );
  });

  it("mostra estado de processamento (loading) e mensagem de análise", () => {
    let options: { onSuccess?: (res: unknown) => void } = {};
    chatMutateMock.mockImplementation((_req: unknown, opts: typeof options) => {
      options = opts;
    });

    render(<AiChatAssistant />);

    const input = screen.getByLabelText("Mensagem para o assistente");
    fireEvent.change(input, { target: { value: "quem é o cliente?" } });
    fireEvent.click(screen.getByLabelText("Enviar mensagem"));

    // durante o processamento a bolha "IA está analisando..." deve aparecer
    expect(screen.getByText(/IA está analisando/)).toBeTruthy();
  });

  it("trata erro de forma amigável (sem stack trace)", async () => {
    let options: { onError?: (error: Error) => void } = {};
    chatMutateMock.mockImplementation((_req: unknown, opts: typeof options) => {
      options = opts;
    });

    render(<AiChatAssistant />);

    const input = screen.getByLabelText("Mensagem para o assistente");
    fireEvent.change(input, { target: { value: "oi" } });
    fireEvent.click(screen.getByLabelText("Enviar mensagem"));

    options.onError?.(new Error("erro interno com stack trace"));

    await waitFor(() =>
      expect(
        screen.getByText("Não foi possível obter uma resposta da IA. Tente novamente."),
      ).toBeTruthy(),
    );
    expect(screen.queryByText(/stack trace/i)).toBeNull();
  });

  it("nova conversa limpa o estado e zera o conversationId", () => {
    let options: { onSuccess?: (res: unknown) => void } = {};
    chatMutateMock.mockImplementation((_req: unknown, opts: typeof options) => {
      options = opts;
    });

    render(<AiChatAssistant />);

    const input = screen.getByLabelText("Mensagem para o assistente");
    fireEvent.change(input, { target: { value: "oi" } });
    fireEvent.click(screen.getByLabelText("Enviar mensagem"));
    options.onSuccess?.({ conversationId: "conv-1", message: "ok", provider: "FAKE" });

    // clica em nova conversa (existem dois botões: header + sidebar)
    const newButtons = screen.getAllByText("Nova conversa");
    fireEvent.click(newButtons[0]);

    expect(screen.queryByText("ok")).toBeNull();
    expect(screen.getByText(/Pergunte sobre seus clientes/)).toBeTruthy();
  });

  it("bloqueia novo envio enquanto processa", () => {
    let options: { onSuccess?: (res: unknown) => void } = {};
    chatMutateMock.mockImplementation((_req: unknown, opts: typeof options) => {
      options = opts;
    });
    state.isPending = true;

    render(<AiChatAssistant />);

    const input = screen.getByLabelText("Mensagem para o assistente");
    fireEvent.change(input, { target: { value: "oi" } });
    fireEvent.click(screen.getByLabelText("Enviar mensagem"));

    expect(chatMutateMock).not.toHaveBeenCalled();
  });

  it("exibe acesso negado sem a permissão ai:chat", () => {
    canChatMock.mockReturnValue(false);
    render(<AiChatAssistant />);
    expect(screen.getByText(/Léo indisponível/)).toBeTruthy();
    expect(screen.getByText(/ai:chat/)).toBeTruthy();
  });

  it("carrega o histórico ao selecionar uma conversa", async () => {
    conversationsQuery.mockReturnValue({
      data: [
        {
          id: "conv-1",
          title: "Como está o cliente?",
          screen: "customer360",
          recordId: null,
          createdAt: "2026-08-19T00:00:00",
          updatedAt: "2026-08-19T00:00:00",
        },
      ],
      isLoading: false,
    });

    messagesQuery.mockReturnValue({
      data: [
        {
          id: "m-1",
          conversationId: "conv-1",
          role: "user",
          content: "Como está o cliente?",
          createdAt: "2026-08-19T00:00:00",
        },
        {
          id: "m-2",
          conversationId: "conv-1",
          role: "assistant",
          content: "Ativo.",
          createdAt: "2026-08-19T00:00:00",
        },
      ],
      isLoading: false,
    });

    render(<AiChatAssistant />);

    fireEvent.click(screen.getAllByText("Como está o cliente?")[0]);

    await waitFor(() => expect(screen.getByText("Ativo.")).toBeTruthy());
  });

  it("renderiza proposta de ação pendente (AI-05)", async () => {
    conversationsQuery.mockReturnValue({
      data: [],
      isLoading: false,
    });
    actionsQuery.mockReturnValue({
      data: [
        {
          id: "act-1",
          conversationId: "conv-1",
          tool: "create_task",
          entityType: "TASK",
          entityId: null,
          description: "Criar tarefa: Ligar para Joao",
          status: "PROPOSED",
          parameters: { title: "Ligar para Joao" },
          result: null,
          errorMessage: null,
          createdAt: "2026-08-19T00:00:00",
          updatedAt: "2026-08-19T00:00:00",
        },
      ],
    });

    render(<AiChatAssistant />);

    await waitFor(() => expect(screen.getByText(/deseja executar esta ação/)).toBeTruthy());
    expect(screen.getByText("Criar tarefa: Ligar para Joao")).toBeTruthy();
    expect(screen.getByLabelText("Confirmar ação")).toBeTruthy();
    expect(screen.getByLabelText("Cancelar ação")).toBeTruthy();
  });
});

describe("AiChatAssistant análise contextual (AI-06)", () => {
  const ctx = {
    screen: "opportunity",
    route: "/opportunities/123",
    recordType: "OPPORTUNITY" as const,
    recordId: "123",
  };

  it("envia o contexto correto para POST /analyze ao clicar em Analisar", async () => {
    useAiContextMock.mockReturnValue(ctx);
    let options: { onSuccess?: (res: unknown) => void } = {};
    analyzeMutateMock.mockImplementation((_req: unknown, opts: typeof options) => {
      options = opts;
    });

    render(<AiChatAssistant />);

    fireEvent.click(screen.getByLabelText("Analisar o registro atual"));

    expect(analyzeMutateMock).toHaveBeenCalledWith(
      {
        question: "Analise este registro e sugira próximas ações.",
        context: ctx,
      },
      expect.any(Object),
    );

    options.onSuccess?.({
      summary: "Resumo.",
      facts: [{ key: "f", label: "Estágio", value: "Proposta", source: "x" }],
      inferences: [{ key: "i", text: "Inferência.", confidence: 70 }],
      recommendations: [
        {
          key: "r",
          title: "Follow-up",
          description: null,
          priority: 80,
          justification: null,
          action: null,
        },
      ],
    });

    await waitFor(() => expect(screen.getByText("Resumo.")).toBeTruthy());
    expect(screen.getByLabelText("Fatos")).toBeTruthy();
    expect(screen.getByLabelText("Inferências")).toBeTruthy();
    expect(screen.getByLabelText("Recomendações")).toBeTruthy();
  });

  it("bloqueia duplo clique e mostra loading durante a análise", () => {
    useAiContextMock.mockReturnValue(ctx);
    let options: { onSuccess?: (res: unknown) => void } = {};
    analyzeMutateMock.mockImplementation((_req: unknown, opts: typeof options) => {
      options = opts;
    });

    const { rerender } = render(<AiChatAssistant />);

    fireEvent.click(screen.getByLabelText("Analisar o registro atual"));
    // simula a requisição em andamento (isPending do react-query) e re-renderiza
    state.isPending = true;
    rerender(<AiChatAssistant />);

    const button = screen.getByLabelText("Analisar o registro atual");
    expect((button as HTMLButtonElement).disabled).toBe(true);
    fireEvent.click(button);

    // guard de estado + botão desabilitado impedem a segunda chamada
    expect(analyzeMutateMock).toHaveBeenCalledTimes(1);
    expect(screen.getByLabelText("Analisando registro")).toBeTruthy();
  });

  it("desabilita Analisar sem registro em foco (contexto não suportado)", () => {
    render(<AiChatAssistant />);
    const button = screen.getByLabelText(/Analisar o registro atual \(nenhum registro em foco\)/);
    expect((button as HTMLButtonElement).disabled).toBe(true);
    expect(analyzeMutateMock).not.toHaveBeenCalled();
  });

  it("exibe erro amigável quando a análise falha (sem stack trace)", async () => {
    useAiContextMock.mockReturnValue(ctx);
    let options: { onError?: (error: Error) => void } = {};
    analyzeMutateMock.mockImplementation((_req: unknown, opts: typeof options) => {
      options = opts;
    });

    render(<AiChatAssistant />);

    fireEvent.click(screen.getByLabelText("Analisar o registro atual"));
    options.onError?.(new Error("erro interno stack trace"));

    await waitFor(() =>
      expect(
        screen.getByText("Não foi possível realizar a análise. Tente novamente."),
      ).toBeTruthy(),
    );
    expect(screen.queryByText(/stack trace/)).toBeNull();
  });

  it("não confirma nenhuma ação automaticamente após a análise", async () => {
    useAiContextMock.mockReturnValue(ctx);
    let options: { onSuccess?: (res: unknown) => void } = {};
    analyzeMutateMock.mockImplementation((_req: unknown, opts: typeof options) => {
      options = opts;
    });

    render(<AiChatAssistant />);

    fireEvent.click(screen.getByLabelText("Analisar o registro atual"));
    options.onSuccess?.({
      summary: "Resumo.",
      facts: [],
      inferences: [],
      recommendations: [
        {
          key: "r",
          title: "Follow-up",
          description: null,
          priority: 80,
          justification: null,
          action: "create_task",
        },
      ],
    });

    await waitFor(() => expect(screen.getByText("Resumo.")).toBeTruthy());
    // nenhum botão de confirmação de Write Tool (AI-05) deve surgir da análise
    expect(screen.queryByLabelText("Confirmar ação")).toBeNull();
    expect(screen.queryByLabelText("Cancelar ação")).toBeNull();
  });
});
