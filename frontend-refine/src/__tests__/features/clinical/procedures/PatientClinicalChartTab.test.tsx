import { describe, it, expect, vi, beforeEach } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { PatientClinicalChartTab } from "@/features/clinical/procedures/components/PatientClinicalChartTab";
import type { PatientProcedureResponse } from "@/features/clinical/procedures/types/procedure.types";

const can = vi.hoisted(() => ({ value: (() => true) as (permission: string) => boolean }));
const drawer = vi.hoisted(() => ({
  props: null as null | { open: boolean; initialTooth?: number | null; initialDentition?: string },
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

const procedures = [
  makeProcedure(),
  makeProcedure({ id: "p2", name: "Limpeza", state: "PRE_EXISTING", teeth: [] }),
];

vi.mock("@/features/clinical/procedures/hooks/useProcedures", () => ({
  useProcedures: () => ({ data: procedures, isLoading: false }),
  useDeleteProcedure: () => ({ mutate: vi.fn(), isPending: false }),
  useCompleteProcedure: () => ({ mutate: vi.fn(), isPending: false }),
}));

vi.mock("@/features/identity/auth/hooks/useAuthorization", () => ({
  useAuthorization: () => ({ can: (permission: string) => can.value(permission) }),
}));

vi.mock("@/features/clinical/procedures/components/ProcedureDrawer", () => ({
  ProcedureDrawer: (props: {
    open: boolean;
    initialTooth?: number | null;
    initialDentition?: string;
  }) => {
    drawer.props = props;
    return null;
  },
}));

vi.mock("@/features/clinical/budgets/components/BudgetFormDialog", () => ({
  BudgetFormDialog: () => null,
}));

vi.mock("@/features/clinical/budgets/components/BudgetDetailsDialog", () => ({
  BudgetDetailsDialog: () => null,
}));

vi.mock("@/features/clinical/procedures/components/CompleteProcedureDialog", () => ({
  CompleteProcedureDialog: () => null,
}));

describe("PatientClinicalChartTab", () => {
  beforeEach(() => {
    can.value = () => true;
    drawer.props = null;
  });

  it("clicar no dente abre o novo procedimento com o dente e a dentição atual", () => {
    render(<PatientClinicalChartTab companyId="co1" contactId="c1" />);
    expect(drawer.props?.open).toBe(false);
    fireEvent.click(screen.getByRole("button", { name: "Dente 11, A realizar" }));
    expect(drawer.props?.open).toBe(true);
    expect(drawer.props?.initialTooth).toBe(11);
    expect(drawer.props?.initialDentition).toBe("PERMANENT");
    // A lista não é mais filtrada pelo dente.
    expect(screen.getByText("Limpeza")).toBeTruthy();
    expect(screen.queryByText("Dente 11")).toBeNull();
  });

  it("sem contact:update clicar no dente não abre o drawer", () => {
    can.value = () => false;
    render(<PatientClinicalChartTab companyId="co1" contactId="c1" />);
    fireEvent.click(screen.getByRole("button", { name: "Dente 11, A realizar" }));
    expect(drawer.props?.open).toBe(false);
  });

  it("botão de novo procedimento exige contact:update", () => {
    can.value = () => false;
    render(<PatientClinicalChartTab companyId="co1" contactId="c1" />);
    expect(screen.queryByText("+ Novo procedimento")).toBeNull();
  });
});
