"use client";

import {
  Bar,
  BarChart,
  CartesianGrid,
  LabelList,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

export type ReportBarSeries = {
  key: string;
  name: string;
  color: string;
  stackId?: string;
};

type ReportBarChartProps = {
  data: Record<string, string | number>[];
  xKey: string;
  series: ReportBarSeries[];
  height?: number;
  tickFormatter?: (value: string) => string;
  valueFormatter?: (value: number) => string;
  labelFormatter?: (value: number) => string;
  allowDecimals?: boolean;
  barSize?: number;
  xAxisAngle?: number;
};

export function ReportBarChart({
  data,
  xKey,
  series,
  height = 200,
  tickFormatter,
  valueFormatter,
  labelFormatter,
  allowDecimals = false,
  barSize = 18,
  xAxisAngle,
}: ReportBarChartProps) {
  return (
    <div className="w-full" style={{ height }}>
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={data} margin={{ top: 16, right: 8, left: 0, bottom: 0 }}>
          <CartesianGrid vertical={false} strokeDasharray="3 3" stroke="hsl(var(--border))" />
          <XAxis
            dataKey={xKey}
            tickLine={false}
            axisLine={false}
            height={xAxisAngle ? 72 : undefined}
            tick={{ fontSize: 11, fill: "hsl(var(--muted-foreground))" }}
            tickFormatter={tickFormatter}
            angle={xAxisAngle}
            textAnchor={xAxisAngle ? "end" : "middle"}
            interval={0}
          />
          <YAxis
            allowDecimals={allowDecimals}
            tickLine={false}
            axisLine={false}
            width={44}
            tick={{ fontSize: 11, fill: "hsl(var(--muted-foreground))" }}
            tickFormatter={valueFormatter}
          />
          <Tooltip
            cursor={{ fill: "hsl(var(--muted) / 0.4)" }}
            formatter={valueFormatter ? (value) => valueFormatter(Number(value)) : undefined}
          />
          {series.map((item) => (
            <Bar
              key={item.key}
              dataKey={item.key}
              name={item.name}
              fill={item.color}
              stackId={item.stackId}
              radius={[3, 3, 0, 0]}
              barSize={item.stackId ? undefined : barSize}
            >
              {labelFormatter ? (
                <LabelList
                  dataKey={item.key}
                  position="top"
                  formatter={(value: number | string) => labelFormatter(Number(value))}
                  style={{ fontSize: 10, fill: "hsl(var(--muted-foreground))" }}
                />
              ) : null}
            </Bar>
          ))}
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
