import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { FinanceKpiCards } from "@/features/finance/components/FinanceKpiCards";
import type { SummaryResponse } from "@/features/finance/types/finance.types";

const SUMMARY: SummaryResponse = {
  incomeTotalCents: 150000,
  expenseTotalCents: 90000,
  incomeForecastCents: 50000,
  expenseForecastCents: 30000,
  balanceCents: 60000,
};

describe("FinanceKpiCards", () => {
  it("shows three KPIs on the 'all' tab", () => {
    render(<FinanceKpiCards tab="all" summary={SUMMARY} />);

    expect(screen.getByText("Receitas")).toBeTruthy();
    expect(screen.getByText("Despesas")).toBeTruthy();
    expect(screen.getByText("Saldo")).toBeTruthy();
    expect(screen.getByText(/1\.500,00/)).toBeTruthy();
  });

  it("shows income totals and forecast on the 'income' tab", () => {
    render(<FinanceKpiCards tab="income" summary={SUMMARY} />);

    expect(screen.getByText("Entradas totais")).toBeTruthy();
    expect(screen.getByText("Total previsto")).toBeTruthy();
    expect(screen.queryByText("Receitas")).toBeNull();
  });

  it("shows expense totals and forecast on the 'expense' tab", () => {
    render(<FinanceKpiCards tab="expense" summary={SUMMARY} />);

    expect(screen.getByText("Saídas totais")).toBeTruthy();
    expect(screen.getByText("Total previsto")).toBeTruthy();
  });

  it("shows 'Sem registros' when the value is zero", () => {
    render(
      <FinanceKpiCards
        tab="income"
        summary={{ ...SUMMARY, incomeTotalCents: 0, incomeForecastCents: 0 }}
      />,
    );

    expect(screen.getAllByText("Sem registros").length).toBe(2);
  });
});
