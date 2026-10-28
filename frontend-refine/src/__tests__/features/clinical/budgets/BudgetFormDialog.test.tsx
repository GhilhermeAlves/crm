import { describe, it, expect, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { BudgetFormDialog } from "@/features/clinical/budgets/components/BudgetFormDialog";
import type { PatientProcedureResponse } from "@/features/clinical/procedures/types/procedure.types";

const { createMutate, procedures } = vi.hoisted(() => ({
  createMutate: vi.fn(),
  procedures: { value: [] as unknown[] },
}));

vi.mock("@/features/identity/modules/hooks/useIsDentalClinic", () => ({
  useIsDentalClinic: () => true,
}));

vi.mock("@/features/clinical/procedures/hooks/useProcedures", () => ({
  useProcedures: () => ({ data: procedures.value }),
}));

vi.mock("@/features/clinical/budgets/components/BudgetTitleCombobox", () => ({
  BudgetTitleCombobox: ({
    id,
    value,
    onChange,
  }: {
    id: string;
    value: string;
    onChange: (value: string) => void;
  }) => <input id={id} value={value} onChange={(e) => onChange(e.target.value)} />,
}));

vi.mock("@/features/clinical/budgets/hooks/useBudgets", () => ({
  useCreateBudget: () => ({ mutate: createMutate, isPending: false }),
  useUpdateBudget: () => ({ mutate: vi.fn(), isPending: false }),
}));

function proc(overrides: Partial<PatientProcedureResponse>): PatientProcedureResponse {
  return {
    id: "p1",
    contactId: "c1",
    catalogItemId: "s1",
    name: "Restauração",
    category: null,
    state: "PLANNED",
    dentition: "PERMANENT",
    priceCents: 15000,
    professionalId: null,
    professionalName: null,
    notes: null,
    performedAt: null,
    teeth: [{ tooth: 11, faces: ["M"] }],
    transaction: null,
    budget: null,
    generateTransactionOnDone: false,
    createdAt: "2026-10-20T10:00:00",
    updatedAt: "2026-10-20T10:00:00",
    ...overrides,
  };
}

function renderForm() {
  render(<BudgetFormDialog companyId="co1" contactId="c1" open onOpenChange={vi.fn()} />);
}

const text = (id: string) => screen.getByTestId(id).textContent?.replace(/ /g, " ");

describe("BudgetFormDialog", () => {
  it("calcula subtotal e total ao marcar itens e digitar desconto", () => {
    procedures.value = [
      proc({ id: "p1", name: "Restauração", priceCents: 15000 }),
      proc({ id: "p2", name: "Limpeza", priceCents: 10000, teeth: [] }),
      proc({ id: "p3", name: "Já orçado", budget: { id: "b9", title: "X", status: "DRAFT" } }),
      proc({ id: "p4", name: "Feito", state: "DONE" }),
    ];
    renderForm();
    expect(screen.queryByText(/Já orçado/)).toBeNull();
    expect(screen.queryByText(/Feito/)).toBeNull();
    fireEvent.click(screen.getByRole("checkbox", { name: "Restauração — 11 (M)" }));
    fireEvent.click(screen.getByRole("checkbox", { name: "Limpeza — Boca toda" }));
    expect(text("budget-subtotal")).toBe("R$ 250,00");
    fireEvent.change(screen.getByLabelText("Desconto (R$)"), { target: { value: "50" } });
    expect(text("budget-total")).toBe("R$ 200,00");

    fireEvent.change(screen.getByLabelText("Título"), { target: { value: "Plano" } });
    fireEvent.click(screen.getByRole("button", { name: "Salvar" }));
    expect(createMutate).toHaveBeenCalledWith(
      {
        title: "Plano",
        procedureIds: ["p1", "p2"],
        discountCents: 5000,
        notes: null,
        itemPrices: { p1: 15000, p2: 10000 },
      },
      expect.anything(),
    );
  });

  it("desconto maior que o subtotal desabilita salvar e mostra aviso", () => {
    procedures.value = [proc({})];
    renderForm();
    fireEvent.change(screen.getByLabelText("Título"), { target: { value: "Plano" } });
    fireEvent.click(screen.getByRole("checkbox", { name: "Restauração — 11 (M)" }));
    const save = screen.getByRole("button", { name: "Salvar" }) as HTMLButtonElement;
    expect(save.disabled).toBe(false);
    fireEvent.change(screen.getByLabelText("Desconto (R$)"), { target: { value: "200" } });
    expect(save.disabled).toBe(true);
    expect(screen.getByText("O desconto não pode ser maior que o subtotal.")).toBeTruthy();
  });

  it("sem procedimentos disponíveis mostra a orientação", () => {
    procedures.value = [];
    renderForm();
    expect(
      screen.getByText('Nenhum procedimento "A realizar" disponível. Cadastre na Ficha clínica.'),
    ).toBeTruthy();
    expect((screen.getByRole("button", { name: "Salvar" }) as HTMLButtonElement).disabled).toBe(
      true,
    );
  });
});

describe("BudgetFormDialog - refetch", () => {
  it("preserva o título digitado quando o budget muda de referência (mesmo id)", () => {
    procedures.value = [proc({})];
    const make = () => ({
      id: "b1",
      contactId: "c1",
      title: "Original",
      status: "DRAFT" as const,
      subtotalCents: 15000,
      discountCents: 0,
      totalCents: 15000,
      notes: null,
      paymentMethod: null,
      installments: null,
      firstDueDate: null,
      approvedAt: null,
      lostAt: null,
      items: [],
      installmentsList: [],
      updatedAt: "2026-10-20T10:00:00",
      updatedByName: null,
    });
    const ui = (b: ReturnType<typeof make>) => (
      <BudgetFormDialog companyId="co1" contactId="c1" open onOpenChange={vi.fn()} budget={b} />
    );
    const { rerender } = render(ui(make()));
    fireEvent.change(screen.getByLabelText("Título"), { target: { value: "Editado" } });
    rerender(ui(make()));
    expect((screen.getByLabelText("Título") as HTMLInputElement).value).toBe("Editado");
  });
});
