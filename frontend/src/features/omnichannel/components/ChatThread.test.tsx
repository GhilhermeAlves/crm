import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { ChatThread } from "./ChatThread";
import type { ConversationDetail } from "../types/omnichannel.types";

if (!Element.prototype.scrollIntoView) {
  Element.prototype.scrollIntoView = vi.fn();
}

const {
  canSuggestMock,
  canTakeoverMock,
  canFollowUpReadMock,
  canFollowUpManageMock,
  suggestMutateMock,
  takeoverMutateMock,
  releaseMutateMock,
  followUpsDataMock,
  createFollowUpMutateMock,
  cancelFollowUpMutateMock,
} = vi.hoisted(() => ({
  canSuggestMock: vi.fn(() => true),
  canTakeoverMock: vi.fn(() => true),
  canFollowUpReadMock: vi.fn(() => true),
  canFollowUpManageMock: vi.fn(() => true),
  suggestMutateMock: vi.fn(),
  takeoverMutateMock: vi.fn(),
  releaseMutateMock: vi.fn(),
  followUpsDataMock: vi.fn(),
  createFollowUpMutateMock: vi.fn(),
  cancelFollowUpMutateMock: vi.fn(),
}));

vi.mock("@/features/ai/hooks/useAi", () => ({
  useSuggestReply: () => ({ mutate: suggestMutateMock, isPending: false }),
  useAiPermissions: () => ({ canSuggest: canSuggestMock() }),
}));

vi.mock("../hooks/useOmnichannel", () => ({
  useOmnichannelPermissions: () => ({
    canRead: true,
    canSend: true,
    canCreate: true,
    canUpdate: true,
    canDelete: true,
    canTakeover: canTakeoverMock(),
    canFollowUpRead: canFollowUpReadMock(),
    canFollowUpManage: canFollowUpManageMock(),
  }),
  useTakeoverConversation: () => ({ mutate: takeoverMutateMock, isPending: false }),
  useReleaseConversation: () => ({ mutate: releaseMutateMock, isPending: false }),
  useConversationFollowUps: () => ({ data: followUpsDataMock() }),
  useCreateFollowUp: () => ({ mutate: createFollowUpMutateMock, isPending: false }),
  useCancelFollowUp: () => ({ mutate: cancelFollowUpMutateMock, isPending: false }),
}));

function detail(mode: ConversationDetail["mode"]): ConversationDetail {
  return {
    id: "conv-1",
    channelId: "ch-1",
    contactId: "ct-1",
    externalPhone: "+5511999998888",
    status: "OPEN",
    mode,
    lastMessageAt: "2026-09-05T10:00:00",
    unreadCount: 0,
    messages: {
      content: [
        {
          id: "m-1",
          conversationId: "conv-1",
          direction: "INBOUND",
          senderPhone: "+5511999998888",
          recipientPhone: null,
          type: "TEXT",
          body: "Qual o horário de atendimento?",
          status: "SENT",
          externalMessageId: "wamid-1",
          providerError: null,
          sentAt: "2026-09-05T10:00:00",
          createdAt: "2026-09-05T10:00:00",
        },
      ],
      page: 0,
      pageSize: 30,
      totalElements: 1,
      totalPages: 1,
    },
  };
}

describe("ChatThread (Sprint 3 - Human Takeover / Sprint 4 - Follow-ups)", () => {
  beforeEach(() => {
    canSuggestMock.mockReset();
    canTakeoverMock.mockReset();
    canFollowUpReadMock.mockReset();
    canFollowUpManageMock.mockReset();
    suggestMutateMock.mockReset();
    takeoverMutateMock.mockReset();
    releaseMutateMock.mockReset();
    followUpsDataMock.mockReset();
    createFollowUpMutateMock.mockReset();
    cancelFollowUpMutateMock.mockReset();
    canSuggestMock.mockReturnValue(true);
    canTakeoverMock.mockReturnValue(true);
    canFollowUpReadMock.mockReturnValue(true);
    canFollowUpManageMock.mockReturnValue(true);
    followUpsDataMock.mockReturnValue(undefined);
  });

  it("mostra estado vazio quando nenhuma conversa é selecionada", () => {
    render(
      <ChatThread
        detail={undefined}
        isLoading={false}
        canSend={true}
        onSend={vi.fn()}
        sending={false}
      />,
    );
    expect(screen.getByText("Selecione uma conversa")).toBeTruthy();
  });

  it("mostra botão Assumir manualmente em modo AUTOMATIC e chama takeover", () => {
    render(
      <ChatThread
        detail={detail("AUTOMATIC")}
        isLoading={false}
        canSend={true}
        onSend={vi.fn()}
        sending={false}
      />,
    );

    const button = screen.getByRole("button", { name: /Assumir manualmente/i });
    fireEvent.click(button);

    expect(takeoverMutateMock).toHaveBeenCalledWith("conv-1");
    expect(releaseMutateMock).not.toHaveBeenCalled();
    expect(screen.queryByText("Atendimento humano")).toBeNull();
  });

  it("mostra badge de atendimento humano e botão Retomar IA em modo HUMAN", () => {
    render(
      <ChatThread
        detail={detail("HUMAN")}
        isLoading={false}
        canSend={true}
        onSend={vi.fn()}
        sending={false}
      />,
    );

    expect(screen.getByText("Atendimento humano")).toBeTruthy();
    const button = screen.getByRole("button", { name: /Retomar IA/i });
    fireEvent.click(button);

    expect(releaseMutateMock).toHaveBeenCalledWith("conv-1");
    expect(takeoverMutateMock).not.toHaveBeenCalled();
  });

  it("não mostra o botão de takeover sem a permissão omnichannel:takeover", () => {
    canTakeoverMock.mockReturnValue(false);
    render(
      <ChatThread
        detail={detail("AUTOMATIC")}
        isLoading={false}
        canSend={true}
        onSend={vi.fn()}
        sending={false}
      />,
    );

    expect(screen.queryByRole("button", { name: /Assumir manualmente/i })).toBeNull();
  });
});

