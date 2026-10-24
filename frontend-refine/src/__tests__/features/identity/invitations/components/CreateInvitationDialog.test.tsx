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
  });
});
