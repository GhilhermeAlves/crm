import { describe, it, expect, vi, beforeEach } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { PatientClinicalChartTab } from "@/features/clinical/procedures/components/PatientClinicalChartTab";
import type { PatientProcedureResponse } from "@/features/clinical/procedures/types/procedure.types";

const can = vi.hoisted(() => ({ value: (() => true) as (permission: string) => boolean }));

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
    createdAt: "2026-10-20T10:00:00",
    updatedAt: "2026-10-20T10:00:00",
    ...overrides,
  };
}

const procedures = [
  makeProcedure(),
  makeProcedure({ id: "p2", name: "Limpeza", state: "PRE_EXISTING", teeth: [] }),
];

vi.mock("@/features/clinical/procedures/hooks/useProcedures", () => ({
  useProcedures: () => ({ data: procedures, isLoading: false }),
  useDeleteProcedure: () => ({ mutate: vi.fn(), isPending: false }),
}));

vi.mock("@/features/identity/auth/hooks/useAuthorization", () => ({
  useAuthorization: () => ({ can: (permission: string) => can.value(permission) }),
}));

vi.mock("@/features/clinical/procedures/components/ProcedureDrawer", () => ({
  ProcedureDrawer: () => null,
}));

vi.mock("@/features/clinical/procedures/components/CompleteProcedureDialog", () => ({
  CompleteProcedureDialog: () => null,
}));

describe("PatientClinicalChartTab", () => {
  beforeEach(() => {
    can.value = () => true;
  });

  it("clicar no dente filtra a ficha geral e clicar de novo limpa", () => {
    render(<PatientClinicalChartTab companyId="co1" contactId="c1" />);
    expect(screen.getByText("Limpeza")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Dente 11, A realizar" }));
    expect(screen.queryByText("Limpeza")).toBeNull();
    fireEvent.click(screen.getByRole("button", { name: "Dente 11, A realizar" }));
    expect(screen.getByText("Limpeza")).toBeTruthy();
  });

  it("botão de novo procedimento exige contact:update", () => {
    can.value = () => false;
    render(<PatientClinicalChartTab companyId="co1" contactId="c1" />);
    expect(screen.queryByText("+ Novo procedimento")).toBeNull();
  });
});
