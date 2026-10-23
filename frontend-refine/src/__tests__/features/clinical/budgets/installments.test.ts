import { describe, expect, it } from "vitest";
import { splitInstallments } from "@/features/clinical/budgets/utils/installments";

describe("splitInstallments", () => {
  it("divide igual com o resto na primeira parcela e vencimentos mensais", () => {
    expect(splitInstallments(10000, 3, "2026-11-10")).toEqual([
      { dueDate: "2026-11-10", amountCents: 3334 },
      { dueDate: "2026-12-10", amountCents: 3333 },
      { dueDate: "2027-01-10", amountCents: 3333 },
    ]);
  });

  it("uma parcela leva o total", () => {
    expect(splitInstallments(5000, 1, "2026-11-10")).toEqual([
      { dueDate: "2026-11-10", amountCents: 5000 },
    ]);
  });
});
