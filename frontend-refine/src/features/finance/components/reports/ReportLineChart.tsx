"use client";

import {
  CartesianGrid,
  LabelList,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

export type ReportLineSeries = {
  key: string;
  name: string;
  color: string;
};

type ReportLineChartProps = {
  data: Record<string, string | number>[];
  xKey: string;
  series: ReportLineSeries[];
  height?: number;
  tickFormatter?: (value: string) => string;
  valueFormatter?: (value: number) => string;
  labelFormatter?: (value: number) => string;
  allowDecimals?: boolean;
};

export function ReportLineChart({
  data,
  xKey,
  series,
  height = 220,
  tickFormatter,
  valueFormatter,
  labelFormatter,
  allowDecimals = false,
}: ReportLineChartProps) {
  return (
    <div className="w-full" style={{ height }}>
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={data} margin={{ top: 16, right: 16, left: 0, bottom: 0 }}>
          <CartesianGrid vertical={false} strokeDasharray="3 3" stroke="hsl(var(--border))" />
          <XAxis
            dataKey={xKey}
            tickLine={false}
            axisLine={false}
            tick={{ fontSize: 11, fill: "hsl(var(--muted-foreground))" }}
            tickFormatter={tickFormatter}
          />
          <YAxis
            allowDecimals={allowDecimals}
            tickLine={false}
            axisLine={false}
            width={48}
            tick={{ fontSize: 11, fill: "hsl(var(--muted-foreground))" }}
            tickFormatter={valueFormatter}
          />
          <Tooltip
            formatter={valueFormatter ? (value) => valueFormatter(Number(value)) : undefined}
          />
          {series.map((item) => (
            <Line
              key={item.key}
              type="monotone"
              dataKey={item.key}
              name={item.name}
              stroke={item.color}
              strokeWidth={2}
              dot={{ r: 2 }}
              connectNulls
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
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
