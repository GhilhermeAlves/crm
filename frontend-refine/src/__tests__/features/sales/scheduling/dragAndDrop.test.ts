import { describe, it, expect } from "vitest";
import { format } from "date-fns";
import {
  computeDropTimes,
  isDraggable,
} from "@/features/sales/scheduling/components/WeeklyCalendar";
import type { Appointment } from "@/features/sales/scheduling/types/scheduling.types";

const appt = {
  id: "a1",
  startAt: "2026-10-22T09:00:00",
  endAt: "2026-10-22T09:45:00",
  status: "CONFIRMED",
} as Appointment;

const hm = (d: Date) => format(d, "yyyy-MM-dd HH:mm");

describe("arrastar e soltar", () => {
  it("mantém a duração e encaixa em 5 minutos", () => {
    // 60px por hora, faixa começando às 8h: 152px ≈ 10h32 → 10h30.
    const { start, end } = computeDropTimes(appt, new Date(2026, 9, 23), 152, 60, 8);
    expect(hm(start)).toBe("2026-10-23 10:30");
    expect(hm(end)).toBe("2026-10-23 11:15");
  });

  it("não deixa começar antes da meia-noite", () => {
    const { start } = computeDropTimes(appt, new Date(2026, 9, 23), -500, 60, 0);
    expect(hm(start)).toBe("2026-10-23 00:00");
  });

  it("só agendamentos em aberto podem ser arrastados", () => {
    expect(isDraggable(appt)).toBe(true);
    expect(isDraggable({ ...appt, status: "SCHEDULED" })).toBe(true);
    expect(isDraggable({ ...appt, status: "CANCELED" })).toBe(false);
    expect(isDraggable({ ...appt, status: "COMPLETED" })).toBe(false);
    expect(isDraggable({ ...appt, status: "NO_SHOW" })).toBe(false);
  });
});
