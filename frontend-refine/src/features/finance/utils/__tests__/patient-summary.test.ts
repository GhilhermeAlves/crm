import { describe, it, expect } from "vitest";

import { summarizePatientTransactions } from "../patient-summary";
import type { FinancialTransaction } from "../../types/finance.types";

const today = new Date("2026-10-13T12:00:00");

function makeTransaction(overrides: Partial<FinancialTransaction>): FinancialTransaction {
  return {
    id: "t1",
    companyId: "co1",
    type: "INCOME",
    description: "Consulta",
    category: "CONSULTATION",
    amountCents: 10_000,
    feeCents: 0,
    paymentMethod: "PIX",
    dueDate: "2026-10-13",
    paidAt: null,
    status: "PENDING",
    contactId: "c1",
    contactName: "Ana",
    professionalUserId: null,
    professionalName: null,
    catalogItemId: null,
    catalogItemName: null,
    installmentGroupId: null,
    installmentNumber: null,
    installmentTotal: null,
    notes: null,
    createdBy: null,
    createdAt: "",
    updatedAt: "",
    overdue: false,
    ...overrides,
  };
}

describe("summarizePatientTransactions", () => {
  it("distingue atrasado de pendente na virada do dia", () => {
    const summary = summarizePatientTransactions(
      [
        makeTransaction({ id: "ontem", amountCents: 5_000, dueDate: "2026-10-12" }),
        makeTransaction({ id: "hoje", amountCents: 7_000, dueDate: "2026-10-13" }),
        makeTransaction({ id: "amanha", amountCents: 3_000, dueDate: "2026-10-14" }),
      ],
      today,
    );

    expect(summary.overdueCents).toBe(5_000);
    expect(summary.pendingCents).toBe(10_000);
    expect(summary.totalCents).toBe(15_000);
  });

  it("conta pagos no total e no pago, fora de pendente/atrasado", () => {
    const summary = summarizePatientTransactions(
      [
        makeTransaction({ id: "pago", amountCents: 20_000, status: "PAID" }),
        makeTransaction({ id: "pendente", amountCents: 4_000, dueDate: "2026-10-20" }),
      ],
      today,
    );

    expect(summary.paidCents).toBe(20_000);
    expect(summary.totalCents).toBe(24_000);
    expect(summary.pendingCents).toBe(4_000);
    expect(summary.overdueCents).toBe(0);
  });

  it("ignora lançamentos cancelados", () => {
    const summary = summarizePatientTransactions(
      [
        makeTransaction({ id: "cancelado", amountCents: 9_000, status: "CANCELLED" }),
        makeTransaction({ id: "pendente", amountCents: 1_000 }),
      ],
      today,
    );

    expect(summary.totalCents).toBe(1_000);
    expect(summary.pendingCents).toBe(1_000);
  });
});
