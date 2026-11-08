import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import { PastDueBanner } from "@/features/billing/components/PastDueBanner";

const state = vi.hoisted(() => ({
  billing: null as null | Record<string, unknown>,
  canUpdate: true,
  isCrmAdmin: false,
  enabledArg: undefined as undefined | boolean,
}));

vi.mock("@/features/billing/hooks", () => ({
  useMyBilling: (enabled: boolean) => {
    state.enabledArg = enabled;
    return { data: enabled ? state.billing : undefined };
  },
}));

vi.mock("@/features/identity/auth/hooks/useAuthorization", () => ({
  useAuthorization: () => ({ can: () => state.canUpdate, isCrmAdmin: state.isCrmAdmin }),
}));

const pastDue = {
  status: "PAST_DUE",
  plan: "STARTER",
  monthlyPriceCents: 19700,
  nextDueDate: "2026-12-15",
  syncError: null,
  hasSubscription: true,
  overdueSince: "2026-11-15",
  graceUntil: null,
  openInvoiceUrl: "https://pix/1",
};

describe("PastDueBanner", () => {
  beforeEach(() => {
    state.billing = pastDue;
    state.canUpdate = true;
    state.isCrmAdmin = false;
  });

  it("admin da clínica vê o aviso e o Pix", () => {
    render(<PastDueBanner />);
    expect(screen.getByRole("alert").textContent).toContain("Mensalidade venceu em 15/11.");
    expect(screen.getByRole("link", { name: /Pagar com Pix/ }).getAttribute("href")).toBe(
      "https://pix/1",
    );
  });

  it("sem permissão vê só o aviso", () => {
    state.canUpdate = false;
    render(<PastDueBanner />);
    expect(screen.queryByRole("link", { name: /Pagar com Pix/ })).toBeNull();
    expect(screen.getByText(/Avise o administrador da clínica/)).toBeTruthy();
  });

  it("em dia não mostra nada", () => {
    state.billing = { ...pastDue, status: "ACTIVE" };
    const { container } = render(<PastDueBanner />);
    expect(container.innerHTML).toBe("");
  });

  it("CRM_ADMIN não consulta nem vê", () => {
    state.isCrmAdmin = true;
    const { container } = render(<PastDueBanner />);
    expect(state.enabledArg).toBe(false);
    expect(container.innerHTML).toBe("");
  });

  it("mostra até quando foi liberada", () => {
    state.billing = { ...pastDue, graceUntil: "2026-11-30" };
    render(<PastDueBanner />);
    expect(screen.getByRole("alert").textContent).toContain("Liberada até 30/11.");
  });
});
