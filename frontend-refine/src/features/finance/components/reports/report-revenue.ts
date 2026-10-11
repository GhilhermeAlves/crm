export const REVENUE_LINE_PALETTE = [
  "#2563eb",
  "#16a34a",
  "#f59e0b",
  "#7c3aed",
  "#dc2626",
  "#0891b2",
  "#db2777",
  "#65a30d",
  "#4f46e5",
  "#9333ea",
];

export type RevenueSeries = {
  key: string;
  name: string;
  color: string;
};

export type RevenueRow = {
  period: string;
  name: string;
  amountCents: number;
};

export type RevenueSeriesBuild = {
  data: Record<string, string | number>[];
  series: RevenueSeries[];
};

/**
 * Pivota as linhas (período, nome, valor) em uma série por nome, mantendo os
 * períodos na ordem retornada pelo backend e preenchendo zeros quando ausente.
 */
export function buildRevenueSeries(rows: RevenueRow[]): RevenueSeriesBuild {
  const series: RevenueSeries[] = [];
  const keyByName = new Map<string, string>();
  const periodOrder: string[] = [];
  const byPeriod = new Map<string, Record<string, number>>();

  for (const row of rows) {
    let key = keyByName.get(row.name);
    if (!key) {
      key = `s${series.length}`;
      keyByName.set(row.name, key);
      series.push({
        key,
        name: row.name,
        color: REVENUE_LINE_PALETTE[series.length % REVENUE_LINE_PALETTE.length],
      });
    }
    if (!byPeriod.has(row.period)) {
      byPeriod.set(row.period, {});
      periodOrder.push(row.period);
    }
    byPeriod.get(row.period)![key] = row.amountCents / 100;
  }

  const data = periodOrder.map((period) => ({ period, ...byPeriod.get(period) }));
  return { data, series };
}
