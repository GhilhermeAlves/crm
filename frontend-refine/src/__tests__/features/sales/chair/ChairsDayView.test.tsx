import { describe, it, expect, vi } from "vitest";
import { render, screen, fireEvent, within } from "@testing-library/react";
import { ChairsDayView, groupByChair } from "@/features/sales/chair/components/ChairsDayView";
import type { Chair } from "@/features/sales/chair/types/chair.types";
import type { Appointment } from "@/features/sales/scheduling/types/scheduling.types";

const chair = (id: string, name: string, sortOrder: number, active = true) =>
  ({ id, name, sortOrder, active, color: "#0EA5E9" }) as Chair;

const appt = (id: string, startAt: string, chairId: string | null) =>
  ({
    id,
    title: `Consulta ${id}`,
    hostId: "h1",
    startAt,
    endAt: startAt.replace(/T(\d\d)/, (_, h) => `T${String(Number(h) + 1).padStart(2, "0")}`),
    status: "SCHEDULED",
    chairId,
  }) as Appointment;

const day = new Date(2026, 9, 22);
const chairs = [
  chair("c2", "Cadeira 2", 1),
  chair("c1", "Cadeira 1", 0),
  chair("c3", "Inativa", 2, false),
];
const appointments = [
  appt("b", "2026-10-22T10:00:00", "c1"),
  appt("a", "2026-10-22T08:00:00", "c1"),
  appt("x", "2026-10-22T09:00:00", null),
  appt("i", "2026-10-22T11:00:00", "c3"),
  appt("outro-dia", "2026-10-23T08:00:00", "c1"),
];

describe("groupByChair", () => {
  it("uma coluna por cadeira ativa em ordem, e as sem cadeira (ou de cadeira inativa) no fim", () => {
    const columns = groupByChair(day, chairs, appointments);

    expect(columns.map((c) => c.title)).toEqual(["Cadeira 1", "Cadeira 2", "Sem cadeira"]);
    expect(columns[0].items.map((a) => a.id)).toEqual(["a", "b"]);
    expect(columns[1].items).toEqual([]);
    expect(columns[2].items.map((a) => a.id)).toEqual(["x", "i"]);
  });
});

describe("ChairsDayView", () => {
  it("abre o agendamento ao clicar", () => {
    const onClick = vi.fn();
    render(
      <ChairsDayView
        date={day}
        chairs={chairs}
        appointments={appointments}
        onAppointmentClick={onClick}
      />,
    );

    const column = screen.getByRole("region", { name: "Cadeira 1" });
    fireEvent.click(within(column).getByText("Consulta a"));
    expect(onClick).toHaveBeenCalledWith(expect.objectContaining({ id: "a" }));
    expect(
      within(screen.getByRole("region", { name: "Cadeira 2" })).getByText("Livre"),
    ).toBeTruthy();
  });

  it("sem cadeiras ativas, orienta onde cadastrar", () => {
    render(<ChairsDayView date={day} chairs={[]} appointments={[]} onAppointmentClick={vi.fn()} />);
    expect(screen.getByText(/Configurações → Agenda → Cadeiras/)).toBeTruthy();
  });
});
