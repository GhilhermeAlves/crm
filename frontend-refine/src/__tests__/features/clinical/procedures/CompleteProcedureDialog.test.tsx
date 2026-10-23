import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { CompleteProcedureDialog } from "@/features/clinical/procedures/components/CompleteProcedureDialog";
import type { PatientProcedureResponse } from "@/features/clinical/procedures/types/procedure.types";

vi.mock("@/features/identity/members/hooks/useMembers", () => ({
  useMembers: () => ({ data: [{ userId: "u1", name: "Dra. Ana" }] }),
}));

vi.mock("@/features/identity/auth/hooks/useAuthorization", () => ({
  useAuthorization: () => ({ can: () => true }),
}));

vi.mock("@/features/clinical/procedures/hooks/useProcedures", () => ({
  useCompleteProcedure: () => ({ mutate: vi.fn(), isPending: false }),
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
    createdAt: "2026-10-20T10:00:00",
    updatedAt: "2026-10-20T10:00:00",
    ...overrides,
  };
}

function renderDialog(procedure: PatientProcedureResponse) {
  render(
    <CompleteProcedureDialog
      companyId="co1"
      contactId="c1"
      procedure={procedure}
      onOpenChange={vi.fn()}
    />,
  );
}

describe("CompleteProcedureDialog", () => {
  it("sem orçamento oferece gerar lançamento financeiro", () => {
    renderDialog(makeProcedure());
    expect(screen.getByText("Gerar lançamento financeiro")).toBeTruthy();
    expect(screen.queryByText(/Cobrado pelo orçamento/)).toBeNull();
  });

  it("orçamento aprovado esconde o lançamento e mostra a cobrança pelo orçamento", () => {
    renderDialog(makeProcedure({ budget: { id: "b1", title: "Plano", status: "APPROVED" } }));
    expect(screen.queryByText("Gerar lançamento financeiro")).toBeNull();
    expect(screen.getByText("Cobrado pelo orçamento Plano.")).toBeTruthy();
  });

  it("vem pré-marcado quando o procedimento guardou gerar lançamento ao realizar", () => {
    renderDialog(makeProcedure({ generateTransactionOnDone: true }));
    expect(screen.getByRole("checkbox").getAttribute("aria-checked")).toBe("true");
  });

  it("sem o flag guardado o lançamento vem desmarcado", () => {
    renderDialog(makeProcedure());
    expect(screen.getByRole("checkbox").getAttribute("aria-checked")).toBe("false");
  });

  it("orçamento em rascunho mantém o lançamento financeiro", () => {
    renderDialog(makeProcedure({ budget: { id: "b1", title: "Plano", status: "DRAFT" } }));
    expect(screen.getByText("Gerar lançamento financeiro")).toBeTruthy();
  });
});
