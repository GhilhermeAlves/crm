import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import * as React from "react";
import {
  useOmnichannelPermissions,
  useConversationFollowUps,
  useCreateFollowUp,
  useCancelFollowUp,
} from "./useOmnichannel";

const {
  canMock,
  listFollowUpsMock,
  createFollowUpMock,
  cancelFollowUpMock,
  toastErrorMock,
  toastSuccessMock,
  invalidateMock,
} = vi.hoisted(() => ({
  canMock: vi.fn(),
  listFollowUpsMock: vi.fn(),
  createFollowUpMock: vi.fn(),
  cancelFollowUpMock: vi.fn(),
  toastErrorMock: vi.fn(),
  toastSuccessMock: vi.fn(),
  invalidateMock: vi.fn(),
}));

vi.mock("@/features/auth/hooks/useAuthorization", () => ({
  useAuthorization: () => ({ can: canMock }),
}));

vi.mock("../services/omnichannel.service", () => ({
  OmnichannelService: {
    listChannels: vi.fn(),
    createChannel: vi.fn(),
    updateChannel: vi.fn(),
    setChannelStatus: vi.fn(),
    deleteChannel: vi.fn(),
    listConversations: vi.fn(),
    getConversation: vi.fn(),
    sendMessage: vi.fn(),
    markRead: vi.fn(),
    takeover: vi.fn(),
    release: vi.fn(),
    listFollowUps: listFollowUpsMock,
    createFollowUp: createFollowUpMock,
    getFollowUp: vi.fn(),
    cancelFollowUp: cancelFollowUpMock,
  },
}));

vi.mock("sonner", () => ({
  toast: { error: toastErrorMock, success: toastSuccessMock },
}));

vi.mock("@tanstack/react-query", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@tanstack/react-query")>();
  return {
    ...actual,
    useQueryClient: () => ({ invalidateQueries: invalidateMock }),
  };
});

function renderHookWith<TResult>(render: () => TResult) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const wrapper = ({ children }: { children: React.ReactNode }) =>
    React.createElement(QueryClientProvider, { client }, children);
  return renderHook<TResult, unknown>(render, { wrapper });
}

const followUp = {
  id: "fu-1",
  conversationId: "conv-1",
  status: "PENDING",
  actionType: "SEND_MESSAGE",
  actionContent: "Só passando pra confirmar…",
  executeAt: "2030-01-01T10:00:00",
  attempts: 0,
  lastError: null,
  resultText: null,
  cancelledAt: null,
  cancelledReason: null,
  createdAt: "2026-09-06T09:00:00",
  updatedAt: "2026-09-06T09:00:00",
} as const;

describe("useOmnichannelPermissions (Sprint 4 - Follow-ups)", () => {
  beforeEach(() => {
    canMock.mockReset();
    toastErrorMock.mockReset();
    toastSuccessMock.mockReset();
    invalidateMock.mockReset();
  });

  it("expõe canFollowUpRead conforme a permissão omnichannel:followup:read", () => {
    canMock.mockReturnValue(true);
    const { result } = renderHookWith(() => useOmnichannelPermissions());
    expect(result.current.canFollowUpRead).toBe(true);

    canMock.mockReturnValue(false);
    const { result: denied } = renderHookWith(() => useOmnichannelPermissions());
    expect(denied.current.canFollowUpRead).toBe(false);
  });

  it("expõe canFollowUpManage conforme a permissão omnichannel:followup", () => {
    canMock.mockReturnValue(true);
    const { result } = renderHookWith(() => useOmnichannelPermissions());
    expect(result.current.canFollowUpManage).toBe(true);

    canMock.mockReturnValue(false);
    const { result: denied } = renderHookWith(() => useOmnichannelPermissions());
    expect(denied.current.canFollowUpManage).toBe(false);
  });
});

describe("useConversationFollowUps (Sprint 4)", () => {
  beforeEach(() => {
    listFollowUpsMock.mockReset();
    canMock.mockReset();
  });

  it("lista os follow-ups da conversa apenas quando há conversationId", async () => {
    listFollowUpsMock.mockResolvedValue({
      content: [followUp],
      page: 0,
      pageSize: 30,
      totalElements: 1,
      totalPages: 1,
    });

    const { result } = renderHookWith(() => useConversationFollowUps("conv-1"));
    await waitFor(() => expect(result.current.data?.content).toHaveLength(1));
    expect(listFollowUpsMock).toHaveBeenCalledWith("conv-1", 0, 30);
  });

  it("não dispara a requisição com conversationId nulo", () => {
    renderHookWith(() => useConversationFollowUps(null));
    expect(listFollowUpsMock).not.toHaveBeenCalled();
  });
});

describe("useCreateFollowUp (Sprint 4)", () => {
  beforeEach(() => {
    createFollowUpMock.mockReset();
    toastErrorMock.mockReset();
    toastSuccessMock.mockReset();
    invalidateMock.mockReset();
  });

  it("cria o follow-up, invalida o cache e notifica sucesso", async () => {
    createFollowUpMock.mockResolvedValue(followUp);

    const { result } = renderHookWith(() => useCreateFollowUp("conv-1"));
    result.current.mutate({
      conversationId: "conv-1",
      content: "Só passando pra confirmar…",
      executeAt: "2030-01-01T10:00:00",
    });
    await waitFor(() => expect(createFollowUpMock).toHaveBeenCalled());

    expect(invalidateMock).toHaveBeenCalledWith({
      queryKey: ["omnichannel", "conversation", "conv-1", "follow-ups"],
    });
    expect(toastSuccessMock).toHaveBeenCalledWith("Follow-up agendado");
  });

  it("notifica erro quando o agendamento falha", async () => {
    createFollowUpMock.mockRejectedValue(new Error("atendimento humano ativo"));

    const { result } = renderHookWith(() => useCreateFollowUp("conv-1"));
    result.current.mutate({
      conversationId: "conv-1",
      content: "Mensagem",
      executeAt: "2030-01-01T10:00:00",
    });
    await waitFor(() => expect(createFollowUpMock).toHaveBeenCalled());

    expect(toastErrorMock).toHaveBeenCalledWith(
      expect.stringContaining("atendimento humano ativo"),
    );
  });
});

describe("useCancelFollowUp (Sprint 4)", () => {
  beforeEach(() => {
    cancelFollowUpMock.mockReset();
    toastErrorMock.mockReset();
    toastSuccessMock.mockReset();
    invalidateMock.mockReset();
  });

  it("cancela o follow-up, invalida o cache e notifica sucesso", async () => {
    cancelFollowUpMock.mockResolvedValue(followUp);

    const { result } = renderHookWith(() => useCancelFollowUp("conv-1"));
    result.current.mutate("fu-1");
    await waitFor(() => expect(cancelFollowUpMock).toHaveBeenCalledWith("fu-1"));

    expect(invalidateMock).toHaveBeenCalledWith({
      queryKey: ["omnichannel", "conversation", "conv-1", "follow-ups"],
    });
    expect(toastSuccessMock).toHaveBeenCalledWith("Follow-up cancelado");
  });
});
