import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { OpportunityCard } from "./OpportunityCard";
import type { Opportunity } from "../types/pipeline.types";

const OPP: Opportunity = {
  id: "opp-1",
  companyId: "c-1",
  title: "Contrato anual",
  value: 1500.5,
  contactId: "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  pipelineId: "pipe-1",
  stageId: "stage-1",
  stageName: "Prospecção",
  probability: 45,
  assignedTo: null,
  expectedCloseDate: null,
  status: "OPEN",
  wonAt: null,
  lostAt: null,
  lossReason: null,
  notes: null,
  createdAt: "2026-08-13T00:00:00",
  updatedAt: "2026-08-13T00:00:00",
};

describe("OpportunityCard (Sprint 11)", () => {
  it("renderiza título, valor formatado em BRL e estágio/probabilidade", () => {
    render(<OpportunityCard opportunity={OPP} isFirst isLast canMove canWin canLose />);
    expect(screen.getByText("Contrato anual")).toBeTruthy();
    expect(screen.getByText("1.500,50", { exact: false })).toBeTruthy();
    expect(screen.getByText("Prospecção · 45%")).toBeTruthy();
  });

  it("desabilita voltar no primeiro estágio e avançar no último", () => {
    render(<OpportunityCard opportunity={OPP} isFirst isLast canMove />);
    expect(screen.getByLabelText("Mover para trás").hasAttribute("disabled")).toBe(true);
    expect(screen.getByLabelText("Avançar").hasAttribute("disabled")).toBe(true);
  });

  it("habilita movimentos quando há margem e permissão", () => {
    render(<OpportunityCard opportunity={OPP} isFirst={false} isLast={false} canMove />);
    expect(screen.getByLabelText("Mover para trás").hasAttribute("disabled")).toBe(false);
    expect(screen.getByLabelText("Avançar").hasAttribute("disabled")).toBe(false);
  });

  it("exibe o menu Concluir quando há permissão de ganhar/perder", () => {
    const onWin = vi.fn();
    const onLost = vi.fn();
    const onDelete = vi.fn();
    render(
      <OpportunityCard
        opportunity={OPP}
        isFirst
        isLast
        canWin
        canLose
        canDelete
        onWin={onWin}
        onLost={onLost}
        onDelete={onDelete}
      />,
    );
    expect(screen.getByText("Concluir")).toBeTruthy();
  });

  it("oculta o menu Concluir quando sem permissão de ganhar/perder", () => {
    render(<OpportunityCard opportunity={OPP} isFirst isLast />);
    expect(screen.queryByText("Concluir")).toBeNull();
  });
});
