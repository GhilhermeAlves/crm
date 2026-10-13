import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { PatientNotes } from "@/features/masterdata/contacts/components/PatientNotes";
import type { ContactNote } from "@/features/masterdata/contacts/types/patient.types";
import type { Contact } from "@/features/masterdata/contacts/types/contact.types";

const { notesState, createMock, updateMock, deleteMock } = vi.hoisted(() => ({
  notesState: { value: [] as ContactNote[] },
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

vi.mock("@/features/masterdata/contacts/hooks/usePatient", () => ({
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

    expect(createMock).toHaveBeenCalledWith({ body: "Nova observação" }, expect.anything());
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
