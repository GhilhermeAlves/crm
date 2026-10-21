import { describe, it, expect } from "vitest";
import { filterAppointments } from "@/features/sales/scheduling/utils/filterAppointments";
import type {
  Appointment,
  AppointmentStatus,
} from "@/features/sales/scheduling/types/scheduling.types";

const appt = (over: Partial<Appointment>) =>
  ({
    id: "a",
    hostId: "h1",
    contactId: null,
    title: "Consulta",
    status: "SCHEDULED",
    ...over,
  }) as Appointment;

const list = [
  appt({ id: "1", contactId: "c1", status: "CONFIRMED" }),
  appt({ id: "2", hostId: "h2", status: "SCHEDULED" }),
  appt({ id: "3", title: "Remoção de sutura", status: "NO_SHOW" }),
];
const contacts = [{ id: "c1", firstName: "José", lastName: "Araújo" }];
const base = { hostIds: new Set(["h1", "h2"]), statuses: new Set<AppointmentStatus>(), search: "" };

const ids = (r: Appointment[]) => r.map((a) => a.id);

describe("filterAppointments", () => {
  it("filtra por profissional", () => {
    expect(ids(filterAppointments(list, { ...base, hostIds: new Set(["h2"]) }))).toEqual(["2"]);
  });

  it("status vazio mostra todos; com seleção, só os escolhidos", () => {
    expect(ids(filterAppointments(list, base))).toEqual(["1", "2", "3"]);
    expect(
      ids(
        filterAppointments(list, {
          ...base,
          statuses: new Set<AppointmentStatus>(["CONFIRMED", "NO_SHOW"]),
        }),
      ),
    ).toEqual(["1", "3"]);
  });

  it("busca pelo nome do paciente sem acento e pelo título", () => {
    expect(ids(filterAppointments(list, { ...base, search: "jose araujo" }, contacts))).toEqual([
      "1",
    ]);
    expect(ids(filterAppointments(list, { ...base, search: "REMOCAO" }, contacts))).toEqual(["3"]);
  });
});
