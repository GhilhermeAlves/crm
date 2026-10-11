import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";

import { ReportsHeadlineCards } from "@/features/reports/components/ReportsHeadlineCards";

describe("ReportsHeadlineCards", () => {
  it("formata receita em BRL e conta consultas e orçamentos", () => {
    render(
      <ReportsHeadlineCards
        revenueThisMonthCents={123456}
        attendancesThisWeek={3}
        budgetsCreatedThisMonth={2}
      />,
    );

    expect(screen.getByText("Receita gerada")).toBeTruthy();
    expect(screen.getByText("R$ 1.234,56")).toBeTruthy();
    expect(screen.getByText("3 consultas")).toBeTruthy();
    expect(screen.getByText("2 criados")).toBeTruthy();
    expect(screen.getByText("Dados desta semana")).toBeTruthy();
  });

  it("mostra esqueletos durante o carregamento", () => {
    render(
      <ReportsHeadlineCards
        revenueThisMonthCents={0}
        attendancesThisWeek={0}
        budgetsCreatedThisMonth={0}
        isLoading
      />,
    );

    expect(screen.queryByText("R$ 0,00")).toBeNull();
  });
});
