import { describe, it, expect, vi, beforeEach } from "vitest";
import { AxiosError, AxiosHeaders, type AxiosResponse } from "axios";
import { AiService, aiErrorMessage } from "./ai.service";

const { getMock, postMock } = vi.hoisted(() => ({
  getMock: vi.fn(),
  postMock: vi.fn(),
}));

vi.mock("@/lib/api", () => ({
  default: { get: getMock, post: postMock },
}));

describe("AiService (AI-04)", () => {
  beforeEach(() => {
    getMock.mockReset();
    postMock.mockReset();
  });

  it("envia mensagem sem conversationId (nova conversa)", async () => {
    postMock.mockResolvedValue({
      data: { conversationId: "conv-1", message: "Resposta.", provider: "FAKE" },
    });

    const res = await AiService.chat({
      message: "Como está esse cliente?",
      conversationId: null,
      context: {
        screen: "customer360",
        route: "/customers/123",
        recordType: "CUSTOMER",
        recordId: "123",
      },
    });

    expect(postMock).toHaveBeenCalledWith(
      "/ai/chat",
      expect.objectContaining({ message: "Como está esse cliente?", conversationId: null }),
    );
    expect(res.conversationId).toBe("conv-1");
    expect(res.provider).toBe("FAKE");
  });

  it("envia mensagem continuando uma conversa existente", async () => {
    postMock.mockResolvedValue({
      data: { conversationId: "conv-1", message: "Mais detalhes.", provider: "FAKE" },
    });

    await AiService.chat({ message: "e os contatos?", conversationId: "conv-1", context: null });

    expect(postMock).toHaveBeenCalledWith(
      "/ai/chat",
      expect.objectContaining({
        message: "e os contatos?",
        conversationId: "conv-1",
        context: null,
      }),
    );
  });

  it("lista conversas do usuário", async () => {
    getMock.mockResolvedValue({
      data: [
        {
          id: "conv-1",
          title: "Como está o cliente?",
          screen: "customer360",
          recordId: "123",
          createdAt: "2026-08-19T00:00:00",
          updatedAt: "2026-08-19T00:00:00",
        },
      ],
    });

    const res = await AiService.listConversations();

    expect(getMock).toHaveBeenCalledWith("/ai/conversations");
    expect(res).toHaveLength(1);
    expect(res[0].title).toBe("Como está o cliente?");
  });

  it("lista mensagens de uma conversa", async () => {
    getMock.mockResolvedValue({
      data: [
        {
          id: "m-1",
          conversationId: "conv-1",
          role: "user",
          content: "Oi",
          createdAt: "2026-08-19T00:00:00",
        },
      ],
    });

    const res = await AiService.getConversationMessages("conv-1");

    expect(getMock).toHaveBeenCalledWith("/ai/conversations/conv-1/messages");
    expect(res[0].role).toBe("user");
  });
});

describe("aiErrorMessage (AI-04 §22)", () => {
  function axiosError(status: number | null): AxiosError {
    const config = { headers: new AxiosHeaders() };
    if (status === null) {
      return new AxiosError("Network Error", "ERR_NETWORK", config);
    }
    const response: AxiosResponse = {
      data: { message: "erro" },
      status,
      statusText: "",
      headers: {},
      config,
    };
    return new AxiosError("Request failed", undefined, config, undefined, response);
  }

  it("mapeia 401 para sessão expirada", () => {
    expect(aiErrorMessage(axiosError(401))).toContain("sessão");
  });

  it("mapeia 403 para falta de permissão", () => {
    expect(aiErrorMessage(axiosError(403))).toContain("permissão");
  });

  it("mapeia 404 para conversa não encontrada", () => {
    expect(aiErrorMessage(axiosError(404))).toContain("não encontrada");
  });

  it("mapeia 429 para excesso de solicitações", () => {
    expect(aiErrorMessage(axiosError(429))).toContain("Muitas solicitações");
  });

  it("mapeia 500 para erro genérico da IA", () => {
    expect(aiErrorMessage(axiosError(500))).toContain("Tente novamente");
  });

  it("mapeia 502 para provedor indisponível", () => {
    expect(aiErrorMessage(axiosError(502))).toContain("provedor");
  });

  it("mapeia erro de rede (sem response) para falha de conexão", () => {
    expect(aiErrorMessage(axiosError(null))).toContain("conexão");
  });

  it("cai em fallback amigável para erros desconhecidos", () => {
    expect(aiErrorMessage(new Error("stack trace"))).toBe(
      "Não foi possível obter uma resposta da IA. Tente novamente.",
    );
  });
});

describe("AiService actions (AI-05)", () => {
  beforeEach(() => {
    getMock.mockReset();
    postMock.mockReset();
  });

  it("lista ações de uma conversa", async () => {
    const action = {
      id: "act-1",
      conversationId: "conv-1",
      tool: "create_task",
      entityType: "TASK",
      entityId: null,
      description: "Criar tarefa: Ligar",
      status: "PROPOSED",
      parameters: { title: "Ligar" },
      result: null,
      errorMessage: null,
      createdAt: "2026-01-01T10:00:00",
      updatedAt: "2026-01-01T10:00:00",
    };
    getMock.mockResolvedValue({ data: [action] });

    const res = await AiService.listConversationActions("conv-1");

    expect(getMock).toHaveBeenCalledWith("/ai/conversations/conv-1/actions");
    expect(res).toEqual([action]);
  });

  it("confirma uma ação", async () => {
    postMock.mockResolvedValue({
      data: { id: "act-1", conversationId: "conv-1", status: "EXECUTED" },
    });

    const res = await AiService.confirmAction("act-1");

    expect(postMock).toHaveBeenCalledWith("/ai/actions/act-1/confirm");
    expect(res.status).toBe("EXECUTED");
  });

  it("cancela uma ação", async () => {
    postMock.mockResolvedValue({
      data: { id: "act-1", conversationId: "conv-1", status: "CANCELLED" },
    });

    const res = await AiService.cancelAction("act-1");

    expect(postMock).toHaveBeenCalledWith("/ai/actions/act-1/cancel");
    expect(res.status).toBe("CANCELLED");
  });
});
