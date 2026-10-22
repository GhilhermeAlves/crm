import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { ContactPatientAction } from "@/features/clinical/patients/components/ContactPatientAction";

const { modulesState, pushMock, mutateMock } = vi.hoisted(() => ({
  modulesState: { enabled: null as string[] | null },
  pushMock: vi.fn(),
  mutateMock: vi.fn(),
}));

vi.mock("next/navigation", () => ({ useRouter: () => ({ push: pushMock }) }));

vi.mock("next/link", () => ({
  default: ({ href, children, ...rest }: Record<string, unknown>) => (
    <a href={String(href)} {...rest}>
      {children as React.ReactNode}
    </a>
  ),
}));

vi.mock("@/features/identity/modules/hooks/useCompanyModules", () => ({
  useActiveCompanyModules: () => ({ enabledModules: modulesState.enabled }),
}));

vi.mock("@/features/masterdata/contacts/hooks/useContacts", () => ({
  useContactPermissions: () => ({ canUpdate: true }),
}));

vi.mock("@/features/clinical/patients/hooks/usePatient", () => ({
  useMakePatient: () => ({ mutate: mutateMock, isPending: false }),
}));

beforeEach(() => {
  modulesState.enabled = null;
  pushMock.mockReset();
  mutateMock.mockReset();
});

describe("ContactPatientAction", () => {
  it("não aparece em empresa sem o módulo Pacientes", () => {
    modulesState.enabled = ["CRM"];
    const { container } = render(
      <ContactPatientAction companyId="co1" contactId="c1" isPatient={false} />,
    );

    expect(container.textContent).toBe("");
  });

  it("não aparece enquanto os módulos não carregaram", () => {
    const { container } = render(
      <ContactPatientAction companyId="co1" contactId="c1" isPatient={false} />,
    );

    expect(container.textContent).toBe("");
  });

  it("leva à ficha quando o contato já é paciente", () => {
    modulesState.enabled = ["CRM", "PATIENTS"];
    render(<ContactPatientAction companyId="co1" contactId="c1" isPatient />);

    expect(screen.getByText("Ver ficha do paciente").closest("a")?.getAttribute("href")).toBe(
      "/patients/c1",
    );
  });

  it("torna paciente e abre a ficha", () => {
    modulesState.enabled = ["PATIENTS"];
    mutateMock.mockImplementation((_id: string, opts: { onSuccess: () => void }) =>
      opts.onSuccess(),
    );
    render(<ContactPatientAction companyId="co1" contactId="c1" isPatient={false} />);

    fireEvent.click(screen.getByRole("button", { name: /Tornar paciente/ }));

    expect(mutateMock).toHaveBeenCalledWith("c1", expect.any(Object));
    expect(pushMock).toHaveBeenCalledWith("/patients/c1");
  });
});
