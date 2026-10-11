"use client";

import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from "recharts";

export type ReportPieSlice = {
  name: string;
  value: number;
  color: string;
};

type ReportPieChartProps = {
  data: ReportPieSlice[];
  height?: number;
  valueFormatter?: (value: number) => string;
};

export function ReportPieChart({ data, height = 240, valueFormatter }: ReportPieChartProps) {
  return (
    <div className="w-full" style={{ height }}>
      <ResponsiveContainer width="100%" height="100%">
        <PieChart>
          <Pie
            data={data}
            dataKey="value"
            nameKey="name"
            innerRadius="55%"
            outerRadius="80%"
            paddingAngle={2}
            stroke="none"
          >
            {data.map((slice) => (
              <Cell key={slice.name} fill={slice.color} />
            ))}
          </Pie>
          <Tooltip
            formatter={valueFormatter ? (value) => valueFormatter(Number(value)) : undefined}
          />
        </PieChart>
      </ResponsiveContainer>
    </div>
  );
}
