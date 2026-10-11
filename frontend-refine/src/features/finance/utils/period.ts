import { endOfMonth, endOfWeek, format, startOfMonth, startOfWeek } from "date-fns";
import { ptBR } from "date-fns/locale";

export type PeriodKey = "today" | "week" | "month" | "last-month" | "3-months" | "custom";

export type DateRangeValue = {
  from: string;
  to: string;
};

export const PERIOD_KEYS: PeriodKey[] = [
  "today",
  "week",
  "month",
  "last-month",
  "3-months",
  "custom",
];

export const PERIOD_SHORTCUTS: { value: PeriodKey; label: string }[] = [
  { value: "today", label: "Hoje" },
  { value: "week", label: "Esta semana" },
  { value: "month", label: "Este mês" },
  { value: "last-month", label: "Mês passado" },
  { value: "3-months", label: "Últimos 3 meses" },
  { value: "custom", label: "Personalizado" },
];

const toISODate = (date: Date): string => format(date, "yyyy-MM-dd");

const toDisplayDate = (iso: string): string => {
  const [year, month, day] = iso.split("-");
  return `${day}/${month}/${year}`;
};

/** Resolve um período (com atalho ou intervalo customizado) em `{ from, to }` no formato `yyyy-MM-dd`. */
export function resolvePeriod(
  period: PeriodKey,
  custom?: { from: string; to: string } | null,
  now: Date = new Date(),
): { from: string; to: string } {
  switch (period) {
    case "today": {
      const day = toISODate(now);
      return { from: day, to: day };
    }
    case "week": {
      const from = startOfWeek(now, { weekStartsOn: 1 });
      const to = endOfWeek(now, { weekStartsOn: 1 });
      return { from: toISODate(from), to: toISODate(to) };
    }
    case "month": {
      return { from: toISODate(startOfMonth(now)), to: toISODate(endOfMonth(now)) };
    }
    case "last-month": {
      const reference = new Date(now.getFullYear(), now.getMonth() - 1, 1);
      return { from: toISODate(startOfMonth(reference)), to: toISODate(endOfMonth(reference)) };
    }
    case "3-months": {
      const from = new Date(now.getFullYear(), now.getMonth() - 2, 1);
      return { from: toISODate(startOfMonth(from)), to: toISODate(endOfMonth(now)) };
    }
    case "custom":
      return custom ?? { from: toISODate(startOfMonth(now)), to: toISODate(endOfMonth(now)) };
    default:
      return { from: toISODate(startOfMonth(now)), to: toISODate(endOfMonth(now)) };
  }
}

/** Rótulo legível do período atualmente selecionado. */
export function periodLabel(
  period: PeriodKey,
  custom?: { from: string; to: string } | null,
  now: Date = new Date(),
): string {
  switch (period) {
    case "today":
      return `Hoje (${toDisplayDate(toISODate(now))})`;
    case "week":
      return "Esta semana";
    case "month":
      return `Este mês (${format(now, "MMMM/yyyy", { locale: ptBR })})`;
    case "last-month":
      return "Mês passado";
    case "3-months":
      return "Últimos 3 meses";
    case "custom": {
      const range = resolvePeriod("custom", custom, now);
      return `${toDisplayDate(range.from)} – ${toDisplayDate(range.to)}`;
    }
    default:
      return "Este mês";
  }
}

/** Converte um `Date` em `{ from, to }` no formato `yyyy-MM-dd` (para períodos customizados). */
export function toPeriodRange(from: Date, to: Date): { from: string; to: string } {
  return { from: toISODate(from), to: toISODate(to) };
}
