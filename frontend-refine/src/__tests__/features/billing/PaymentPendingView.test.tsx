import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { PaymentPendingView } from "@/features/billing/components/PaymentPendingView";

const state = vi.hoisted(() => ({
  billing: null as null | Record<string, unknown>,
  mutate: vi.fn(),
  refetch: vi.fn(),
  error: false,
  logout: vi.fn(),
  companies: [] as { companyId: string; name: string }[],
  switchMutate: vi.fn(),
}));

vi.mock("@/features/identity/auth/hooks/useAuth", () => ({
  useAuth: () => ({ user: { companyId: "c1" }, logout: state.logout }),
}));

vi.mock("@/features/identity/auth/hooks/useAuthMutations", () => ({
  useMyCompanies: () => ({ data: state.companies }),
  useSwitchCompany: () => ({ mutate: state.switchMutate, isPending: false }),
}));

vi.mock("@/features/billing/hooks", () => ({
  useMyBilling: () => ({
    data: state.error ? undefined : state.billing,
    isLoading: false,
    isError: state.error,
    refetch: state.refetch,
  }),
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
    state.refetch.mockReset();
    state.error = false;
    state.logout.mockReset();
    state.switchMutate.mockReset();
    state.companies = [];
  });

  it("Sair chama o logout", () => {
    render(<PaymentPendingView />);
    fireEvent.click(screen.getByRole("button", { name: "Sair" }));
    expect(state.logout).toHaveBeenCalled();
  });

  it("Sair também aparece no estado de erro", () => {
    state.error = true;
    render(<PaymentPendingView />);
    expect(screen.getByRole("button", { name: "Sair" })).toBeTruthy();
  });

  it("com mais de uma empresa permite trocar", () => {
    state.companies = [
      { companyId: "c1", name: "Clínica A" },
      { companyId: "c2", name: "Clínica B" },
    ];
    render(<PaymentPendingView />);
    fireEvent.click(screen.getByRole("button", { name: "Clínica B" }));
    expect(state.switchMutate.mock.calls[0][0]).toBe("c2");
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

  it("erro ao carregar permite tentar de novo", () => {
    state.error = true;
    render(<PaymentPendingView />);
    expect(screen.getByText("Não foi possível carregar os dados de cobrança.")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Tentar de novo" }));
    expect(state.refetch).toHaveBeenCalled();
  });

  it("falha ao verificar pagamento mostra aviso", () => {
    state.mutate.mockImplementation((_v: unknown, opts: { onError: () => void }) => opts.onError());
    render(<PaymentPendingView />);
    fireEvent.click(screen.getByRole("button", { name: /Já paguei/ }));
    expect(
      screen.getByText("Não foi possível verificar agora. Tente de novo em instantes."),
    ).toBeTruthy();
  });

  it("empresa liberada vê o caminho de volta", () => {
    state.billing = { ...blocked, status: "ACTIVE" };
    render(<PaymentPendingView />);
    expect(screen.getByText("Sua empresa está liberada.")).toBeTruthy();
  });

  describe("volta direto ao sistema (/crm), sem passar pela apresentação", () => {
    const assign = vi.fn();
    const original = window.location;

    beforeEach(() => {
      assign.mockReset();
      Object.defineProperty(window, "location", {
        configurable: true,
        value: { ...original, assign },
      });
    });

    afterEach(() => {
      Object.defineProperty(window, "location", { configurable: true, value: original });
    });

    it("Já paguei com pagamento confirmado", () => {
      state.mutate.mockImplementation(
        (_v: unknown, opts: { onSuccess: (v: { status: string }) => void }) =>
          opts.onSuccess({ status: "ACTIVE" }),
      );
      render(<PaymentPendingView />);
      fireEvent.click(screen.getByRole("button", { name: /Já paguei/ }));
      expect(assign).toHaveBeenCalledWith("/crm");
    });

    it("Já paguei sem pagamento continua na tela", () => {
      state.mutate.mockImplementation(
        (_v: unknown, opts: { onSuccess: (v: { status: string }) => void }) =>
          opts.onSuccess({ status: "BLOCKED" }),
      );
      render(<PaymentPendingView />);
      fireEvent.click(screen.getByRole("button", { name: /Já paguei/ }));
      expect(assign).not.toHaveBeenCalled();
      expect(screen.getByText(/Ainda não identificamos o pagamento/)).toBeTruthy();
    });

    it("Voltar ao sistema", () => {
      state.billing = { ...blocked, status: "ACTIVE" };
      render(<PaymentPendingView />);
      fireEvent.click(screen.getByRole("button", { name: "Voltar ao sistema" }));
      expect(assign).toHaveBeenCalledWith("/crm");
    });
  });
});
