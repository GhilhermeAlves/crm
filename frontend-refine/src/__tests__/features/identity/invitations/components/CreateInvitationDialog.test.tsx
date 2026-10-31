import { describe, it, expect, vi, beforeEach } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { CreateInvitationDialog } from "@/features/identity/invitations/components/CreateInvitationDialog";

const { createMutate } = vi.hoisted(() => ({ createMutate: vi.fn() }));

vi.mock("@/features/identity/invitations/hooks/useInvitations", () => ({
  useCreateInvitation: () => ({ mutate: createMutate, isPending: false }),
}));

describe("CreateInvitationDialog", () => {
  beforeEach(() => createMutate.mockReset());

  it("envia o convite sem especialidade quando ela não é informada", async () => {
    render(<CreateInvitationDialog companyId="co1" />);
    fireEvent.click(screen.getByRole("button", { name: "Convidar membro" }));
    expect(screen.getByText("Especialidade (opcional)")).toBeTruthy();

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
