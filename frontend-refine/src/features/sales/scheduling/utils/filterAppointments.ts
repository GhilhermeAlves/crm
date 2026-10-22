import type { Appointment, AppointmentStatus } from "../types/scheduling.types";

type ContactName = { id: string; firstName?: string | null; lastName?: string | null };

export type AppointmentFilters = {
  hostIds: Set<string>;
  /** Vazio = todos os status. */
  statuses: Set<AppointmentStatus>;
  /** Busca por nome do paciente ou título, sem diferenciar acento e maiúsculas. */
  search: string;
  /** Marcadores: vazio/ausente = todos; com seleção, basta ter um dos marcados. */
  tagIds?: Set<string>;
};

const normalize = (text: string) =>
  text
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim();

export function filterAppointments(
  appointments: Appointment[],
  { hostIds, statuses, search, tagIds }: AppointmentFilters,
  contacts: ContactName[] = [],
): Appointment[] {
  const term = normalize(search);
  const namesById = new Map(
    contacts.map((c) => [c.id, normalize(`${c.firstName ?? ""} ${c.lastName ?? ""}`)]),
  );

  return appointments.filter((a) => {
    if (!hostIds.has(a.hostId)) return false;
    if (statuses.size > 0 && !statuses.has(a.status)) return false;
    if (tagIds && tagIds.size > 0 && !(a.tagIds ?? []).some((id) => tagIds.has(id))) return false;
    if (!term) return true;
    const contactName = a.contactId ? (namesById.get(a.contactId) ?? "") : "";
    return contactName.includes(term) || normalize(a.title).includes(term);
  });
}
