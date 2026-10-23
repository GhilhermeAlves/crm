import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import { PatientEvolutionsTab } from "@/features/clinical/evolutions/components/PatientEvolutionsTab";
import type { PatientEvolutionResponse } from "@/features/clinical/evolutions/types/evolution.types";
import type { Contact } from "@/features/masterdata/contacts/types/contact.types";

const { evolutionsState, userState } = vi.hoisted(() => ({
  evolutionsState: { value: [] as PatientEvolutionResponse[] },
  userState: {
    value: { id: "u1", companyId: "co1", membershipRole: "AGENT", roles: [] as string[] },
  },
}));

vi.mock("@/features/identity/auth/hooks/useAuth", () => ({
  useAuth: () => ({ user: userState.value }),
}));

vi.mock("@/features/clinical/patients/hooks/usePatient", () => ({
  useContactAppointments: () => ({ data: [] }),
}));

vi.mock("@/features/clinical/evolutions/hooks/useEvolutions", () => ({
  useEvolutions: () => ({ data: evolutionsState.value }),
  useCreateEvolution: () => ({ mutate: vi.fn(), isPending: false }),
  useUpdateEvolution: () => ({ mutate: vi.fn(), isPending: false }),
  useFinalizeEvolution: () => ({ mutate: vi.fn(), isPending: false }),
  useDeleteEvolution: () => ({ mutate: vi.fn(), isPending: false }),
}));

const patient = { id: "c1", firstName: "Ana" } as Contact;

function makeEvolution(overrides: Partial<PatientEvolutionResponse>): PatientEvolutionResponse {
  return {
    id: "e1",
    contactId: "c1",
    appointmentId: null,
    body: "Restauração no dente 11",
    status: "DRAFT",
    createdBy: "u1",
    createdByName: "Ana",
    finalizedBy: null,
    finalizedByName: null,
    finalizedAt: null,
    createdAt: "2026-10-20T10:00:00",
    updatedAt: "2026-10-20T10:00:00",
    ...overrides,
  };
}

function renderTab() {
  render(<PatientEvolutionsTab companyId="co1" contactId="c1" patient={patient} />);
}

describe("PatientEvolutionsTab", () => {
  beforeEach(() => {
    evolutionsState.value = [];
    userState.value = { id: "u1", companyId: "co1", membershipRole: "AGENT", roles: [] };
  });

  it("evolução finalizada não mostra botões de ação", () => {
    evolutionsState.value = [
      makeEvolution({
        status: "FINALIZED",
        finalizedBy: "u1",
        finalizedByName: "Ana",
        finalizedAt: "2026-10-20T11:00:00",
      }),
    ];
    renderTab();

    expect(screen.getByText("Finalizada")).toBeTruthy();
    expect(screen.queryByText("Finalizar")).toBeNull();
    expect(screen.queryByText("Editar")).toBeNull();
    expect(screen.queryByText("Excluir")).toBeNull();
  });

  it("rascunho do próprio autor mostra Finalizar", () => {
    evolutionsState.value = [makeEvolution({ createdBy: "u1" })];
    renderTab();

    expect(screen.getByText("Rascunho")).toBeTruthy();
    expect(screen.getByText("Finalizar")).toBeTruthy();
    expect(screen.getByText("Editar")).toBeTruthy();
  });

  it("admin edita rascunho de outro autor, mas não finaliza", () => {
    userState.value = { id: "u9", companyId: "co1", membershipRole: "ADMIN", roles: [] };
    evolutionsState.value = [makeEvolution({ createdBy: "u1" })];
    renderTab();

    expect(screen.getByText("Editar")).toBeTruthy();
    expect(screen.queryByText("Finalizar")).toBeNull();
  });
});
