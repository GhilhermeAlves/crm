import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";

import { CashFlowReport } from "@/features/finance/components/reports/CashFlowReport";

vi.mock("@/features/finance/hooks/useFinance", () => ({
  useCashFlow: () => ({
    data: {
      series: [],
      incomeByMethod: {
        boletoCents: 900000,
        creditCardCents: 0,
        debitCardCents: 0,
        pixCents: 40000,
        cashCents: 0,
        transferCents: 0,
        checkCents: 0,
        otherCents: 0,
      },
      incomeHistory: [],
      expenseHistory: [],
      currentMonth: {
        income: { paidCents: 0, openCents: 0, defaultCents: 0 },
        expense: { paidCents: 0, openCents: 0, defaultCents: 0 },
      },
    },
    isLoading: false,
    isError: false,
    refetch: () => {},
  }),
}));

if (typeof globalThis.ResizeObserver === "undefined") {
  globalThis.ResizeObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  } as unknown as typeof ResizeObserver;
}

describe("CashFlowReport", () => {
  it("mostra sempre as 8 formas de pagamento do detalhamento de entradas", () => {
    render(<CashFlowReport companyId="c1" />);

    for (const label of [
      "Boleto",
      "Cartão de crédito",
      "Cartão de débito",
      "Pix",
      "Dinheiro",
      "Transferência",
      "Cheque",
      "Outro",
    ]) {
      expect(screen.getByText(label)).toBeTruthy();
    }

    expect(screen.getByText("R$9k")).toBeTruthy();
  });

  it("mostra 'Sem resultados' nos gráficos vazios", () => {
    render(<CashFlowReport companyId="c1" />);

    expect(screen.getAllByText("Sem resultados").length).toBeGreaterThanOrEqual(1);
  });
});
