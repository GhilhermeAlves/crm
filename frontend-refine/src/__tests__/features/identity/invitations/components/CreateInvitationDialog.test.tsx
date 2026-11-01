import { describe, it, expect, vi, beforeEach } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { CreateInvitationDialog } from "@/features/identity/invitations/components/CreateInvitationDialog";
import { invitableRoles } from "@/features/identity/invitations/types/invitation.types";

const { createMutate } = vi.hoisted(() => ({ createMutate: vi.fn() }));

vi.mock("@/features/identity/invitations/hooks/useInvitations", () => ({
  useCreateInvitation: () => ({ mutate: createMutate, isPending: false }),
}));

vi.mock("@/features/identity/auth/hooks/useAuthorization", () => ({
  useAuthorization: () => ({ isSuperAdmin: false, isCrmAdmin: false }),
}));

describe("invitableRoles", () => {
  it("esconde ADMIN de quem não é super administrador", () => {
    expect(invitableRoles(false)).toEqual(["MANAGER", "AGENT", "VIEWER"]);
  });

  it("mostra ADMIN para o super administrador e o administrador do CRM", () => {
    expect(invitableRoles(true)).toContain("ADMIN");
  });
});

describe("CreateInvitationDialog", () => {
  beforeEach(() => createMutate.mockReset());

  it("envia o convite sem campo de especialidade", async () => {
    render(<CreateInvitationDialog companyId="co1" />);
    fireEvent.click(screen.getByRole("button", { name: "Convidar membro" }));
    expect(screen.queryByText(/Especialidade/)).toBeNull();

    fireEvent.change(screen.getByLabelText("E-mail"), { target: { value: "ana@clinica.com" } });
    fireEvent.click(screen.getByRole("button", { name: "Enviar convite" }));

    await waitFor(() => expect(createMutate).toHaveBeenCalled());
    const payload = createMutate.mock.calls[0][0] as Record<string, unknown>;
    expect(payload.email).toBe("ana@clinica.com");
    expect(payload.specialty).toBeUndefined();
    expect(payload.sendEmail).toBe(true);
  });

  it("permite desmarcar o envio por e-mail", async () => {
    render(<CreateInvitationDialog companyId="co1" />);
    fireEvent.click(screen.getByRole("button", { name: "Convidar membro" }));
    fireEvent.change(screen.getByLabelText("E-mail"), { target: { value: "ana@clinica.com" } });
    fireEvent.click(screen.getByLabelText("Enviar convite por e-mail"));
    fireEvent.click(screen.getByRole("button", { name: "Enviar convite" }));

    await waitFor(() => expect(createMutate).toHaveBeenCalled());
    const payload = createMutate.mock.calls[0][0] as Record<string, unknown>;
    expect(payload.sendEmail).toBe(false);
  });

  it("exibe o link com copiar e WhatsApp após criar o convite", async () => {
    createMutate.mockImplementation(
      (
        _payload: unknown,
        options?: { onSuccess?: (data: { invitation: { email: string }; url: string }) => void },
      ) => {
        options?.onSuccess?.({
          invitation: { email: "ana@clinica.com" },
          url: "/convite/tok",
        });
      },
    );

    render(<CreateInvitationDialog companyId="co1" />);
    fireEvent.click(screen.getByRole("button", { name: "Convidar membro" }));
    fireEvent.change(screen.getByLabelText("E-mail"), { target: { value: "ana@clinica.com" } });
    fireEvent.click(screen.getByRole("button", { name: "Enviar convite" }));

    expect(await screen.findByText("Convite criado")).toBeTruthy();
    expect(screen.getByRole("button", { name: /Copiar link/ })).toBeTruthy();
    expect(screen.getByRole("button", { name: /Enviar por WhatsApp/ })).toBeTruthy();
  });
});
