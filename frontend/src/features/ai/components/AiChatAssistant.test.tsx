import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { AiChatAssistant } from "./AiChatAssistant";

const {
  chatMutateMock,
  conversationsQuery,
  messagesQuery,
  actionsQuery,
  canChatMock,
  invalidateMock,
  state,
} = vi.hoisted(() => {
  const state = {
    isPending: false,
  };
  return {
    chatMutateMock: vi.fn(),
    conversationsQuery: vi.fn(),
    messagesQuery: vi.fn(),
    actionsQuery: vi.fn(),
    canChatMock: vi.fn(() => true),
    invalidateMock: vi.fn(),
    state,
  };
});

vi.mock("../hooks/useAi", () => ({
  useAiPermissions: () => ({ canSuggest: true, canChat: canChatMock() }),
  useAiChat: () => ({ mutate: chatMutateMock, isPending: state.isPending }),
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
}));

vi.mock("../hooks/useAiContext", () => ({
  useAiContext: () => null,
}));

vi.mock("@/features/auth/hooks/useAuth", () => ({
  useAuth: () => ({ user: { id: "u-1", companyId: "c-1" } }),
}));

beforeEach(() => {
  chatMutateMock.mockReset();
  conversationsQuery.mockReset();
  messagesQuery.mockReset();
  actionsQuery.mockReset();
  canChatMock.mockReset();
  invalidateMock.mockReset();
  canChatMock.mockReturnValue(true);
  state.isPending = false;
  conversationsQuery.mockReturnValue({ data: [], isLoading: false });
  messagesQuery.mockReturnValue({ data: undefined, isLoading: false });
  actionsQuery.mockReturnValue({ data: [] });
});

describe("AiChatAssistant (AI-04)", () => {
  it("renderiza o assistente com estado vazio (idle)", () => {
    render(<AiChatAssistant />);
    expect(screen.getByText("Assistente de IA")).toBeTruthy();
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
    expect(screen.getByText(/Assistente de IA indisponível/)).toBeTruthy();
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
