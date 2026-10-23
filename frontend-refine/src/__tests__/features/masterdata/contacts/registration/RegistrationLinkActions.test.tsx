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

  it("gera o link e abre o wa.me com a mensagem", async () => {
    const open = vi.spyOn(window, "open").mockImplementation(() => null);
    renderActions("(34) 99999-8888");
    fireEvent.click(screen.getByRole("button", { name: /Enviar link de cadastro/ }));
    await waitFor(() => expect(open).toHaveBeenCalled());
    expect(RegistrationService.createLink).toHaveBeenCalledWith("co1", "ct1");
    const [url, target, features] = open.mock.calls[0];
    expect(String(url).startsWith("https://wa.me/5534999998888?text=")).toBe(true);
    expect(decodeURIComponent(String(url))).toContain(
      `Olá, Ana! Para agilizar seu atendimento, complete seu cadastro: ${URL_LINK}`,
    );
    expect(target).toBe("_blank");
    expect(features).toBe("noopener");
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
