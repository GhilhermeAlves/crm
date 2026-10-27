import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { PatientDocumentsTab } from "@/features/clinical/documents/components/PatientDocumentsTab";
import type { Contact } from "@/features/masterdata/contacts/types/contact.types";

const { previewMock, issueMock } = vi.hoisted(() => ({
  previewMock: vi.fn(),
  issueMock: vi.fn(),
}));

vi.mock("@/features/identity/auth/hooks/useAuth", () => ({
  useAuth: () => ({
    user: { id: "u1", name: "Dra. Ana", companyId: "co1", membershipRole: "AGENT", roles: [] },
  }),
}));

// O Select do Radix não é operável no jsdom; um input simples expõe o onValueChange.
vi.mock("@/components/ui/select", () => ({
  Select: ({ onValueChange }: { onValueChange: (value: string) => void }) => (
    <input aria-label="Modelo" onChange={(e) => onValueChange(e.target.value)} />
  ),
  SelectTrigger: ({ children }: { children: ReactNode }) => <>{children}</>,
  SelectValue: () => null,
  SelectContent: () => null,
  SelectItem: () => null,
}));

vi.mock("@/features/clinical/documents/hooks/usePatientDocuments", () => ({
  useDocumentTemplates: () => ({
    data: [{ id: "t1", type: "PRESCRIPTION", title: "Receita simples", body: "x", active: true }],
  }),
  usePatientDocuments: () => ({ data: [] }),
  usePreviewPatientDocument: () => ({ mutateAsync: previewMock }),
  useIssuePatientDocument: () => ({ mutate: issueMock, isPending: false }),
  useDeletePatientDocument: () => ({ mutate: vi.fn(), isPending: false }),
  useOpenPatientDocumentPdf: () => ({ mutate: vi.fn(), isPending: false }),
}));

const patient = { id: "c1", firstName: "Ana" } as Contact;

describe("PatientDocumentsTab", () => {
  beforeEach(() => {
    previewMock.mockReset();
    issueMock.mockReset();
    previewMock.mockResolvedValue({
      type: "PRESCRIPTION",
      title: "Receita",
      body: "Paciente: Ana\nAmoxicilina 500mg",
    });
  });

  it("chama a prévia ao escolher o modelo e emite com o corpo editado", async () => {
    render(<PatientDocumentsTab companyId="co1" contactId="c1" patient={patient} />);

    fireEvent.click(screen.getByText("Emitir documento"));
    fireEvent.change(screen.getByLabelText("Modelo"), { target: { value: "t1" } });

    expect(previewMock).toHaveBeenCalledWith("t1");
    const textarea = await waitFor(() => {
      const el = document.querySelector("textarea");
      expect(el?.value).toBe("Paciente: Ana\nAmoxicilina 500mg");
      return el as HTMLTextAreaElement;
    });

    fireEvent.change(textarea, { target: { value: "Paciente: Ana\nIbuprofeno 400mg" } });
    fireEvent.change(screen.getByLabelText(/Registro profissional/), {
      target: { value: "CRO-SP 12345" },
    });
    fireEvent.click(screen.getByText("Emitir"));

    expect(issueMock).toHaveBeenCalledWith(
      expect.objectContaining({
        templateId: "t1",
        type: "PRESCRIPTION",
        title: "Receita",
        body: "Paciente: Ana\nIbuprofeno 400mg",
        professionalName: "Dra. Ana",
        professionalRegistry: "CRO-SP 12345",
      }),
      expect.anything(),
    );
  });
});
