import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import * as React from "react";
import { InviteTemplateCard } from "@/features/masterdata/company/mail/InviteTemplateCard";

const { getMock, postMock, putMock } = vi.hoisted(() => ({
  getMock: vi.fn(),
  postMock: vi.fn(),
  putMock: vi.fn(),
}));

vi.mock("@/lib/api", () => ({
  default: { get: getMock, post: postMock, put: putMock },
}));
vi.mock("sonner", () => ({ toast: { success: vi.fn(), error: vi.fn() } }));

const BASE = "/companies/c1/mail-settings";

function renderCard() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <InviteTemplateCard companyId="c1" />
    </QueryClientProvider>,
  );
}

function mockApi(leoAvailable: boolean) {
  getMock.mockImplementation(async (url: string) => {
    if (url === BASE) {
      return {
        data: {
          configured: true,
          email: "clinica@x.com",
          inviteTemplate: {
            enabled: false,
            logo: null,
            color: null,
            title: null,
            message: null,
            footer: null,
          },
        },
      };
    }
    if (url === `${BASE}/invite-template/leo`) return { data: { available: leoAvailable } };
    throw new Error(`GET inesperado: ${url}`);
  });
  postMock.mockImplementation(async (url: string) => {
    if (url.endsWith("/preview")) return { data: { subject: "Assunto", html: "<p>prévia</p>" } };
    if (url.endsWith("/suggest")) {
      return {
        data: {
          title: "Bem-vinda!",
          message: "Que bom ter você na {empresa} como {perfil}.",
          footer: "Clínica Bela",
        },
      };
    }
    return { data: {} };
  });
  putMock.mockResolvedValue({ data: {} });
}

async function goToStep(name: string) {
  fireEvent.click(await screen.findByRole("button", { name: new RegExp(name) }));
}

describe("InviteTemplateCard (assistente do modelo do convite)", () => {
  beforeEach(() => {
    getMock.mockReset();
    postMock.mockReset();
    putMock.mockReset();
  });

  it("mostra os 5 passos e a prévia ao vivo", async () => {
    mockApi(false);
    renderCard();

    expect(await screen.findByText(/Passo 1 de 5: Ativar/)).toBeTruthy();
    for (const step of ["Ativar", "Logo", "Cor", "Textos", "Revisar"]) {
      expect(screen.getByRole("button", { name: new RegExp(step) })).toBeTruthy();
    }
    await waitFor(() =>
      expect(postMock).toHaveBeenCalledWith(
        `${BASE}/invite-template/preview`,
        expect.objectContaining({ enabled: false }),
      ),
    );
    expect(await screen.findByText("Assunto")).toBeTruthy();
  });

  it("Próximo avança e cor inválida bloqueia o avanço", async () => {
    mockApi(false);
    renderCard();

    fireEvent.click(await screen.findByRole("button", { name: /Próximo/ }));
    expect(screen.getByText(/Passo 2 de 5: Logo/)).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: /Próximo/ }));
    expect(screen.getByText(/Passo 3 de 5: Cor/)).toBeTruthy();

    fireEvent.change(screen.getByLabelText("Cor principal em hexadecimal"), {
      target: { value: "azul" },
    });
    expect((screen.getByRole("button", { name: /Próximo/ }) as HTMLButtonElement).disabled).toBe(
      true,
    );
    expect(screen.getByText(/formato #RRGGBB/)).toBeTruthy();
  });

  it("Leo preenche título, mensagem e rodapé", async () => {
    mockApi(true);
    renderCard();

    await goToStep("Textos");
    fireEvent.click(await screen.findByRole("radio", { name: "Profissional" }));
    fireEvent.click(screen.getByRole("button", { name: /Sugerir textos/ }));

    await waitFor(() =>
      expect((screen.getByLabelText("Título") as HTMLInputElement).value).toBe("Bem-vinda!"),
    );
    expect((screen.getByLabelText("Mensagem de boas-vindas") as HTMLInputElement).value).toBe(
      "Que bom ter você na {empresa} como {perfil}.",
    );
    expect((screen.getByLabelText("Rodapé") as HTMLInputElement).value).toBe("Clínica Bela");
    expect(postMock).toHaveBeenCalledWith(
      `${BASE}/invite-template/suggest`,
      expect.objectContaining({ tone: "PROFISSIONAL" }),
    );
  });

  it("sem Leo disponível não mostra o botão", async () => {
    mockApi(false);
    renderCard();

    await goToStep("Textos");
    expect(await screen.findByLabelText("Título")).toBeTruthy();
    expect(screen.queryByText("Pedir ao Leo")).toBeNull();
  });

  it("salva no passo Revisar", async () => {
    mockApi(false);
    renderCard();

    await goToStep("Revisar");
    fireEvent.click(await screen.findByRole("button", { name: /Salvar modelo/ }));

    await waitFor(() =>
      expect(putMock).toHaveBeenCalledWith(
        `${BASE}/invite-template`,
        expect.objectContaining({ enabled: false, color: "#0b5cff" }),
      ),
    );
  });
});
