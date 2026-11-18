import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, within } from "@testing-library/react";
import { CreditSimulator } from "@/features/finance/credit/components/CreditSimulator";

const saved = Array.from({ length: 12 }, (_, i) => ({ installments: i + 1, ratePercent: 10 }));

vi.mock("@/features/finance/credit/hooks/useCardFees", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("@/features/finance/credit/hooks/useCardFees")>();
  return {
    ...actual,
    useCardFees: () => ({ data: saved, isLoading: false }),
    useCardFeePresets: () => ({ data: [], isLoading: false }),
  };
});

const row = (n: number) => screen.getByTestId(`credit-row-${n}`);
const brl = (text: string) => text.replace(/ /g, " ");

describe("CreditSimulator", () => {
  it("mostra 12 linhas com as taxas cadastradas", () => {
    render(<CreditSimulator amountCents={100000} companyId="co" />);
    expect(screen.getAllByTestId(/^credit-row-/)).toHaveLength(12);
    expect(brl(row(1).textContent ?? "")).toContain("R$ 900,00");
  });

  it("repassar juros aumenta o total do paciente e a clínica recebe o valor cheio", () => {
    render(<CreditSimulator amountCents={100000} companyId="co" />);
    fireEvent.click(screen.getByRole("switch"));
    const text = brl(row(1).textContent ?? "");
    expect(text).toContain("R$ 1.111,11");
    expect(text).toContain("R$ 1.000,00");
  });

  it("editar a taxa muda só a simulação e dá para voltar às cadastradas", () => {
    render(<CreditSimulator amountCents={100000} companyId="co" />);
    const input = within(row(2)).getByLabelText("Taxa de 2x");
    fireEvent.change(input, { target: { value: "5" } });
    expect(brl(row(2).textContent ?? "")).toContain("R$ 950,00");

    fireEvent.click(screen.getByRole("button", { name: "Voltar às taxas cadastradas" }));
    expect((within(row(2)).getByLabelText("Taxa de 2x") as HTMLInputElement).value).toBe("10");
    expect(brl(row(2).textContent ?? "")).toContain("R$ 900,00");
  });
});
