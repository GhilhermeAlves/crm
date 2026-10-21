import { describe, it, expect, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import {
  AppointmentDetailPanel,
  canMoveTo,
  whatsappUrl,
} from "@/features/sales/scheduling/components/AppointmentDetailPanel";
import type { Appointment } from "@/features/sales/scheduling/types/scheduling.types";
import type { Contact } from "@/features/masterdata/contacts/types/contact.types";

const appointment = {
  id: "a1",
  title: "Avaliação",
  startAt: "2026-10-22T13:00:00Z",
  endAt: "2026-10-22T14:00:00Z",
  status: "SCHEDULED",
  locationKind: null,
} as unknown as Appointment;

const contact = {
  id: "c1",
  firstName: "Ana",
  lastName: "Souza",
  phone: "(34) 99999-8888",
} as unknown as Contact;

function renderPanel(overrides: Partial<Appointment> = {}) {
  const onChangeStatus = vi.fn();
  const onDelete = vi.fn();
  render(
    <AppointmentDetailPanel
      appointment={{ ...appointment, ...overrides }}
      contact={contact}
      onClose={vi.fn()}
      onChangeStatus={onChangeStatus}
      onDelete={onDelete}
      canUpdate
      canDelete
    />,
  );
  return { onChangeStatus, onDelete };
}

describe("AppointmentDetailPanel", () => {
  it("confirma e marca presença com um clique", () => {
    const { onChangeStatus } = renderPanel();
    fireEvent.click(screen.getByRole("button", { name: "Confirmado" }));
    fireEvent.click(screen.getByRole("button", { name: /Compareceu/ }));
    expect(onChangeStatus).toHaveBeenNthCalledWith(1, "a1", "CONFIRMED");
    expect(onChangeStatus).toHaveBeenNthCalledWith(2, "a1", "COMPLETED");
  });

  it("pede confirmação antes de cancelar", () => {
    const { onChangeStatus } = renderPanel();
    fireEvent.click(screen.getByRole("button", { name: "Cancelado" }));
    expect(onChangeStatus).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: "Cancelar agendamento" }));
    expect(onChangeStatus).toHaveBeenCalledWith("a1", "CANCELED");
  });

  it("agendamento cancelado não tem ações de status", () => {
    renderPanel({ status: "CANCELED" });
    const disabled = (name: string | RegExp) =>
      (screen.getByRole("button", { name }) as HTMLButtonElement).disabled;
    expect(disabled(/Compareceu/)).toBe(true);
    expect(disabled(/Faltou/)).toBe(true);
    expect(disabled("Confirmado")).toBe(true);
  });

  it("mostra o link do WhatsApp do paciente", () => {
    renderPanel();
    expect(screen.getByRole("link", { name: "Conversar no WhatsApp" }).getAttribute("href")).toBe(
      "https://wa.me/5534999998888",
    );
  });
});

describe("regras de status", () => {
  it("segue o backend", () => {
    expect(canMoveTo("SCHEDULED", "CONFIRMED")).toBe(true);
    expect(canMoveTo("CONFIRMED", "CONFIRMED")).toBe(false);
    expect(canMoveTo("COMPLETED", "CONFIRMED")).toBe(false);
    expect(canMoveTo("CONFIRMED", "SCHEDULED")).toBe(false);
    expect(canMoveTo("CANCELED", "COMPLETED")).toBe(false);
    expect(canMoveTo("COMPLETED", "NO_SHOW")).toBe(true);
  });

  it("monta o número do WhatsApp", () => {
    expect(whatsappUrl("+55 34 99999-8888")).toBe("https://wa.me/5534999998888");
    expect(whatsappUrl("123")).toBeNull();
  });
});
