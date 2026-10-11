import type { ReportGranularity } from "../../types/finance.types";

export function formatReportNumber(value: number): string {
  return value.toLocaleString("pt-BR");
}

export function formatReportPercent(value: number): string {
  return `${value.toLocaleString("pt-BR", { maximumFractionDigits: 1 })}%`;
}

/** Formata valores em reais de forma compacta (ex.: "R$ 9 mil"). */
export function formatCompactBRL(value: number): string {
  const abs = Math.abs(value);
  const sign = value < 0 ? "-" : "";
  if (abs >= 1000) {
    const thousands = new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 1 }).format(
      abs / 1000,
    );
    return `${sign}R$ ${thousands} mil`;
  }
  return `${sign}R$ ${new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 0 }).format(abs)}`;
}

/** Formata o rótulo do eixo X a partir do período retornado pelo backend. */
export function formatPeriodTick(period: string, granularity: ReportGranularity): string {
  if (granularity === "MONTH" && period.length === 7) {
    const [year, month] = period.split("-");
    return `${month}/${year.slice(2)}`;
  }
  if (period.length === 10) {
    const [, month, day] = period.split("-");
    return `${day}/${month}`;
  }
  return period;
}
