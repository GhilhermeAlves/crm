import { endOfMonth, format, startOfMonth } from "date-fns";
import { ptBR } from "date-fns/locale";

/** Atalhos de período usados pelos relatórios (Prompt 9/10). */
export type ReportPeriodKey =
  "this-month" | "last-month" | "last-2-months" | "last-3-months" | "this-year" | "custom";

export type ReportPeriodRange = {
  from: string;
  to: string;
};

export const REPORT_PERIOD_OPTIONS: { value: ReportPeriodKey; label: string }[] = [
  { value: "this-month", label: "Este mês" },
  { value: "last-month", label: "Mês passado" },
  { value: "last-2-months", label: "2 meses anteriores ou este mês" },
  { value: "last-3-months", label: "Últimos 3 meses" },
  { value: "this-year", label: "Este ano" },
];

export const REPORT_PERIOD_KEYS: ReportPeriodKey[] = [
  ...REPORT_PERIOD_OPTIONS.map((option) => option.value),
  "custom",
];

const toISODate = (date: Date): string => format(date, "yyyy-MM-dd");

const toDisplayDate = (iso: string): string => {
  const [year, month, day] = iso.split("-");
  return `${day}/${month}/${year}`;
};

/** Resolve um período de relatório em `{ from, to }` no formato `yyyy-MM-dd`. */
export function resolveReportPeriod(
  period: ReportPeriodKey,
  custom?: ReportPeriodRange | null,
  now: Date = new Date(),
): ReportPeriodRange {
  const year = now.getFullYear();
  const month = now.getMonth();

  switch (period) {
    case "this-month":
      return { from: toISODate(startOfMonth(now)), to: toISODate(endOfMonth(now)) };
    case "last-month": {
      const reference = new Date(year, month - 1, 1);
      return { from: toISODate(startOfMonth(reference)), to: toISODate(endOfMonth(reference)) };
    }
    case "last-2-months":
      return { from: toISODate(new Date(year, month - 1, 1)), to: toISODate(endOfMonth(now)) };
    case "last-3-months":
      return { from: toISODate(new Date(year, month - 2, 1)), to: toISODate(endOfMonth(now)) };
    case "this-year":
      return { from: toISODate(new Date(year, 0, 1)), to: toISODate(new Date(year, 11, 31)) };
    case "custom":
      return custom ?? { from: toISODate(startOfMonth(now)), to: toISODate(endOfMonth(now)) };
    default:
      return { from: toISODate(startOfMonth(now)), to: toISODate(endOfMonth(now)) };
  }
}

/** Rótulo legível do período selecionado. */
export function reportPeriodLabel(
  period: ReportPeriodKey,
  custom?: ReportPeriodRange | null,
  now: Date = new Date(),
): string {
  switch (period) {
    case "this-month":
      return `Este mês (${format(now, "MMMM/yyyy", { locale: ptBR })})`;
    case "last-month":
      return "Mês passado";
    case "last-2-months":
      return "2 meses anteriores ou este mês";
    case "last-3-months":
      return "Últimos 3 meses";
    case "this-year":
      return `Este ano (${now.getFullYear()})`;
    case "custom": {
      const range = resolveReportPeriod("custom", custom, now);
      return `${toDisplayDate(range.from)} – ${toDisplayDate(range.to)}`;
    }
    default:
      return "Este mês";
  }
}
