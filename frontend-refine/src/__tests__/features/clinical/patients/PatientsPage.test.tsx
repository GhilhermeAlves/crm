import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import PatientsPage from "@/app/(dashboard)/patients/page";
import type { Contact } from "@/features/masterdata/contacts/types/contact.types";

const { patientsState, searchCalls } = vi.hoisted(() => ({
  patientsState: { value: [] as Partial<Contact>[] },
  searchCalls: [] as string[],
}));

vi.mock("next/link", () => ({
  default: ({ href, children, ...rest }: Record<string, unknown>) => (
    <a href={String(href)} {...rest}>
      {children as React.ReactNode}
    </a>
  ),
}));

vi.mock("@/features/identity/auth/hooks/useAuth", () => ({
  useAuth: () => ({ user: { id: "u1", companyId: "co1" } }),
}));

vi.mock("@/features/masterdata/contacts/hooks/useContacts", () => ({
  useContactPermissions: () => ({ canCreate: true, canUpdate: true, canDelete: false }),
}));

vi.mock("@/features/clinical/patients/hooks/usePatient", () => ({
  usePatients: (_companyId: string | null, search: string) => {
    searchCalls.push(search);
    return { data: patientsState.value, isLoading: false, isError: false, refetch: vi.fn() };
  },
}));

const ana: Partial<Contact> = {
  id: "p1",
  firstName: "Ana",
  lastName: "Souza",
  cpf: "11122233344",
  mobile: "34999990000",
  recordNumber: "PRT-7",
  patientSince: "2025-01-10",
};

beforeEach(() => {
  patientsState.value = [];
  searchCalls.length = 0;
});

describe("PatientsPage", () => {
  it("lista os pacientes com contagem e link para a ficha", () => {
    patientsState.value = [ana];
    render(<PatientsPage />);

    expect(screen.getByRole("heading", { name: "Lista de Pacientes (1)" })).toBeTruthy();
    expect(screen.getByText("Ana Souza").closest("a")?.getAttribute("href")).toBe("/patients/p1");
    expect(screen.getByText("PRT-7")).toBeTruthy();
    expect(screen.getByText("10/01/2025")).toBeTruthy();
    expect(screen.getByText("Novo paciente").closest("a")?.getAttribute("href")).toBe(
      "/patients/new",
    );
  });

  it("só busca ao confirmar a pesquisa", () => {
    render(<PatientsPage />);

    fireEvent.change(screen.getByLabelText("Buscar paciente"), { target: { value: " prt-7 " } });
    expect(searchCalls.at(-1)).toBe("");
    fireEvent.click(screen.getByRole("button", { name: "Buscar" }));

    expect(searchCalls.at(-1)).toBe("prt-7");
  });

  it("mostra estado vazio sem pacientes", () => {
    render(<PatientsPage />);

    expect(screen.getByText("Nenhum paciente cadastrado")).toBeTruthy();
  });
});
