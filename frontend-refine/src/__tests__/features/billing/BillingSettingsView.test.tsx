import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import { BillingSettingsView } from "@/features/billing/components/BillingSettingsView";

const state = vi.hoisted(() => ({
  billing: null as null | Record<string, unknown>,
  invoices: [] as Record<string, unknown>[],
  billingError: false,
  invoicesError: false,
}));

vi.mock("@/features/billing/hooks", () => ({
  useMyBilling: () => ({
    data: state.billingError ? undefined : state.billing,
    isLoading: false,
    isError: state.billingError,
  }),
  useMyInvoices: () => ({
    data: state.invoicesError ? undefined : state.invoices,
    isLoading: false,
    isError: state.invoicesError,
  }),
}));

const active = {
  status: "ACTIVE",
  plan: "STARTER",
  monthlyPriceCents: 19700,
  nextDueDate: "2026-12-15",
  syncError: null,
  hasSubscription: true,
  overdueSince: null,
  graceUntil: null,
  openInvoiceUrl: null,
};

describe("BillingSettingsView", () => {
  beforeEach(() => {
    state.billing = active;
    state.billingError = false;
    state.invoicesError = false;
    state.invoices = [
      {
        id: "i1",
        dueDate: "2026-11-15",
        valueCents: 19700,
        status: "RECEIVED",
        invoiceUrl: "https://pix/1",
        paidAt: "2026-11-14T10:00:00",
      },
      {
        id: "i2",
        dueDate: "2026-12-15",
        valueCents: 19700,
        status: "PENDING",
        invoiceUrl: "https://pix/2",
        paidAt: null,
      },
    ];
  });

  it("mostra plano, valor, situação e vencimento", () => {
    render(<BillingSettingsView />);
    expect(screen.getByText("Starter")).toBeTruthy();
    expect(screen.getAllByText(/197,00/).length).toBeGreaterThan(0);
    expect(screen.getByText("Em dia")).toBeTruthy();
    expect(screen.getAllByText("15/12/2026").length).toBeGreaterThan(0);
  });

  it("histórico com link só na fatura aberta", () => {
    render(<BillingSettingsView />);
    expect(screen.getByText("Paga")).toBeTruthy();
    const links = screen.getAllByRole("link", { name: "Abrir" });
    expect(links).toHaveLength(1);
    expect(links[0].getAttribute("href")).toBe("https://pix/2");
  });

  it("erro ao carregar cobrança", () => {
    state.billingError = true;
    render(<BillingSettingsView />);
    expect(screen.getByText("Não foi possível carregar os dados de cobrança.")).toBeTruthy();
  });

  it("erro ao carregar faturas", () => {
    state.invoicesError = true;
    render(<BillingSettingsView />);
    expect(screen.getByText("Não foi possível carregar as faturas.")).toBeTruthy();
    expect(screen.queryByText("Nenhuma fatura ainda.")).toBeNull();
  });

  it("sem cobrança ativa", () => {
    state.billing = { ...active, status: "NOT_STARTED", hasSubscription: false };
    render(<BillingSettingsView />);
    expect(
      screen.getByText("A cobrança automática não está ativa para esta empresa."),
    ).toBeTruthy();
  });
});
