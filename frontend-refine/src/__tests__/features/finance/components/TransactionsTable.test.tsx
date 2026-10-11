import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { TransactionsTable } from "@/features/finance/components/TransactionsTable";
import type { FinancialTransaction } from "@/features/finance/types/finance.types";

function makeTransaction(overrides: Partial<FinancialTransaction>): FinancialTransaction {
  return {
    id: "tx-1",
    companyId: "company-1",
    type: "INCOME",
    description: "Procedimento",
    category: "PROCEDURE",
    amountCents: 100000,
    feeCents: 0,
    paymentMethod: "PIX",
    dueDate: "2026-05-10",
    paidAt: null,
    status: "PAID",
    contactId: null,
    contactName: null,
    professionalUserId: null,
    professionalName: null,
    catalogItemId: null,
    catalogItemName: null,
    installmentGroupId: null,
    installmentNumber: null,
    installmentTotal: null,
    notes: null,
    createdBy: null,
    createdAt: "2026-05-01T10:00:00Z",
    updatedAt: "2026-05-01T10:00:00Z",
    overdue: false,
    ...overrides,
  };
}

const baseProps = {
  page: 1,
  totalPages: 1,
  totalElements: 1,
  pageSize: 20,
  onPageChange: () => {},
  onNew: () => {},
  onImport: () => {},
};

describe("TransactionsTable", () => {
  it("renders the empty state when there are no transactions", () => {
    render(<TransactionsTable transactions={[]} {...baseProps} />);

    expect(screen.getByText("Nenhum lançamento financeiro encontrado")).toBeTruthy();
  });

  it("renders an income row with a positive value and paid badge", () => {
    render(
      <TransactionsTable
        transactions={[
          makeTransaction({ description: "Limpeza dental", amountCents: 120000, status: "PAID" }),
        ]}
        {...baseProps}
      />,
    );

    expect(screen.getByText("Limpeza dental")).toBeTruthy();
    expect(screen.getByText("Pago")).toBeTruthy();
    expect(screen.getByText(/\+R\$/)).toBeTruthy();
    expect(screen.getByText(/1\.200,00/)).toBeTruthy();
  });

  it("renders an expense row with a negative value", () => {
    render(
      <TransactionsTable
        transactions={[
          makeTransaction({
            type: "EXPENSE",
            description: "Aluguel do consultório",
            category: "RENT",
            amountCents: 200000,
            status: "PAID",
          }),
        ]}
        {...baseProps}
      />,
    );

    expect(screen.getByText("Aluguel do consultório")).toBeTruthy();
    expect(screen.getByText(/−R\$/)).toBeTruthy();
    expect(screen.getByText(/2\.000,00/)).toBeTruthy();
  });

  it("shows a pending transaction that is overdue as 'Atrasado'", () => {
    render(
      <TransactionsTable
        transactions={[makeTransaction({ status: "PENDING", overdue: true })]}
        {...baseProps}
      />,
    );

    expect(screen.getByText("Atrasado")).toBeTruthy();
  });

  it("renders a cancelled transaction badge", () => {
    render(
      <TransactionsTable
        transactions={[makeTransaction({ status: "CANCELLED" })]}
        {...baseProps}
      />,
    );

    expect(screen.getByText("Cancelado")).toBeTruthy();
  });
});
