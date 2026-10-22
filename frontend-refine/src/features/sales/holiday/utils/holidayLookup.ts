import { format } from "date-fns";
import { getHolidayForDate } from "@/lib/holidays";
import type { CompanyHoliday } from "../types/holiday.types";

/**
 * Nome do feriado de um dia: o da clínica (já projetado no período pelo backend, inclusive
 * os anuais) tem prioridade sobre o nacional.
 */
export function buildHolidayLookup(
  clinicHolidays: Pick<CompanyHoliday, "date" | "name">[],
): (day: Date) => string | undefined {
  const byDate = new Map(clinicHolidays.map((h) => [h.date, h.name]));
  return (day) => byDate.get(format(day, "yyyy-MM-dd")) ?? getHolidayForDate(day)?.name;
}
