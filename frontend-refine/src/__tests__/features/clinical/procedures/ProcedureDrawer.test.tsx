import { describe, it, expect, vi, beforeEach } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { ProcedureDrawer } from "@/features/clinical/procedures/components/ProcedureDrawer";
import type { PatientProcedureResponse } from "@/features/clinical/procedures/types/procedure.types";

const { permissions } = vi.hoisted(() => ({ permissions: { value: [] as string[] } }));

vi.mock("@/features/masterdata/catalog/hooks/useCatalog", () => ({
  useCatalog: () => ({
    data: {
      content: [
        { id: "s1", name: "Restauração", category: "Dentística", price: 150, active: true },
      ],
    },
  }),
}));

vi.mock("@/features/identity/members/hooks/useMembers", () => ({
  useMembers: () => ({ data: [{ userId: "u1", name: "Dra. Ana" }] }),
}));

vi.mock("@/features/identity/auth/hooks/useAuthorization", () => ({
  useAuthorization: () => ({
    can: (permission?: string) => !permission || permissions.value.includes(permission),
  }),
}));

vi.mock("@/features/clinical/procedures/hooks/useProcedures", () => ({
  useCreateProcedure: () => ({ mutate: vi.fn(), isPending: false }),
  useUpdateProcedure: () => ({ mutate: vi.fn(), isPending: false }),
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
    createdAt: "2026-10-20T10:00:00",
    updatedAt: "2026-10-20T10:00:00",
    ...overrides,
  };
}

function renderDrawer(procedure: PatientProcedureResponse | null) {
  render(
    <ProcedureDrawer
      companyId="co1"
      contactId="c1"
      open
      onOpenChange={vi.fn()}
      procedure={procedure}
    />,
  );
}

describe("ProcedureDrawer", () => {
  beforeEach(() => {
    permissions.value = [];
  });

  it("checkbox de lançamento só aparece em Realizado e com finance:create", () => {
    permissions.value = ["finance:create"];
    renderDrawer(null);
    expect(screen.queryByText("Gerar lançamento financeiro")).toBeNull();
    fireEvent.click(screen.getByRole("radio", { name: "Realizado" }));
    expect(screen.getByText("Gerar lançamento financeiro")).toBeTruthy();
    permissions.value = [];
    renderDrawer(null);
    fireEvent.click(screen.getAllByRole("radio", { name: "Realizado", hidden: true }).at(-1)!);
    expect(screen.getAllByText("Gerar lançamento financeiro")).toHaveLength(1);
  });

  it("procedimento realizado só deixa editar observações", () => {
    renderDrawer(makeProcedure({ state: "DONE", performedAt: "2026-10-22" }));
    expect(
      screen.getByText("Procedimento realizado: só as observações podem ser alteradas."),
    ).toBeTruthy();
    expect((screen.getByLabelText("Valor (R$)") as HTMLInputElement).disabled).toBe(true);
    expect((screen.getByLabelText("Observações") as HTMLTextAreaElement).disabled).toBe(false);
  });

  it("selecionar dente mostra os chips de face", () => {
    renderDrawer(null);
    fireEvent.click(screen.getByRole("button", { name: "Dente 11" }));
    expect(screen.getByRole("button", { name: "Face M do dente 11" })).toBeTruthy();
  });
});
