import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import HomePage from "@/app/(dashboard)/crm/page";

const { modulesState } = vi.hoisted(() => ({ modulesState: { value: [] as string[] } }));

vi.mock("@/features/identity/auth/hooks/useAuth", () => ({
  useAuth: () => ({ user: { id: "u1", companyId: "co1", name: "Raquel Aguiar" } }),
}));

vi.mock("@/features/identity/auth/hooks/useAuthorization", () => ({
  useAuthorization: () => ({ can: () => true }),
}));

vi.mock("@/features/identity/modules/hooks/useCompanyModules", () => ({
  useActiveCompanyModules: () => ({
    isEnabled: (m: string) => modulesState.value.includes(m),
  }),
}));

vi.mock("@/features/analytics/dashboard/hooks/useOperationalDashboard", () => ({
  useOperationalDashboard: () => ({
    data: {
      tasksDueToday: 1,
      dueToday: [{ id: "t1", title: "Ligar para fornecedor", dueAt: null }],
    },
    isLoading: false,
  }),
}));

vi.mock("@/features/sales/scheduling/hooks/useScheduling", () => ({
  useAppointments: () => ({
    data: [
      {
        id: "a1",
        title: "Consulta Ana",
        startAt: "2026-10-30T13:00:00Z",
        status: "CONFIRMED",
      },
    ],
    isLoading: false,
  }),
}));

vi.mock("@/features/finance/hooks/useFinance", () => ({
  useHeadline: () => ({ data: { revenueThisMonthCents: 150000 }, isLoading: false }),
}));

vi.mock("@/features/analytics/crm/components/CrmOverview", () => ({
  CrmOverview: () => <div>Painel comercial</div>,
}));

describe("HomePage", () => {
  beforeEach(() => {
    modulesState.value = [];
  });

  it("sem o módulo Comercial não mostra os cards de vendas", () => {
    modulesState.value = ["AGENDA", "PATIENTS", "TASKS", "FINANCE"];

    render(<HomePage />);

    expect(screen.getByText(/Boa (tarde|noite)|Bom dia/)).toBeTruthy();
    expect(screen.getByText("Agenda de hoje")).toBeTruthy();
    expect(screen.getByText("Consulta Ana")).toBeTruthy();
    expect(screen.getByText("Ligar para fornecedor")).toBeTruthy();
    expect(screen.getByText("Receita do mês")).toBeTruthy();
    expect(screen.getByText("Pacientes")).toBeTruthy();
    expect(screen.queryByText("Painel comercial")).toBeNull();
    expect(screen.queryByText("Pipeline")).toBeNull();
  });

  it("com o módulo Comercial mostra o painel de vendas", () => {
    modulesState.value = ["CRM"];

    render(<HomePage />);

    expect(screen.getByText("Painel comercial")).toBeTruthy();
    expect(screen.queryByText("Agenda de hoje")).toBeNull();
    expect(screen.queryByText("Receita do mês")).toBeNull();
  });
});
