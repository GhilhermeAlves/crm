import { describe, it, expect, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { ProcedureList } from "@/features/clinical/procedures/components/ProcedureList";
import type { PatientProcedureResponse } from "@/features/clinical/procedures/types/procedure.types";

vi.mock("@/features/clinical/procedures/hooks/useProcedures", () => ({
  useDeleteProcedure: () => ({ mutate: vi.fn(), isPending: false }),
}));

function makeProcedure(
  overrides: Partial<PatientProcedureResponse> = {},
): PatientProcedureResponse {
  return {
    id: "p1",
    contactId: "c1",
    catalogItemId: "s1",
    name: "Restauração",
    category: "Dentística",
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

function renderList(procedures: PatientProcedureResponse[]) {
  render(
    <ProcedureList
      companyId="co1"
      contactId="c1"
      procedures={procedures}
      canEdit={true}
      onEdit={vi.fn()}
      onComplete={vi.fn()}
    />,
  );
}

describe("ProcedureList", () => {
  it("mostra ações por estado e filtra por estado", () => {
    renderList([
      makeProcedure({ id: "p1", name: "Restauração", state: "PLANNED" }),
      makeProcedure({ id: "p2", name: "Limpeza", state: "DONE", performedAt: "2026-10-01" }),
    ]);
    expect(screen.getAllByText("Marcar como realizado")).toHaveLength(1);
    fireEvent.click(screen.getByRole("button", { name: "Filtrar: Realizado" }));
    expect(screen.queryByText("Restauração")).toBeNull();
    expect(screen.getByText("Limpeza")).toBeTruthy();
  });

  it("exclusão avisa cancelamento do lançamento pendente", () => {
    renderList([
      makeProcedure({
        state: "DONE",
        performedAt: "2026-10-01",
        transaction: { id: "t1", status: "PENDING" },
      }),
    ]);
    fireEvent.click(screen.getByRole("button", { name: "Excluir procedimento" }));
    expect(
      screen.getByText("O lançamento financeiro pendente deste procedimento será cancelado."),
    ).toBeTruthy();
  });

  it("procedimento com pagamento não pode ser excluído", () => {
    renderList([
      makeProcedure({
        state: "DONE",
        performedAt: "2026-10-01",
        transaction: { id: "t1", status: "PAID" },
      }),
    ]);
    const button = screen.getByRole("button", {
      name: "Excluir procedimento",
    }) as HTMLButtonElement;
    expect(button.disabled).toBe(true);
    expect(button.getAttribute("title")).toBe(
      "Procedimento com pagamento registrado não pode ser excluído",
    );
  });

  it("sem permissão de edição esconde as ações", () => {
    render(
      <ProcedureList
        companyId="co1"
        contactId="c1"
        procedures={[makeProcedure()]}
        canEdit={false}
        onEdit={vi.fn()}
        onComplete={vi.fn()}
      />,
    );
    expect(screen.queryByText("Editar")).toBeNull();
    expect(screen.queryByText("Marcar como realizado")).toBeNull();
    expect(screen.queryByRole("button", { name: "Excluir procedimento" })).toBeNull();
  });

  it("mostra o orçamento do procedimento ou traço", () => {
    renderList([
      makeProcedure({
        id: "p1",
        name: "Restauração",
        budget: { id: "b1", title: "Plano ortodôntico", status: "DRAFT" },
      }),
      makeProcedure({ id: "p2", name: "Limpeza", budget: null }),
    ]);
    expect(screen.getByText("Orçamento")).toBeTruthy();
    expect(screen.getByText("Plano ortodôntico · Rascunho")).toBeTruthy();
  });

  it("procedimento de orçamento aprovado não pode ser excluído", () => {
    renderList([makeProcedure({ budget: { id: "b1", title: "Plano", status: "APPROVED" } })]);
    const button = screen.getByRole("button", {
      name: "Excluir procedimento",
    }) as HTMLButtonElement;
    expect(button.disabled).toBe(true);
    expect(button.getAttribute("title")).toBe(
      "Procedimento de orçamento aprovado não pode ser excluído",
    );
  });

  it("exclusão em orçamento rascunho avisa remoção do orçamento", () => {
    renderList([makeProcedure({ budget: { id: "b1", title: "Plano", status: "DRAFT" } })]);
    fireEvent.click(screen.getByRole("button", { name: "Excluir procedimento" }));
    expect(screen.getByText("O procedimento será removido do orçamento Plano.")).toBeTruthy();
  });
});
