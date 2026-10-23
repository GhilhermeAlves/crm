import { describe, it, expect, vi } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { CreateAppointmentDialog } from "@/features/sales/scheduling/components/CreateAppointmentDialog";

vi.mock("@/features/identity/auth/hooks/useAuth", () => ({
  useAuth: () => ({ user: { id: "u1", companyId: "co1" } }),
}));

vi.mock("@/features/masterdata/catalog/hooks/useCatalog", () => ({
  useCatalog: () => ({ data: { content: [] } }),
}));

vi.mock("@/features/sales/tag/hooks/useTags", () => ({
  useCreateTag: () => ({ mutate: vi.fn(), isPending: false }),
}));

const HOST = "11111111-1111-4111-8111-111111111111";
const TAG_A = "22222222-2222-4222-8222-222222222222";
const TAG_B = "33333333-3333-4333-8333-333333333333";

function renderDialog(props: Partial<Parameters<typeof CreateAppointmentDialog>[0]> = {}) {
  const onSubmit = vi.fn();
  render(
    <CreateAppointmentDialog
      open
      onOpenChange={vi.fn()}
      isLoading={false}
      onSubmit={onSubmit}
      appointmentTypes={[]}
      members={[{ id: HOST, name: "Dra. Raquel" }]}
      contacts={[]}
      defaultStart="2026-10-22T10:00"
      defaultEnd="2026-10-22T10:30"
      defaultHostId={HOST}
      tags={[
        { id: TAG_A, name: "Ortodontia", color: "#22C55E" },
        { id: TAG_B, name: "Implante", color: null },
      ]}
      {...props}
    />,
  );
  return { onSubmit };
}

// O formulário (Sheet + calendário) é pesado de renderizar no jsdom: 2–7s por teste.
describe("CreateAppointmentDialog — marcadores e cadeira", { timeout: 20_000 }, () => {
  it("envia os marcadores escolhidos", async () => {
    const { onSubmit } = renderDialog();

    fireEvent.change(screen.getByLabelText("Título"), { target: { value: "Avaliação" } });
    const orto = screen.getByRole("button", { name: "Ortodontia" });
    fireEvent.click(orto);
    expect(orto.getAttribute("aria-pressed")).toBe("true");
    fireEvent.click(screen.getByRole("button", { name: "Confirmar" }));

    await waitFor(() => expect(onSubmit).toHaveBeenCalled());
    expect(onSubmit.mock.calls[0][0].tagIds).toEqual([TAG_A]);
    expect(onSubmit.mock.calls[0][0].chairId).toBeUndefined();
  });

  it("clicar de novo tira o marcador", () => {
    renderDialog();
    const orto = screen.getByRole("button", { name: "Ortodontia" });
    fireEvent.click(orto);
    fireEvent.click(orto);
    expect(orto.getAttribute("aria-pressed")).toBe("false");
  });

  it("sem cadeiras, o campo não aparece; sem marcadores, só o + Novo marcador", () => {
    renderDialog({ tags: [], chairs: [] });
    expect(screen.queryByLabelText("Cadeira")).toBeNull();
    expect(screen.queryByRole("button", { name: "Ortodontia" })).toBeNull();
    expect(screen.getByRole("button", { name: "Novo marcador" })).toBeTruthy();
  });

  it("com cadeiras, mostra o campo Cadeira", () => {
    renderDialog({ chairs: [{ id: TAG_B, name: "Cadeira 1", color: null }] });
    expect(screen.getByLabelText("Cadeira")).toBeTruthy();
  });
});
