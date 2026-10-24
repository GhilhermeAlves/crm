import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { BudgetPrintView } from "@/features/clinical/budgets/components/BudgetPrintView";
import type { BudgetDetail } from "@/features/clinical/budgets/types/budget.types";
import { ALL_BLOCKS } from "@/features/clinical/budgets/utils/blocks";

const budget: BudgetDetail = {
  id: "b1",
  contactId: "c1",
  title: "Tratamento completo",
  status: "DRAFT",
  subtotalCents: 30000,
  discountCents: 5000,
  totalCents: 25000,
  notes: "Retorno em 30 dias",
  paymentMethod: null,
  installments: null,
  firstDueDate: null,
  approvedAt: null,
  lostAt: null,
  items: [
    {
      procedureId: "p1",
      name: "Restauração",
      teeth: [{ tooth: 11, faces: ["M"] }],
      professionalName: "Dr. Bruno",
      priceCents: 30000,
    },
  ],
  installmentsList: [],
  updatedAt: "2026-10-20T10:00:00",
  updatedByName: "Dra. Ana",
};

describe("BudgetPrintView", () => {
  it("com todos os blocos mostra valor, dentista, total e observações", () => {
    render(<BudgetPrintView budget={budget} blocks={ALL_BLOCKS} patientName="Maria Silva" />);
    expect(screen.getByText("Tratamento completo")).toBeTruthy();
    expect(screen.getByText("Paciente: Maria Silva")).toBeTruthy();
    expect(screen.getByText("Valor")).toBeTruthy();
    expect(screen.getByText("Dentista")).toBeTruthy();
    expect(screen.getByText("Total")).toBeTruthy();
    expect(screen.getByText("Retorno em 30 dias")).toBeTruthy();
  });

  it("sem os blocos de valores oculta a coluna de valor e o total", () => {
    render(<BudgetPrintView budget={budget} blocks={[]} patientName="Maria Silva" />);
    expect(screen.getByText("Restauração")).toBeTruthy();
    expect(screen.queryByText("Valor")).toBeNull();
    expect(screen.queryByText("Dentista")).toBeNull();
    expect(screen.queryByText("Total")).toBeNull();
    expect(screen.queryByText("Desconto")).toBeNull();
    expect(screen.queryByText("Retorno em 30 dias")).toBeNull();
  });

  it("fora da odontologia não imprime a coluna de dentes", () => {
    render(
      <BudgetPrintView
        budget={budget}
        blocks={ALL_BLOCKS}
        patientName="Maria Silva"
        showTeeth={false}
      />,
    );
    expect(screen.queryByText("Dentes")).toBeNull();
    expect(screen.getByText("Profissional")).toBeTruthy();
  });
});
