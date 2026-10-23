import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { CreateAppointmentDialog } from "@/features/sales/scheduling/components/CreateAppointmentDialog";
import type { Contact } from "@/features/masterdata/contacts/types/contact.types";

vi.mock("@/features/identity/auth/hooks/useAuth", () => ({
  useAuth: () => ({ user: { id: "u1", companyId: "co1" } }),
}));

const PROC_ID = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa";
vi.mock("@/features/masterdata/catalog/hooks/useCatalog", () => ({
  useCatalog: () => ({ data: { content: [{ id: PROC_ID, name: "Limpeza" }] } }),
}));

vi.mock("@/features/sales/tag/hooks/useTags", () => ({
  useCreateTag: () => ({ mutate: vi.fn(), isPending: false }),
}));

vi.mock("@/features/masterdata/contacts/registration/components/RegistrationLinkActions", () => ({
  RegistrationLinkActions: () => <button type="button">Enviar link de cadastro</button>,
}));

const HOST_ID = "11111111-1111-4111-8111-111111111111";
const CONTACT_ID = "22222222-2222-4222-8222-222222222222";

const contact = {
  id: CONTACT_ID,
  firstName: "Ana",
  lastName: "Souza",
  mobile: "(34) 99999-8888",
  phone: null,
} as unknown as Contact;

type Overrides = Partial<React.ComponentProps<typeof CreateAppointmentDialog>>;

function renderDialog(overrides: Overrides = {}) {
  const onSubmit = vi.fn();
  render(
    <CreateAppointmentDialog
      open
      onOpenChange={vi.fn()}
      isLoading={false}
      onSubmit={onSubmit}
      onSubmitBlock={vi.fn()}
      appointmentTypes={[]}
      members={[{ id: HOST_ID, name: "Dra. Bia" }]}
      contacts={[contact]}
      defaultStart="2026-10-26T10:00"
      defaultEnd="2026-10-26T11:00"
      defaultHostId={HOST_ID}
      defaultContactId={CONTACT_ID}
      workHoursStart="08:00"
      workHoursEnd="18:00"
      {...overrides}
    />,
  );
  return { onSubmit };
}

async function submit(onSubmit: ReturnType<typeof vi.fn>) {
  fireEvent.click(screen.getByRole("button", { name: "Confirmar" }));
  await waitFor(() => expect(onSubmit).toHaveBeenCalled());
  return onSubmit.mock.calls[0][0];
}

describe("CreateAppointmentDialog", () => {
  beforeEach(() => vi.clearAllMocks());

  it("inicia como Pendente e envia status SCHEDULED", async () => {
    const { onSubmit } = renderDialog();
    const pending = screen.getByRole("radio", { name: "Pendente" });
    expect(pending.getAttribute("aria-checked")).toBe("true");
    const values = await submit(onSubmit);
    expect(values.status).toBe("SCHEDULED");
    expect(values.recurrence).toBeNull();
  });

  it("mostra o envio do link de cadastro quando há paciente", () => {
    renderDialog();
    expect(screen.getByRole("button", { name: "Enviar link de cadastro" })).toBeTruthy();
  });

  it("pré-preenche o título com o nome do paciente", () => {
    renderDialog();
    expect((screen.getByLabelText("Título") as HTMLInputElement).value).toBe("Ana Souza");
  });

  it("envia recorrência semanal", async () => {
    const { onSubmit } = renderDialog();
    fireEvent.click(screen.getByLabelText("Repetir agendamento"));
    fireEvent.click(await screen.findByRole("option", { name: "Semanal" }));
    const values = await submit(onSubmit);
    expect(values.recurrence).toEqual({ frequency: "WEEKLY", interval: 1, endType: "NEVER" });
  });

  it("envia recorrência personalizada", async () => {
    const { onSubmit } = renderDialog();
    fireEvent.click(screen.getByLabelText("Repetir agendamento"));
    fireEvent.click(await screen.findByRole("option", { name: "Personalizado…" }));
    expect(await screen.findByText("Configurar recorrência")).toBeTruthy();
    fireEvent.click(screen.getByLabelText("Aumentar intervalo"));
    fireEvent.click(screen.getByRole("radio", { name: "Após" }));
    fireEvent.change(screen.getByLabelText("Número de ocorrências"), { target: { value: "5" } });
    fireEvent.click(screen.getByRole("button", { name: "Concluir" }));
    await waitFor(() => expect(screen.queryByText("Configurar recorrência")).toBeNull());
    const values = await submit(onSubmit);
    expect(values.recurrence).toEqual({
      frequency: "WEEKLY",
      interval: 2,
      endType: "AFTER_COUNT",
      count: 5,
    });
  });

  it("avisa quando o evento fica fora da faixa de horário", () => {
    renderDialog({ defaultStart: "2026-10-26T19:00", defaultEnd: "2026-10-26T20:00" });
    expect(screen.getByText(/Evento fora da faixa de horário/)).toBeTruthy();
  });

  it("não avisa dentro do expediente", () => {
    renderDialog();
    expect(screen.queryByText(/Evento fora da faixa de horário/)).toBeNull();
  });

  it("esconde a aba Bloqueio de Agenda sem onSubmitBlock", () => {
    renderDialog({ onSubmitBlock: undefined });
    expect(screen.queryByText("Bloqueio de Agenda")).toBeNull();
  });

  it("mostra a aba Bloqueio de Agenda com onSubmitBlock", () => {
    renderDialog();
    expect(screen.getByText("Bloqueio de Agenda")).toBeTruthy();
  });

  it("envia os procedimentos escolhidos", async () => {
    const { onSubmit } = renderDialog();
    fireEvent.click(screen.getByRole("button", { name: "Limpeza" }));
    const values = await submit(onSubmit);
    expect(values.catalogItemIds).toEqual([PROC_ID]);
  });

  it("envia contactPhone só quando o celular muda", async () => {
    const { onSubmit } = renderDialog();
    const values = await submit(onSubmit);
    expect(values.contactPhone).toBeUndefined();
  });

  it("envia o novo celular e bloqueia celular inválido", async () => {
    const { onSubmit } = renderDialog();
    const input = screen.getByLabelText(/Celular do\(a\) paciente/);
    fireEvent.change(input, { target: { value: "123" } });
    fireEvent.click(screen.getByRole("button", { name: "Confirmar" }));
    expect(await screen.findByText(/Celular inválido/)).toBeTruthy();
    expect(onSubmit).not.toHaveBeenCalled();
    fireEvent.change(input, { target: { value: "(34) 98888-7777" } });
    const values = await submit(onSubmit);
    expect(values.contactPhone).toBe("(34) 98888-7777");
  });
});
