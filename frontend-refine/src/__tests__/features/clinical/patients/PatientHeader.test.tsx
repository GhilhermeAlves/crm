import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import { PatientHeader } from "@/features/clinical/patients/components/PatientHeader";
import type { Contact } from "@/features/masterdata/contacts/types/contact.types";

const { anamnesisState } = vi.hoisted(() => ({
  anamnesisState: { value: null as { alerts: string[] } | null },
}));

vi.mock("@/features/identity/auth/hooks/useCompanyId", () => ({ useCompanyId: () => "co1" }));

vi.mock("@/features/identity/auth/hooks/useAuthorization", () => ({
  useAuthorization: () => ({ can: () => true }),
}));

vi.mock("@/features/masterdata/contacts/hooks/useContacts", () => ({
  useContactPermissions: () => ({ canUpdate: true }),
}));

vi.mock("@/features/clinical/anamnesis/hooks/usePatientAnamnesis", () => ({
  useCurrentPatientAnamnesis: () => ({ data: anamnesisState.value }),
}));

vi.mock("@/features/clinical/patients/hooks/usePatient", () => ({
  usePatientPhoto: () => ({ data: null }),
  useUploadPatientPhoto: () => ({ mutate: vi.fn(), isPending: false }),
  useDeletePatientPhoto: () => ({ mutate: vi.fn(), isPending: false }),
}));

const contact = { id: "c1", firstName: "Ana", lastName: "Silva", mobile: null } as Contact;

describe("PatientHeader", () => {
  beforeEach(() => {
    anamnesisState.value = null;
  });

  it("mostra a quantidade de alertas da anamnese", () => {
    anamnesisState.value = { alerts: ["Alergia a penicilina", "Hipertensão"] };
    render(<PatientHeader contact={contact} />);

    expect(screen.getByText("2 alerta(s) na anamnese")).toBeTruthy();
    expect(screen.queryByText("Sem anamnese")).toBeNull();
  });

  it("mostra 'Sem anamnese' quando não há anamnese concluída", () => {
    render(<PatientHeader contact={contact} />);

    expect(screen.getByText("Sem anamnese")).toBeTruthy();
  });
});
