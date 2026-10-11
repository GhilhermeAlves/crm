"use client";

import {
  Bar,
  CartesianGrid,
  ComposedChart,
  LabelList,
  Line,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

export type CashFlowPoint = {
  period: string;
  income: number;
  openReceivables: number;
  expense: number;
  openPayables: number;
  periodBalance: number;
  accumulatedBalance: number;
};

export const CASH_FLOW_COLORS = {
  income: "#15803d",
  openReceivables: "#86efac",
  expense: "#dc2626",
  openPayables: "#f472b6",
  periodBalance: "#eab308",
  accumulatedBalance: "#4338ca",
};

type CashFlowChartProps = {
  data: CashFlowPoint[];
  height?: number;
  tickFormatter?: (value: string) => string;
  valueFormatter?: (value: number) => string;
  labelFormatter?: (value: number) => string;
};

const BARS: { key: keyof CashFlowPoint; color: string }[] = [
  { key: "income", color: CASH_FLOW_COLORS.income },
  { key: "openReceivables", color: CASH_FLOW_COLORS.openReceivables },
  { key: "expense", color: CASH_FLOW_COLORS.expense },
  { key: "openPayables", color: CASH_FLOW_COLORS.openPayables },
];

const LINES: { key: keyof CashFlowPoint; color: string }[] = [
  { key: "periodBalance", color: CASH_FLOW_COLORS.periodBalance },
  { key: "accumulatedBalance", color: CASH_FLOW_COLORS.accumulatedBalance },
];

export function CashFlowChart({
  data,
  height = 320,
  tickFormatter,
  valueFormatter,
  labelFormatter,
}: CashFlowChartProps) {
  return (
    <div className="w-full" style={{ height }}>
      <ResponsiveContainer width="100%" height="100%">
        <ComposedChart data={data} margin={{ top: 20, right: 16, left: 0, bottom: 0 }}>
          <CartesianGrid vertical={false} strokeDasharray="3 3" stroke="hsl(var(--border))" />
          <XAxis
            dataKey="period"
            tickLine={false}
            axisLine={false}
            tick={{ fontSize: 11, fill: "hsl(var(--muted-foreground))" }}
            tickFormatter={tickFormatter}
          />
          <YAxis
            tickLine={false}
            axisLine={false}
            width={48}
            tick={{ fontSize: 11, fill: "hsl(var(--muted-foreground))" }}
            tickFormatter={valueFormatter}
          />
          <Tooltip
            formatter={valueFormatter ? (value) => valueFormatter(Number(value)) : undefined}
          />
          {BARS.map((item) => (
            <Bar key={item.key} dataKey={item.key} fill={item.color} radius={[3, 3, 0, 0]}>
              {labelFormatter ? (
                <LabelList
                  dataKey={item.key}
                  position="top"
                  formatter={(value: number | string) => labelFormatter(Number(value))}
                  style={{ fontSize: 9, fill: "hsl(var(--muted-foreground))" }}
                />
              ) : null}
            </Bar>
          ))}
          {LINES.map((item) => (
            <Line
              key={item.key}
              type="monotone"
              dataKey={item.key}
              stroke={item.color}
              strokeWidth={2}
              dot={{ r: 2 }}
            >
              {labelFormatter ? (
                <LabelList
                  dataKey={item.key}
                  position="top"
                  formatter={(value: number | string) => labelFormatter(Number(value))}
                  style={{ fontSize: 10, fill: "hsl(var(--muted-foreground))" }}
                />
              ) : null}
            </Line>
          ))}
        </ComposedChart>
      </ResponsiveContainer>
    </div>
  );
}
