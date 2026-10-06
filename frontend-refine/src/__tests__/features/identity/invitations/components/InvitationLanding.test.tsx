import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { render, screen, waitFor, fireEvent, act } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import * as React from "react";
import { InvitationLanding } from "@/features/identity/invitations/components/InvitationLanding";
import type { InvitationPreview } from "@/features/identity/invitations/types/invitation.types";

const { previewMock, registerMock, acceptMock, declineMock } = vi.hoisted(() => ({
  previewMock: vi.fn(),
  registerMock: vi.fn(),
  acceptMock: vi.fn(),
  declineMock: vi.fn(),
}));
const { meMock } = vi.hoisted(() => ({ meMock: vi.fn() }));
const { loginMock, logoutMock } = vi.hoisted(() => ({ loginMock: vi.fn(), logoutMock: vi.fn() }));
const { pushMock } = vi.hoisted(() => ({ pushMock: vi.fn() }));

vi.mock("@/features/identity/invitations/services/invitation.service", () => ({
  InvitationService: {
    preview: previewMock,
    register: registerMock,
    accept: acceptMock,
    decline: declineMock,
  },
}));

vi.mock("@/features/identity/auth/services/auth.service", () => ({
  AuthService: { me: meMock },
}));

vi.mock("@/lib/gateway-auth", () => ({
  loginWithGateway: loginMock,
  logoutWithGateway: logoutMock,
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: pushMock, replace: vi.fn() }),
  usePathname: () => "/convite/tok-secreto",
}));

const TOKEN = "tok-secreto";
const PASSWORD = "Kc!Valid1Aa1";

function preview(overrides: Partial<InvitationPreview> = {}): InvitationPreview {
  return {
    companyName: "Clínica Aurora",
    email: "convidado@aurora.com",
    inviteeName: "Fulano de Tal",
    role: "MANAGER",
    status: "PENDING",
    expiresAt: "2026-10-12T10:00:00",
    hasAccount: false,
    ...overrides,
  };
}

function httpError(status: number, data: Record<string, unknown> = {}) {
  return Object.assign(new Error(`HTTP ${status}`), { response: { status, data } });
}

function renderLanding() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <InvitationLanding token={TOKEN} />
    </QueryClientProvider>,
  );
}

function input(label: string): HTMLInputElement {
  return screen.getByLabelText(label) as HTMLInputElement;
}

async function fillSignup(name = "Fulano de Tal") {
  await screen.findByText("Criar conta e entrar");
  fireEvent.change(input("Nome"), { target: { value: name } });
  fireEvent.change(input("Senha"), { target: { value: PASSWORD } });
  fireEvent.change(input("Confirmar senha"), { target: { value: PASSWORD } });
}

function submit() {
  fireEvent.submit(document.querySelector("form") as HTMLFormElement);
}

