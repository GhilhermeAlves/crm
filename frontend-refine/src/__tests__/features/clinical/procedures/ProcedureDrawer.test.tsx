import { describe, it, expect, vi, beforeEach } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { ProcedureDrawer } from "@/features/clinical/procedures/components/ProcedureDrawer";
import type { PatientProcedureResponse } from "@/features/clinical/procedures/types/procedure.types";

const { permissions, createMutate, updateMutate } = vi.hoisted(() => ({
  permissions: { value: [] as string[] },
  createMutate: { fn: null as null | ((...args: unknown[]) => void) },
  updateMutate: { fn: null as null | ((...args: unknown[]) => void) },
}));

vi.mock("@/features/masterdata/catalog/hooks/useCatalog", () => ({
  useCatalog: () => ({
    data: {
      content: [
        { id: "s1", name: "Restauração", category: "Dentística", price: 150, active: true },
        { id: "s2", name: "Avaliação", category: "Clínica", price: null, active: true },
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
  useCreateProcedure: () => ({
    mutate: (...args: unknown[]) => createMutate.fn?.(...args),
    isPending: false,
  }),
  useUpdateProcedure: () => ({
    mutate: (...args: unknown[]) => updateMutate.fn?.(...args),
    isPending: false,
  }),
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

function renderDrawer(procedure: PatientProcedureResponse | null, initialTooth?: number) {
  render(
    <ProcedureDrawer
      companyId="co1"
      contactId="c1"
      open
      onOpenChange={vi.fn()}
      procedure={procedure}
      initialTooth={initialTooth}
    />,
  );
}

describe("ProcedureDrawer", () => {
  beforeEach(() => {
    permissions.value = [];
    createMutate.fn = null;
    updateMutate.fn = null;
  });

  it("checkbox de lançamento aparece em todos os estados só com finance:create", () => {
    permissions.value = ["finance:create"];
    renderDrawer(null);
    expect(screen.getByText("Gerar lançamento financeiro")).toBeTruthy();
    expect(
      screen.getByText(
        "O lançamento será gerado quando o procedimento for marcado como realizado.",
      ),
    ).toBeTruthy();
    fireEvent.click(screen.getByRole("radio", { name: "Pré-existente" }));
    expect((screen.getByRole("checkbox") as HTMLButtonElement).disabled).toBe(true);
    permissions.value = [];
    renderDrawer(null);
    expect(screen.getAllByText("Gerar lançamento financeiro")).toHaveLength(1);
  });

  it("em A realizar o flag marcado e o valor mascarado vão no payload", () => {
    permissions.value = ["finance:create"];
    const mutate = vi.fn();
    updateMutate.fn = mutate;
    renderDrawer(makeProcedure());
    fireEvent.click(screen.getByRole("checkbox"));
    fireEvent.change(screen.getByLabelText("Valor (R$)"), { target: { value: "1.234,56" } });
    fireEvent.click(screen.getByRole("button", { name: "Salvar" }));
    const { payload } = mutate.mock.calls[0][0] as { payload: Record<string, unknown> };
    expect(payload.generateTransaction).toBe(true);
    expect(payload.priceCents).toBe(123456);
    expect(payload.dueDate).toBeNull();
  });

  it("rótulo Procedimentos e lista com valor ao lado do nome", () => {
    renderDrawer(null);
    expect(screen.getByText("Procedimentos")).toBeTruthy();
    expect(screen.getByText("Selecione um procedimento")).toBeTruthy();
  });

  it("valor usa máscara BRL e envia centavos", () => {
    renderDrawer(makeProcedure());
    const price = screen.getByLabelText("Valor (R$)") as HTMLInputElement;
    expect(price.type).toBe("text");
    expect(price.value).toBe("150,00");
    fireEvent.change(price, { target: { value: "1.234,56" } });
    expect(price.value).toBe("1.234,56");
    fireEvent.change(price, { target: { value: "1.234,567" } });
    expect(price.value).toBe("12.345,67");
  });

  it("dente vindo do odontograma já chega marcado", () => {
    renderDrawer(null, 21);
    expect(screen.getByRole("button", { name: "Dente 21" }).getAttribute("aria-pressed")).toBe(
      "true",
    );
    expect(screen.getByRole("button", { name: "Face O do dente 21" })).toBeTruthy();
  });

  it("procedimento realizado só deixa editar observações", () => {
    renderDrawer(makeProcedure({ state: "DONE", performedAt: "2026-10-22" }));
    expect(
      screen.getByText("Procedimento realizado: só as observações podem ser alteradas."),
    ).toBeTruthy();
    expect((screen.getByLabelText("Valor (R$)") as HTMLInputElement).disabled).toBe(true);
    expect((screen.getByLabelText("Observações") as HTMLTextAreaElement).disabled).toBe(false);
  });

  it("seletor de dentes em toggle marca, mostra faces e desmarca", () => {
    renderDrawer(null);
    const tooth = screen.getByRole("button", { name: "Dente 11" });
    expect(tooth.getAttribute("aria-pressed")).toBe("false");
    fireEvent.click(tooth);
    expect(tooth.getAttribute("aria-pressed")).toBe("true");
    expect(screen.getByRole("button", { name: "Face M do dente 11" })).toBeTruthy();
    fireEvent.click(tooth);
    expect(screen.queryByRole("button", { name: "Face M do dente 11" })).toBeNull();
  });

  it("decídua mostra os dentes 55 a 85", () => {
    renderDrawer(null);
    fireEvent.click(screen.getByRole("button", { name: "Decídua" }));
    expect(screen.getByRole("button", { name: "Dente 55" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "Dente 85" })).toBeTruthy();
    expect(screen.queryByRole("button", { name: "Dente 18" })).toBeNull();
  });
});
