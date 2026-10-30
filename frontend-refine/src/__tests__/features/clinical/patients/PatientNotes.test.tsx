import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { PatientNotes } from "@/features/clinical/patients/components/PatientNotes";
import type { ContactNote } from "@/features/clinical/patients/types/patient.types";
import type { Contact } from "@/features/masterdata/contacts/types/contact.types";

const { notesState, segmentState, createMock, updateMock, deleteMock } = vi.hoisted(() => ({
  notesState: { value: [] as ContactNote[] },
  segmentState: { value: "AESTHETICS" },
  createMock: vi.fn(),
  updateMock: vi.fn(),
  deleteMock: vi.fn(),
}));

vi.mock("@/features/identity/auth/hooks/useAuth", () => ({
  useAuth: () => ({
    user: { id: "u1", companyId: "co1", membershipRole: "AGENT", roles: [] },
  }),
}));

vi.mock("@/features/identity/auth/hooks/useAuthorization", () => ({
  usePermission: () => (permission?: string) => permission === "contact:update",
}));

vi.mock("@/features/identity/modules/hooks/useCompanyModules", () => ({
  useCompanyModules: () => ({ data: { segment: segmentState.value } }),
}));

vi.mock("@/features/clinical/patients/hooks/usePatient", () => ({
  useContactNotes: () => ({ data: notesState.value, isLoading: false }),
  useCreateContactNote: () => ({ mutate: createMock, isPending: false }),
  useUpdateContactNote: () => ({ mutate: updateMock, isPending: false }),
  useDeleteContactNote: () => ({ mutate: deleteMock, isPending: false }),
}));

const contact = { id: "c1", firstName: "Ana", lastName: "Silva", notes: null } as Contact;

function makeNote(overrides: Partial<ContactNote>): ContactNote {
  return {
    id: "n1",
    contactId: "c1",
    body: "Anotação",
    tooth: null,
    createdBy: "u1",
    createdByName: "Ana",
    createdAt: "2026-10-13T10:00:00",
    updatedAt: "2026-10-13T10:00:00",
    ...overrides,
  };
}

describe("PatientNotes", () => {
  beforeEach(() => {
    createMock.mockClear();
    notesState.value = [];
    segmentState.value = "AESTHETICS";
  });

  it("fora da Odontologia não mostra o campo de dente", () => {
    notesState.value = [makeNote({ id: "n1", body: "Com dente", tooth: 36 })];

    render(<PatientNotes contact={contact} />);

    expect(screen.queryByLabelText("Dente")).toBeNull();
    expect(screen.queryByText("Dente 36")).toBeNull();
  });

  it("na Odontologia mostra o dente da anotação e o campo de dente", () => {
    segmentState.value = "DENTAL";
    notesState.value = [
      makeNote({ id: "n1", body: "Sensibilidade", tooth: 36 }),
      makeNote({ id: "n2", body: "Geral" }),
    ];

    render(<PatientNotes contact={contact} />);

    expect(screen.getByLabelText("Dente")).toBeTruthy();
    expect(screen.getByText("Dente 36")).toBeTruthy();
    expect(screen.getByLabelText("Filtrar por dente")).toBeTruthy();
  });

  it("renderiza a lista de anotações", () => {
    notesState.value = [
      makeNote({ id: "n1", body: "Paciente alérgico a látex" }),
      makeNote({ id: "n2", body: "Prefere horários pela manhã" }),
    ];

    render(<PatientNotes contact={contact} />);

    expect(screen.getByText("Paciente alérgico a látex")).toBeTruthy();
    expect(screen.getByText("Prefere horários pela manhã")).toBeTruthy();
  });

  it("envia uma nova anotação", () => {
    render(<PatientNotes contact={contact} />);

    fireEvent.change(screen.getByPlaceholderText("Escreva uma anotação sobre o paciente…"), {
      target: { value: "Nova observação" },
    });
    fireEvent.click(screen.getByText("Adicionar anotação"));

    expect(createMock).toHaveBeenCalledWith(
      { body: "Nova observação", tooth: null },
      expect.anything(),
    );
  });

  it("esconde o botão de editar de anotações de outro autor", () => {
    notesState.value = [
      makeNote({ id: "n1", createdBy: "u2", createdByName: "Outro" }),
      makeNote({ id: "n2", createdBy: "u1", createdByName: "Ana" }),
    ];

    render(<PatientNotes contact={contact} />);

    expect(screen.getAllByLabelText("Editar anotação")).toHaveLength(1);
  });
});
