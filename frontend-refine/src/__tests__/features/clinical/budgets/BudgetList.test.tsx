import { describe, it, expect, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { BudgetList } from "@/features/clinical/budgets/components/BudgetList";
import type { BudgetStatus, BudgetSummary } from "@/features/clinical/budgets/types/budget.types";

vi.mock("@/features/clinical/budgets/hooks/useBudgets", () => ({
  useLoseBudget: () => ({ mutate: vi.fn(), isPending: false }),
  useDeleteBudget: () => ({ mutate: vi.fn(), isPending: false }),
}));

function makeBudget(status: BudgetStatus): BudgetSummary {
  return {
    id: `b-${status}`,
    title: `Orçamento ${status}`,
    status,
    subtotalCents: 10000,
    discountCents: 0,
    totalCents: 10000,
    updatedAt: "2026-10-20T10:00:00",
    updatedByName: "Dra. Ana",
  };
}

function renderList(status: BudgetStatus, canEdit = true) {
  return render(
    <BudgetList
      companyId="co"
      contactId="c1"
      budgets={[makeBudget(status)]}
      canEdit={canEdit}
      onEdit={vi.fn()}
      onApprove={vi.fn()}
      onView={vi.fn()}
    />,
  );
}

describe("BudgetList", () => {
  it("rascunho tem Editar, Aprovar, Marcar como perdido e Excluir", () => {
    renderList("DRAFT");
    expect(screen.getByText("Visualizar")).toBeTruthy();
    expect(screen.getByText("Editar")).toBeTruthy();
    expect(screen.getByText("Aprovar")).toBeTruthy();
    expect(screen.getByText("Marcar como perdido")).toBeTruthy();
    expect(screen.getByText("Excluir")).toBeTruthy();
    expect(screen.getByText("20/10/2026 10:00")).toBeTruthy();
  });

  it("aprovado não tem Editar, Aprovar nem Excluir, e avisa do cancelamento das parcelas", () => {
    renderList("APPROVED");
    expect(screen.queryByText("Editar")).toBeNull();
    expect(screen.queryByText("Aprovar")).toBeNull();
    expect(screen.queryByText("Excluir")).toBeNull();
    fireEvent.click(screen.getByText("Marcar como perdido"));
    expect(screen.getByText("As parcelas pendentes serão canceladas.")).toBeTruthy();
  });

  it("perdido tem Excluir e não tem Marcar como perdido", () => {
    renderList("LOST");
    expect(screen.getByText("Excluir")).toBeTruthy();
    expect(screen.queryByText("Marcar como perdido")).toBeNull();
  });

  it("sem permissão só mostra Visualizar", () => {
    renderList("DRAFT", false);
    expect(screen.getByText("Visualizar")).toBeTruthy();
    expect(screen.queryByText("Editar")).toBeNull();
    expect(screen.queryByText("Excluir")).toBeNull();
  });

  it("lista vazia mostra mensagem", () => {
    render(
      <BudgetList
        companyId="co"
        contactId="c1"
        budgets={[]}
        canEdit
        onEdit={vi.fn()}
        onApprove={vi.fn()}
        onView={vi.fn()}
      />,
    );
    expect(screen.getByText("Nenhum orçamento para este paciente.")).toBeTruthy();
  });
});
