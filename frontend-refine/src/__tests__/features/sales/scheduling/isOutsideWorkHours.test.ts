import { describe, it, expect } from "vitest";
import { isOutsideWorkHours } from "@/features/sales/scheduling/components/CreateAppointmentDialog";

describe("isOutsideWorkHours", () => {
  it("dentro do horário não avisa", () => {
    expect(isOutsideWorkHours("08:00", "18:00", "08:00", "18:00")).toBe(false);
    expect(isOutsideWorkHours("10:00", "11:00", "08:00", "18:00")).toBe(false);
  });

  it("avisa quando começa antes ou termina depois", () => {
    expect(isOutsideWorkHours("07:30", "08:30", "08:00", "18:00")).toBe(true);
    expect(isOutsideWorkHours("17:30", "18:30", "08:00", "18:00")).toBe(true);
  });

  it("sem horário configurado não avisa", () => {
    expect(isOutsideWorkHours("06:00", "07:00")).toBe(false);
  });
});
