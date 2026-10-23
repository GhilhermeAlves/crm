import { describe, it, expect, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { ApproveBudgetDialog } from "@/features/clinical/budgets/components/ApproveBudgetDialog";
import type { BudgetSummary } from "@/features/clinical/budgets/types/budget.types";

const { approveMutate } = vi.hoisted(() => ({ approveMutate: vi.fn() }));

vi.mock("@/features/clinical/budgets/hooks/useBudgets", () => ({
  useApproveBudget: () => ({ mutate: approveMutate, isPending: false }),
}));

function makeBudget(totalCents: number): BudgetSummary {
  return {
    id: "b1",
    title: "Plano",
    status: "DRAFT",
    subtotalCents: totalCents,
    discountCents: 0,
    totalCents,
    updatedAt: "2026-10-20T10:00:00",
    updatedByName: null,
  };
}

const renderDialog = (budget: BudgetSummary) =>
  render(
    <ApproveBudgetDialog companyId="co1" contactId="c1" budget={budget} onOpenChange={vi.fn()} />,
  );

const items = () =>
  screen.getAllByText(/ª parcela/).map((el) => el.textContent?.replace(/ /g, " "));

describe("ApproveBudgetDialog", () => {
  it("mostra 3 parcelas 3334/3333/3333 para 10000 em 3x", () => {
    renderDialog(makeBudget(10000));
    fireEvent.change(screen.getByLabelText("1º vencimento"), { target: { value: "2026-11-10" } });
    fireEvent.change(screen.getByLabelText("Parcelas"), { target: { value: "3" } });
    expect(items()).toEqual([
      "1ª parcela — 10/11/2026 — R$ 33,34",
      "2ª parcela — 10/12/2026 — R$ 33,33",
      "3ª parcela — 10/01/2027 — R$ 33,33",
    ]);
    fireEvent.click(screen.getByRole("button", { name: "Aprovar" }));
    expect(approveMutate).toHaveBeenCalledWith(
      {
        budgetId: "b1",
        payload: { paymentMethod: "PIX", installments: 3, firstDueDate: "2026-11-10" },
      },
      expect.anything(),
    );
  });

  it("limita parcelas a 1-24", () => {
    renderDialog(makeBudget(10000));
    const input = screen.getByLabelText("Parcelas") as HTMLInputElement;
    fireEvent.change(input, { target: { value: "0" } });
    expect(input.value).toBe("1");
    fireEvent.change(input, { target: { value: "99" } });
    expect(input.value).toBe("24");
  });

  it("total zero não gera parcelas", () => {
    renderDialog(makeBudget(0));
    expect(screen.getByText("Orçamento sem valor: nenhuma parcela será gerada.")).toBeTruthy();
    expect(screen.queryByText(/ª parcela/)).toBeNull();
  });
});
