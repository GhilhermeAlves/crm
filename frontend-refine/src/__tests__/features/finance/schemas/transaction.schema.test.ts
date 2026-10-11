import { describe, expect, it } from "vitest";
import { addDays, format } from "date-fns";
import { transactionFormSchema } from "@/features/finance/schemas/transaction.schema";

const validValues = {
  type: "INCOME" as const,
  description: "Consulta de avaliação",
  category: "CONSULTATION",
  amount: "1.000,00",
  paymentMethod: "PIX",
  dueDate: "2026-01-10",
  installments: 1,
  fee: "0",
  paid: false,
};

const issuesFor = (values: Record<string, unknown>) => {
  const result = transactionFormSchema.safeParse({ ...validValues, ...values });
  if (result.success) return [];
  return result.error.issues.map((issue) => issue.path[0]);
};

describe("transactionFormSchema", () => {
  it("accepts a valid income transaction", () => {
    expect(transactionFormSchema.safeParse(validValues).success).toBe(true);
  });

  it("rejects a zero amount", () => {
    expect(issuesFor({ amount: "0,00" })).toContain("amount");
  });

  it("rejects an expense category on an income transaction", () => {
    expect(issuesFor({ type: "INCOME", category: "RENT" })).toContain("category");
  });

  it("requires a payment date when marked as paid", () => {
    expect(issuesFor({ paid: true, paidAt: undefined })).toContain("paidAt");
  });

  it("rejects a future payment date", () => {
    const future = format(addDays(new Date(), 3), "yyyy-MM-dd");
    expect(issuesFor({ paid: true, paidAt: future })).toContain("paidAt");
  });

  it("rejects a fee greater than the amount", () => {
    expect(issuesFor({ amount: "100,00", fee: "200,00" })).toContain("fee");
  });
});
