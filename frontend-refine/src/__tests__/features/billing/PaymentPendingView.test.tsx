import { describe, expect, it, vi, beforeEach } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { PaymentPendingView } from "@/features/billing/components/PaymentPendingView";

const state = vi.hoisted(() => ({
  billing: null as null | Record<string, unknown>,
  mutate: vi.fn(),
}));

vi.mock("@/features/billing/hooks", () => ({
  useMyBilling: () => ({ data: state.billing, isLoading: false }),
  useCheckPayment: () => ({ mutate: state.mutate, isPending: false }),
}));

const blocked = {
  status: "BLOCKED",
  plan: "STARTER",
  monthlyPriceCents: 19700,
  nextDueDate: "2026-12-15",
  syncError: null,
  hasSubscription: true,
  overdueSince: "2026-11-15",
  graceUntil: null,
  openInvoiceUrl: "https://pix/1",
};

describe("PaymentPendingView", () => {
  beforeEach(() => {
    state.billing = blocked;
    state.mutate.mockReset();
  });

  it("mostra valor, atraso e o Pix", () => {
    render(<PaymentPendingView />);
    expect(screen.getByText("Pagamento pendente")).toBeTruthy();
    expect(screen.getByText(/197,00/)).toBeTruthy();
    expect(screen.getByText(/15\/11\/2026/)).toBeTruthy();
    expect(screen.getByRole("link", { name: /Pagar com Pix/ }).getAttribute("href")).toBe(
      "https://pix/1",
    );
  });

  it("Já paguei consulta o pagamento", () => {
    render(<PaymentPendingView />);
    fireEvent.click(screen.getByRole("button", { name: /Já paguei/ }));
    expect(state.mutate).toHaveBeenCalled();
  });

  it("empresa liberada vê o caminho de volta", () => {
    state.billing = { ...blocked, status: "ACTIVE" };
    render(<PaymentPendingView />);
    expect(screen.getByText("Sua empresa está liberada.")).toBeTruthy();
  });
});
