import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { RegistrationLinkActions } from "@/features/masterdata/contacts/registration/components/RegistrationLinkActions";
import { RegistrationService } from "@/features/masterdata/contacts/registration/registration.service";

const toastSuccess = vi.fn();
vi.mock("sonner", () => ({
  toast: { success: (...a: unknown[]) => toastSuccess(...a), error: vi.fn() },
}));

const URL_LINK = "https://crm.test/cadastro/tok123";

function renderActions(phone: string | null) {
  const client = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
  render(
    <QueryClientProvider client={client}>
      <RegistrationLinkActions companyId="co1" contactId="ct1" firstName="Ana" phone={phone} />
    </QueryClientProvider>,
  );
}

describe("RegistrationLinkActions", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(RegistrationService, "createLink").mockResolvedValue({
      url: URL_LINK,
      expiresAt: "2026-11-01T00:00:00",
    });
  });
  afterEach(() => vi.restoreAllMocks());

  it("abre a janela no clique e redireciona para o wa.me com a mensagem", async () => {
    const win = { location: { href: "" }, opener: "x", close: vi.fn() };
    const open = vi.spyOn(window, "open").mockImplementation(() => win as unknown as Window);
    renderActions("(34) 99999-8888");
    fireEvent.click(screen.getByRole("button", { name: /Enviar link de cadastro/ }));
    // aberta de forma síncrona, antes de gerar o link
    expect(open).toHaveBeenCalledWith("about:blank", "_blank");
    await waitFor(() => expect(win.location.href).not.toBe(""));
    expect(RegistrationService.createLink).toHaveBeenCalledWith("co1", "ct1");
    expect(win.location.href.startsWith("https://wa.me/5534999998888?text=")).toBe(true);
    expect(decodeURIComponent(win.location.href)).toContain(
      `Olá, Ana! Para agilizar seu atendimento, complete seu cadastro: ${URL_LINK}`,
    );
    expect(win.opener).toBeNull();
  });

  it("fecha a janela se a geração do link falhar", async () => {
    const win = { location: { href: "" }, opener: "x", close: vi.fn() };
    vi.spyOn(window, "open").mockImplementation(() => win as unknown as Window);
    vi.spyOn(RegistrationService, "createLink").mockRejectedValue(new Error("x"));
    renderActions("(34) 99999-8888");
    fireEvent.click(screen.getByRole("button", { name: /Enviar link de cadastro/ }));
    await waitFor(() => expect(win.close).toHaveBeenCalled());
  });

  it("pop-up bloqueado: copia o link e avisa", async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, "clipboard", { value: { writeText }, configurable: true });
    vi.spyOn(window, "open").mockImplementation(() => null);
    renderActions("(34) 99999-8888");
    fireEvent.click(screen.getByRole("button", { name: /Enviar link de cadastro/ }));
    await waitFor(() => expect(writeText).toHaveBeenCalledWith(URL_LINK));
    expect(toastSuccess).toHaveBeenCalledWith("Pop-up bloqueado: link copiado");
  });

  it("clipboard indisponível: mostra o link para cópia manual", async () => {
    Object.defineProperty(navigator, "clipboard", { value: undefined, configurable: true });
    renderActions("(34) 99999-8888");
    fireEvent.click(screen.getByRole("button", { name: /Copiar link/ }));
    const input = (await screen.findByLabelText("Link de cadastro")) as HTMLInputElement;
    expect(input.value).toBe(URL_LINK);
    expect(input.readOnly).toBe(true);
  });

  it("copia o link para a área de transferência", async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, "clipboard", { value: { writeText }, configurable: true });
    renderActions("(34) 99999-8888");
    fireEvent.click(screen.getByRole("button", { name: /Copiar link/ }));
    await waitFor(() => expect(writeText).toHaveBeenCalledWith(URL_LINK));
    expect(toastSuccess).toHaveBeenCalled();
  });

  it("sem telefone mostra só Copiar link", () => {
    renderActions(null);
    expect(screen.queryByRole("button", { name: /Enviar link de cadastro/ })).toBeNull();
    expect(screen.getByRole("button", { name: /Copiar link/ })).toBeTruthy();
  });
});