describe("ChatThread (Sprint 4 - Follow-ups)", () => {
  beforeEach(() => {
    canFollowUpReadMock.mockReset();
    canFollowUpManageMock.mockReset();
    followUpsDataMock.mockReset();
    createFollowUpMutateMock.mockReset();
    cancelFollowUpMutateMock.mockReset();
    canFollowUpReadMock.mockReturnValue(true);
    canFollowUpManageMock.mockReturnValue(true);
    followUpsDataMock.mockReturnValue(undefined);
  });

  it("mostra estado vazio quando não há follow-ups agendados", () => {
    render(
      <ChatThread
        detail={detail("AUTOMATIC")}
        isLoading={false}
        canSend={true}
        onSend={vi.fn()}
        sending={false}
      />,
    );

    expect(screen.getByText("Nenhum follow-up agendado.")).toBeTruthy();
  });

  it("mostra o próximo follow-up pendente e permite cancelar", () => {
    followUpsDataMock.mockReturnValue({
      content: [
        {
          id: "fu-1",
          conversationId: "conv-1",
          status: "PENDING",
          actionType: "SEND_MESSAGE",
          actionContent: "Só passando pra confirmar horário…",
          executeAt: "2030-01-01T10:00:00",
          attempts: 0,
          lastError: null,
          resultText: null,
          cancelledAt: null,
          cancelledReason: null,
          createdAt: "2026-09-06T09:00:00",
          updatedAt: "2026-09-06T09:00:00",
        },
      ],
      page: 0,
      pageSize: 30,
      totalElements: 1,
      totalPages: 1,
    });
    render(
      <ChatThread
        detail={detail("AUTOMATIC")}
        isLoading={false}
        canSend={true}
        onSend={vi.fn()}
        sending={false}
      />,
    );

    expect(screen.getByText("Próximo follow-up")).toBeTruthy();
    expect(screen.getByText(/Só passando pra confirmar horário/)).toBeTruthy();

    fireEvent.click(screen.getByRole("button", { name: /Cancelar/i }));
    expect(cancelFollowUpMutateMock).toHaveBeenCalledWith("fu-1");
  });

  it("desabilita o agendamento de follow-up em atendimento humano", () => {
    render(
      <ChatThread
        detail={detail("HUMAN")}
        isLoading={false}
        canSend={true}
        onSend={vi.fn()}
        sending={false}
      />,
    );

    const button = screen.getByRole("button", { name: /Agendar follow-up/i }) as HTMLButtonElement;
    expect(button.disabled).toBe(true);
    expect(button.title).toContain("Follow-ups automáticos ficam suspensos");
  });

  it("não mostra o botão de agendamento sem a permissão omnichannel:followup", () => {
    canFollowUpManageMock.mockReturnValue(false);
    render(
      <ChatThread
        detail={detail("AUTOMATIC")}
        isLoading={false}
        canSend={true}
        onSend={vi.fn()}
        sending={false}
      />,
    );

    expect(screen.queryByRole("button", { name: /Agendar follow-up/i })).toBeNull();
  });

  it("não mostra o painel de follow-ups sem nenhuma permissão de follow-up", () => {
    canFollowUpReadMock.mockReturnValue(false);
    canFollowUpManageMock.mockReturnValue(false);
    render(
      <ChatThread
        detail={detail("AUTOMATIC")}
        isLoading={false}
        canSend={true}
        onSend={vi.fn()}
        sending={false}
      />,
    );

    expect(screen.queryByText("Nenhum follow-up agendado.")).toBeNull();
    expect(screen.queryByText("Próximo follow-up")).toBeNull();
  });

  it("ageenda um follow-up pelo diálogo com data/hora normalizada", () => {
    render(
      <ChatThread
        detail={detail("AUTOMATIC")}
        isLoading={false}
        canSend={true}
        onSend={vi.fn()}
        sending={false}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: /Agendar follow-up/i }));

    fireEvent.change(screen.getByLabelText("Data e hora do envio"), {
      target: { value: "2030-01-01T10:00" },
    });
    fireEvent.change(screen.getByLabelText("Mensagem do follow-up"), {
      target: { value: "Seguir com o contato" },
    });
    fireEvent.click(screen.getByRole("button", { name: /^Agendar$/i }));

    expect(createFollowUpMutateMock).toHaveBeenCalledTimes(1);
    expect(createFollowUpMutateMock.mock.calls[0][0]).toEqual({
      conversationId: "conv-1",
      content: "Seguir com o contato",
      executeAt: "2030-01-01T10:00",
    });
  });
});
