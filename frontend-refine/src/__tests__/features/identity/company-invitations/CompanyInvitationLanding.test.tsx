import { describe, it, expect, vi, beforeEach } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { CompanyInvitationLanding } from "@/features/identity/company-invitations/components/CompanyInvitationLanding";

const { preview, register, login } = vi.hoisted(() => ({
  preview: vi.fn(),
  register: vi.fn(),
  login: vi.fn(),
}));

vi.mock("@/features/identity/company-invitations/service", () => ({
  CompanyInvitationService: { preview, register, accept: vi.fn() },
}));
vi.mock("@/lib/gateway-auth", () => ({ loginWithGateway: login, logoutWithGateway: vi.fn() }));
vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }) }));
vi.mock("@/features/identity/auth/hooks/useAuthMutations", () => ({
  useMe: () => ({ isLoading: false, data: null }),
}));

const PENDING = {
  email: "dono@clinica.com",
  inviteeName: "Dono",
  segment: "DENTAL",
  enabledModules: ["CRM", "AGENDA"],
  plan: "STARTER",
  maxUsers: 3,
  status: "PENDING",
  expiresAt: "2099-01-01T00:00:00",
  hasAccount: false,
};

function renderLanding() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <CompanyInvitationLanding token="tok" />
    </QueryClientProvider>,
  );
}

const fill = (label: string, value: string) =>
  fireEvent.change(screen.getByLabelText(label), { target: { value } });

describe("CompanyInvitationLanding", () => {
  beforeEach(() => {
    preview.mockReset();
    register.mockReset();
    login.mockReset();
  });

  it("cria a conta e a empresa de quem ainda não tem conta", async () => {
    preview.mockResolvedValue(PENDING);
    register.mockResolvedValue({});
    renderLanding();

    expect(await screen.findByText("Convite para criar sua empresa")).toBeTruthy();
    fill("Senha", "Senha@1234");
    fill("Confirmar senha", "Senha@1234");
    fill("Razão social", "Clínica Sorriso Ltda");
    fill("Nome fantasia", "Clínica Sorriso");
    fill("CNPJ ou CPF (opcional)", "12345678000199");
    fill("Telefone", "11999990000");
    fill("CEP", "01001000");
    fill("Rua", "Rua A");
    fill("Número", "10");
    fill("Bairro", "Centro");
    fill("Cidade", "São Paulo");
    fill("Estado (UF)", "SP");
    fireEvent.click(screen.getByRole("button", { name: "Criar conta e empresa" }));

    await waitFor(() => expect(register).toHaveBeenCalled());
    const body = register.mock.calls[0][0];
    expect(body.token).toBe("tok");
    expect(body.name).toBe("Dono");
    expect(body.company.email).toBe("dono@clinica.com");
    expect(body.company.tradingName).toBe("Clínica Sorriso");
    await waitFor(() => expect(login).toHaveBeenCalled());
  });

  it("não envia sem os dados obrigatórios da empresa", async () => {
    preview.mockResolvedValue(PENDING);
    renderLanding();

    await screen.findByText("Convite para criar sua empresa");
    fill("Senha", "Senha@1234");
    fill("Confirmar senha", "Senha@1234");
    fireEvent.click(screen.getByRole("button", { name: "Criar conta e empresa" }));

    expect(await screen.findByRole("alert")).toBeTruthy();
    expect(register).not.toHaveBeenCalled();
  });

  it("abre com os dados adiantados pelo admin e aceita empresa sem documento", async () => {
    preview.mockResolvedValue({
      ...PENDING,
      company: {
        legalName: "Ana Souza",
        tradingName: "Ana Estética",
        phone: "11999990000",
        addressZipCode: "01001000",
        addressStreet: "Rua A",
        addressNumber: "10",
        addressNeighborhood: "Centro",
        addressCity: "São Paulo",
        addressState: "SP",
      },
    });
    register.mockResolvedValue({});
    renderLanding();

    await screen.findByText("Convite para criar sua empresa");
    expect((screen.getByLabelText("Nome fantasia") as HTMLInputElement).value).toBe("Ana Estética");
    expect((screen.getByLabelText("CNPJ ou CPF (opcional)") as HTMLInputElement).value).toBe("");
    fill("Senha", "Senha@1234");
    fill("Confirmar senha", "Senha@1234");
    fireEvent.click(screen.getByRole("button", { name: "Criar conta e empresa" }));

    await waitFor(() => expect(register).toHaveBeenCalled());
    const body = register.mock.calls[0][0];
    expect(body.company.tradingName).toBe("Ana Estética");
    expect(body.company.email).toBe("dono@clinica.com");
    expect(body.company.cnpj).toBe("");
  });

  it("explica quando o convite já foi usado", async () => {
    preview.mockResolvedValue({ ...PENDING, status: "ACCEPTED" });
    renderLanding();

    expect(await screen.findByText("Este convite já foi utilizado.")).toBeTruthy();
  });
});
