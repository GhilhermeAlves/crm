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

const patient = {
  name: "Maria Silva",
  birthDate: "1989-04-05",
  gender: "Feminino",
  mobile: "(34) 99999-0000",
  cpf: "000.000.000-00",
  email: null,
};

const company = {
  name: "Clínica Sorriso",
  logoUrl: "https://example.com/logo.png",
  addressLine: "Rua A, 45 - Centro - Uberlândia / MG",
  phone: "(34) 3333-0000",
};

describe("BudgetPrintView", () => {
  it("imprime cabeçalho da clínica com logo, dados do paciente e assinatura", () => {
    const { container } = render(
      <BudgetPrintView budget={budget} blocks={ALL_BLOCKS} patient={patient} company={company} />,
    );
    expect(screen.getByText("Clínica Sorriso")).toBeTruthy();
    expect(screen.getByText("Rua A, 45 - Centro - Uberlândia / MG")).toBeTruthy();
    expect(container.querySelector("img")?.getAttribute("src")).toBe(company.logoUrl);
    expect(screen.getByText("05/04/1989")).toBeTruthy();
    expect(screen.getByText(/Declaro para fins de direito/)).toBeTruthy();
    expect(screen.getAllByText("Maria Silva").length).toBeGreaterThanOrEqual(2);
  });

  it("com todos os blocos mostra valor, dentista, total e observações", () => {
    render(<BudgetPrintView budget={budget} blocks={ALL_BLOCKS} patient={patient} />);
    expect(screen.getByText("Tratamento completo")).toBeTruthy();
    expect(screen.getByText("Valor")).toBeTruthy();
    expect(screen.getByText("Dentista")).toBeTruthy();
    expect(screen.getByText("Valor Total:")).toBeTruthy();
    expect(screen.getByText("Retorno em 30 dias")).toBeTruthy();
  });

  it("sem os blocos de valores oculta a coluna de valor e o total", () => {
    render(<BudgetPrintView budget={budget} blocks={[]} patient={patient} />);
    expect(screen.getByText("Restauração")).toBeTruthy();
    expect(screen.queryByText("Valor")).toBeNull();
    expect(screen.queryByText("Dentista")).toBeNull();
    expect(screen.queryByText("Valor Total:")).toBeNull();
    expect(screen.queryByText("Desconto")).toBeNull();
    expect(screen.queryByText("Retorno em 30 dias")).toBeNull();
  });

  it("fora da odontologia não imprime a coluna de dentes", () => {
    render(
      <BudgetPrintView budget={budget} blocks={ALL_BLOCKS} patient={patient} showTeeth={false} />,
    );
    expect(screen.queryByText("Dente")).toBeNull();
    expect(screen.getByText("Profissional")).toBeTruthy();
  });
});
