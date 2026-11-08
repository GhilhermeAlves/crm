import { describe, expect, it, vi, beforeEach } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { AdminBillingCard } from "@/features/billing/components/AdminBillingCard";

const state = vi.hoisted(() => ({
  billing: null as null | Record<string, unknown>,
  activate: vi.fn(),
  exempt: vi.fn(),
  price: vi.fn(),
  grant: vi.fn(),
}));

vi.mock("@/features/billing/hooks", () => ({
  useAdminBilling: () => ({ data: state.billing, isLoading: false }),
  useAdminInvoices: () => ({ data: [], isLoading: false }),
  useActivateBilling: () => ({ mutate: state.activate, isPending: false }),
  useExemptBilling: () => ({ mutate: state.exempt, isPending: false }),
  useChangeBillingPrice: () => ({ mutate: state.price, isPending: false }),
  useGrantBilling: () => ({ mutate: state.grant, isPending: false }),
}));

const base = {
  status: "NOT_STARTED",
  plan: "STARTER",
  monthlyPriceCents: 19700,
  nextDueDate: null,
  syncError: null,
  hasSubscription: false,
  overdueSince: null,
  graceUntil: null,
  openInvoiceUrl: null,
};

describe("AdminBillingCard", () => {
  beforeEach(() => {
    state.billing = base;
    Object.values(state).forEach(
      (v) => typeof v === "function" && (v as ReturnType<typeof vi.fn>).mockReset(),
    );
  });

  it("ativa a cobrança", () => {
    render(<AdminBillingCard companyId="co1" />);
    fireEvent.click(screen.getByRole("button", { name: "Ativar cobrança" }));
    expect(state.activate).toHaveBeenCalled();
  });

  it("tentar novamente com erro de sincronização", () => {
    state.billing = { ...base, status: "SYNC_PENDING", syncError: "Asaas indisponível." };
    render(<AdminBillingCard companyId="co1" />);
    expect(screen.getByText("Asaas indisponível.")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Tentar novamente" }));
    expect(state.activate).toHaveBeenCalled();
  });

  it("muda o valor em reais e envia centavos", () => {
    state.billing = { ...base, status: "ACTIVE", hasSubscription: true };
    render(<AdminBillingCard companyId="co1" />);
    fireEvent.click(screen.getByRole("button", { name: "Editar" }));
    fireEvent.change(screen.getByLabelText("Valor mensal (R$)"), { target: { value: "170.50" } });
    fireEvent.click(screen.getByRole("button", { name: "Salvar" }));
    expect(state.price).toHaveBeenCalledWith(17050, expect.anything());
  });

  it("libera por X dias quando bloqueada", () => {
    state.billing = {
      ...base,
      status: "BLOCKED",
      hasSubscription: true,
      overdueSince: "2026-11-01",
    };
    render(<AdminBillingCard companyId="co1" />);
    fireEvent.change(screen.getByLabelText("Dias"), { target: { value: "10" } });
    fireEvent.click(screen.getByRole("button", { name: "Liberar" }));
    expect(state.grant).toHaveBeenCalledWith(10);
  });

  it("não oferece isentar com assinatura ativa", () => {
    state.billing = { ...base, status: "ACTIVE", hasSubscription: true };
    render(<AdminBillingCard companyId="co1" />);
    expect(screen.queryByRole("button", { name: "Marcar como isenta" })).toBeNull();
  });
});