describe("InvitationLanding (/convite/[token])", () => {
  beforeEach(() => {
    previewMock.mockReset();
    registerMock.mockReset();
    acceptMock.mockReset();
    declineMock.mockReset();
    meMock.mockReset();
    loginMock.mockReset();
    logoutMock.mockReset();
    pushMock.mockReset();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  // ------------------------------------------------------------ prévia

  it("mostra carregando enquanto consulta a prévia", () => {
    previewMock.mockReturnValue(new Promise(() => {}));
    renderLanding();
    expect(screen.getByText("Carregando convite...")).toBeTruthy();
    expect(previewMock).toHaveBeenCalledWith(TOKEN);
  });

  it("PENDING sem conta: mostra empresa, e-mail, nome, perfil e o formulário", async () => {
    previewMock.mockResolvedValue(preview());
    renderLanding();

    expect(await screen.findByText("Convite para Clínica Aurora")).toBeTruthy();
    expect(screen.getAllByText("convidado@aurora.com").length).toBeGreaterThan(0);
    expect(screen.getByText("Gerente")).toBeTruthy();
    expect(input("Nome").value).toBe("Fulano de Tal");
    expect(screen.getByText("Criar conta e entrar")).toBeTruthy();
    expect(meMock).not.toHaveBeenCalled();
  });

  it.each([
    ["ACCEPTED", "Este convite já foi utilizado."],
    ["EXPIRED", "Este convite expirou."],
    ["REVOKED", "Este convite foi revogado."],
  ] as const)("%s: mostra a mensagem específica e nenhum formulário", async (status, message) => {
    previewMock.mockResolvedValue(preview({ status }));
    renderLanding();

    expect(await screen.findByText(message)).toBeTruthy();
    expect(screen.queryByText("Criar conta e entrar")).toBeNull();
    expect(screen.queryByLabelText("Senha")).toBeNull();
  });

  it("ACCEPTED oferece entrar no sistema", async () => {
    previewMock.mockResolvedValue(preview({ status: "ACCEPTED" }));
    renderLanding();

    fireEvent.click(await screen.findByText("Entrar"));
    expect(loginMock).toHaveBeenCalledWith("/crm");
  });

  it("token desconhecido (404): convite inválido", async () => {
    previewMock.mockRejectedValue(httpError(404, { message: "Convite inválido ou inexistente." }));
    renderLanding();

    expect(await screen.findByText("Convite inválido")).toBeTruthy();
  });

  it("erro de rede: mensagem amigável e tentar novamente", async () => {
    previewMock.mockRejectedValueOnce(Object.assign(new Error("Network Error"), { request: {} }));
    previewMock.mockResolvedValueOnce(preview());
    renderLanding();

    expect(await screen.findByText(/Verifique sua conexão/)).toBeTruthy();
    expect(screen.queryByText(/Network Error/)).toBeNull();
    fireEvent.click(screen.getByText("Tentar novamente"));
    expect(await screen.findByText("Convite para Clínica Aurora")).toBeTruthy();
  });

  // ------------------------------------------------- conta existente

  it("e-mail já tem conta e sem sessão: pede para entrar e volta ao convite", async () => {
    previewMock.mockResolvedValue(preview({ hasAccount: true }));
    meMock.mockRejectedValue(httpError(401));
    renderLanding();

    expect(await screen.findByText(/Esta conta já existe/)).toBeTruthy();
    expect(screen.queryByLabelText("Senha")).toBeNull();
    fireEvent.click(screen.getByText("Entrar"));
    expect(loginMock).toHaveBeenCalledWith(`/convite/${TOKEN}`);
  });

  it("conta existente logada com o mesmo e-mail: aceita e vai ao CRM", async () => {
    previewMock.mockResolvedValue(preview({ hasAccount: true }));
    meMock.mockResolvedValue({ id: "u1", email: "Convidado@Aurora.com", companyId: "other" });
    acceptMock.mockResolvedValue({ status: "ACCEPTED" });
    renderLanding();

    fireEvent.click(await screen.findByText("Aceitar convite"));

    await waitFor(() => expect(pushMock).toHaveBeenCalledWith("/crm"));
    expect(acceptMock).toHaveBeenCalledWith(TOKEN);
    expect(acceptMock).toHaveBeenCalledTimes(1);
  });

  it("conta existente logada com outro e-mail: não aceita, oferece sair", async () => {
    previewMock.mockResolvedValue(preview({ hasAccount: true }));
    meMock.mockResolvedValue({ id: "u2", email: "outra@pessoa.com" });
    renderLanding();

    expect(await screen.findByText("outra@pessoa.com")).toBeTruthy();
    expect(screen.queryByText("Aceitar convite")).toBeNull();
    fireEvent.click(screen.getByText("Sair"));
    expect(logoutMock).toHaveBeenCalled();
  });

  it("aceite que encontra o convite já usado (410) mostra a mensagem específica", async () => {
    previewMock.mockResolvedValue(preview({ hasAccount: true }));
    meMock.mockResolvedValue({ id: "u1", email: "convidado@aurora.com" });
    acceptMock.mockRejectedValue(httpError(410, { invitationStatus: "ACCEPTED" }));
    renderLanding();

    fireEvent.click(await screen.findByText("Aceitar convite"));

    expect(await screen.findByText("Este convite já foi utilizado.")).toBeTruthy();
    expect(pushMock).not.toHaveBeenCalled();
  });

  // --------------------------------------------------------- cadastro

  it("envia token, nome e senha (sem e-mail) e segue para o login", async () => {
    previewMock.mockResolvedValue(preview());
    registerMock.mockResolvedValue({ status: "ACCEPTED" });
    renderLanding();

    await fillSignup("Fulano Silva");
    submit();

    await waitFor(() => expect(registerMock).toHaveBeenCalledTimes(1));
    const body = registerMock.mock.calls[0][0];
    expect(body).toEqual({ token: TOKEN, name: "Fulano Silva", password: PASSWORD });
    expect(body).not.toHaveProperty("email");
    expect(await screen.findByText("Conta criada")).toBeTruthy();
    expect(loginMock).toHaveBeenCalledWith("/crm");
  });

  it("permite informar o nome quando o convite não tem nome", async () => {
    previewMock.mockResolvedValue(preview({ inviteeName: null }));
    registerMock.mockResolvedValue({ status: "ACCEPTED" });
    renderLanding();

    await screen.findByText("Criar conta e entrar");
    expect(input("Nome").value).toBe("");
    await fillSignup("Nova Pessoa");
    submit();

    await waitFor(() => expect(registerMock).toHaveBeenCalled());
    expect(registerMock.mock.calls[0][0].name).toBe("Nova Pessoa");
  });

  it("valida senha e confirmação antes de enviar", async () => {
    previewMock.mockResolvedValue(preview());
    renderLanding();

    await screen.findByText("Criar conta e entrar");
    fireEvent.change(input("Senha"), { target: { value: PASSWORD } });
    fireEvent.change(input("Confirmar senha"), { target: { value: "Diferente1!" } });
    submit();

    expect(await screen.findByText("Senhas não conferem")).toBeTruthy();
    expect(registerMock).not.toHaveBeenCalled();
  });

  it("bloqueia duplo envio enquanto o cadastro está em andamento", async () => {
    previewMock.mockResolvedValue(preview());
    let resolveRegister: (value: unknown) => void = () => {};
    registerMock.mockReturnValue(new Promise((resolve) => (resolveRegister = resolve)));
    renderLanding();

    await fillSignup();
    submit();
    submit();
    fireEvent.click(screen.getByText("Criar conta e entrar"));

    await screen.findByText("Criando conta...");
    const button = screen.getByText("Criando conta...").closest("button") as HTMLButtonElement;
    expect(button.disabled).toBe(true);
    submit();

    await act(async () => resolveRegister({ status: "ACCEPTED" }));
    expect(registerMock).toHaveBeenCalledTimes(1);
  });

  it("erro do backend no cadastro mostra mensagem e permite tentar de novo", async () => {
    previewMock.mockResolvedValue(preview());
    registerMock.mockRejectedValueOnce(httpError(429));
    registerMock.mockResolvedValueOnce({ status: "ACCEPTED" });
    renderLanding();

    await fillSignup();
    submit();

    expect(await screen.findByRole("alert")).toBeTruthy();
    expect(screen.getByRole("alert").textContent).toMatch(/tente novamente/i);
    submit();
    await waitFor(() => expect(registerMock).toHaveBeenCalledTimes(2));
  });

  it("cadastro que encontra o convite expirado (410) troca para a mensagem específica", async () => {
    previewMock.mockResolvedValue(preview());
    registerMock.mockRejectedValue(httpError(410, { invitationStatus: "EXPIRED" }));
    renderLanding();

    await fillSignup();
    submit();

    expect(await screen.findByText("Este convite expirou.")).toBeTruthy();
    expect(screen.queryByLabelText("Senha")).toBeNull();
  });

  it("cadastro que descobre conta existente (409) passa para 'entrar'", async () => {
    previewMock.mockResolvedValue(preview());
    registerMock.mockRejectedValue(
      httpError(409, { message: "Já existe uma conta com este e-mail." }),
    );
    meMock.mockRejectedValue(httpError(401));
    renderLanding();

    await fillSignup();
    submit();

    expect(await screen.findByText(/Esta conta já existe/)).toBeTruthy();
    expect(screen.queryByLabelText("Senha")).toBeNull();
  });

  // --------------------------------------------------------- segurança

  it("e-mail do convite é somente leitura e não faz parte do formulário", async () => {
    previewMock.mockResolvedValue(preview());
    renderLanding();

    const email = (await screen.findByLabelText("E-mail")) as HTMLInputElement;
    expect(email.readOnly).toBe(true);
    expect(email.value).toBe("convidado@aurora.com");
    expect(email.getAttribute("name")).toBeNull();
    fireEvent.change(email, { target: { value: "outro@evil.com" } });
    expect(email.value).toBe("convidado@aurora.com");
  });

  it("não grava token em storage nem escreve token/senha no console", async () => {
    const setItem = vi.spyOn(Storage.prototype, "setItem");
    const consoleSpies = (["log", "info", "warn", "error", "debug"] as const).map((method) =>
      vi.spyOn(console, method).mockImplementation(() => {}),
    );
    previewMock.mockResolvedValue(preview());
    registerMock.mockResolvedValue({ status: "ACCEPTED" });
    renderLanding();

    await fillSignup();
    submit();
    await screen.findByText("Conta criada");

    const stored = setItem.mock.calls.map((call) => call.join(" ")).join("\n");
    expect(stored).not.toContain(TOKEN);
    const logged = consoleSpies
      .flatMap((spy) => spy.mock.calls)
      .map((call) => call.map(String).join(" "))
      .join("\n");
    expect(logged).not.toContain(TOKEN);
    expect(logged).not.toContain(PASSWORD);
  });
});
