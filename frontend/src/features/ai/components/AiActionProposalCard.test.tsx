import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { AiActionProposalCard } from "./AiActionProposalCard";
import type { AiAction } from "../types/ai.types";

const { confirmMutateMock, cancelMutateMock, pendingState } = vi.hoisted(() => {
  const pendingState = { confirm: false, cancel: false };
  return {
    confirmMutateMock: vi.fn(),
    cancelMutateMock: vi.fn(),
    pendingState,
  };
});

vi.mock("../hooks/useAi", () => ({
  useAiConfirmAction: () => ({ mutate: confirmMutateMock, isPending: pendingState.confirm }),
  useAiCancelAction: () => ({ mutate: cancelMutateMock, isPending: pendingState.cancel }),
}));

vi.mock("@tanstack/react-query", () => ({
  useQueryClient: () => ({ invalidateQueries: vi.fn() }),
}));

const base: AiAction = {
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
  createdAt: "2026-08-19T00:00:00",
  updatedAt: "2026-08-19T00:00:00",
};

beforeEach(() => {
  confirmMutateMock.mockReset();
  cancelMutateMock.mockReset();
  pendingState.confirm = false;
  pendingState.cancel = false;
});

describe("AiActionProposalCard (AI-05)", () => {
  it("exibe descrição e botões Confirmar/Cancelar para PROPOSED", () => {
    render(<AiActionProposalCard action={base} />);

    expect(screen.getByText(/deseja executar esta ação/)).toBeTruthy();
    expect(screen.getByText("Criar tarefa: Ligar")).toBeTruthy();
    expect(screen.getByLabelText("Confirmar ação")).toBeTruthy();
    expect(screen.getByLabelText("Cancelar ação")).toBeTruthy();
  });

  it("confirma a ação ao clicar em Confirmar", () => {
    render(<AiActionProposalCard action={base} />);

    fireEvent.click(screen.getByLabelText("Confirmar ação"));

    expect(confirmMutateMock).toHaveBeenCalledWith("act-1");
  });

  it("cancela a ação ao clicar em Cancelar", () => {
    render(<AiActionProposalCard action={base} />);

    fireEvent.click(screen.getByLabelText("Cancelar ação"));

    expect(cancelMutateMock).toHaveBeenCalledWith("act-1");
  });

  it("desabilita os botões enquanto uma ação está em andamento (prevenção de duplo clique)", () => {
    pendingState.confirm = true;
    render(<AiActionProposalCard action={base} />);

    fireEvent.click(screen.getByLabelText("Confirmar ação"));

    expect(confirmMutateMock).not.toHaveBeenCalled();
    expect((screen.getByLabelText("Confirmar ação") as HTMLButtonElement).disabled).toBe(true);
    expect((screen.getByLabelText("Cancelar ação") as HTMLButtonElement).disabled).toBe(true);
  });

  it("exibe estado executado sem botões", () => {
    render(<AiActionProposalCard action={{ ...base, status: "EXECUTED" }} />);

    expect(screen.getByText("Ação executada")).toBeTruthy();
    expect(screen.queryByLabelText("Confirmar ação")).toBeNull();
  });

  it("exibe estado cancelado sem botões", () => {
    render(<AiActionProposalCard action={{ ...base, status: "CANCELLED" }} />);

    expect(screen.getByText("Ação cancelada")).toBeTruthy();
    expect(screen.queryByLabelText("Confirmar ação")).toBeNull();
  });

  it("exibe falha com a mensagem de erro", () => {
    render(
      <AiActionProposalCard
        action={{
          ...base,
          status: "FAILED",
          errorMessage: "Falha ao executar a ação: valor inválido",
        }}
      />,
    );

    expect(screen.getByText("Falha ao executar a ação")).toBeTruthy();
    expect(screen.getByText(/valor inválido/)).toBeTruthy();
  });

  it("exibe estado de execução em andamento", () => {
    render(<AiActionProposalCard action={{ ...base, status: "EXECUTING" }} />);

    expect(screen.getByText("Executando ação...")).toBeTruthy();
    expect(screen.queryByLabelText("Confirmar ação")).toBeNull();
  });
});
