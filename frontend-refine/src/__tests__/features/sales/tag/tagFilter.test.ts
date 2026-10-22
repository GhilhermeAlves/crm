import { describe, it, expect } from "vitest";
import { filterAppointments } from "@/features/sales/scheduling/utils/filterAppointments";
import type {
  Appointment,
  AppointmentStatus,
} from "@/features/sales/scheduling/types/scheduling.types";

const appt = (id: string, tagIds?: string[]) =>
  ({ id, hostId: "h1", title: "Consulta", status: "SCHEDULED", tagIds }) as Appointment;

const list = [appt("1", ["orto"]), appt("2", ["orto", "implante"]), appt("3", []), appt("4")];
const base = { hostIds: new Set(["h1"]), statuses: new Set<AppointmentStatus>(), search: "" };
const ids = (r: Appointment[]) => r.map((a) => a.id);

describe("filtro por marcador", () => {
  it("sem seleção mostra todos, inclusive sem marcador", () => {
    expect(ids(filterAppointments(list, base))).toEqual(["1", "2", "3", "4"]);
    expect(ids(filterAppointments(list, { ...base, tagIds: new Set() }))).toEqual([
      "1",
      "2",
      "3",
      "4",
    ]);
  });

  it("com seleção, basta ter um dos marcadores escolhidos", () => {
    expect(ids(filterAppointments(list, { ...base, tagIds: new Set(["implante"]) }))).toEqual([
      "2",
    ]);
    expect(
      ids(filterAppointments(list, { ...base, tagIds: new Set(["orto", "implante"]) })),
    ).toEqual(["1", "2"]);
  });
});
