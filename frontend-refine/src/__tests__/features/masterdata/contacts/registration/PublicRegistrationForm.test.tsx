import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { PublicRegistrationForm } from "@/features/masterdata/contacts/registration/components/PublicRegistrationForm";

const PREVIEW = {
  companyName: "Clínica Sorriso",
  firstName: "Ana",
  lastName: "Souza",
  phone: "34999998888",
};

function jsonResponse(status: number, body?: unknown) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
  } as Response;
}

describe("PublicRegistrationForm", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });
  afterEach(() => vi.unstubAllGlobals());

  it("carrega o preview e preenche os campos conhecidos", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(200, PREVIEW));
    render(<PublicRegistrationForm token="tok" />);
    expect(await screen.findByText(/Clínica Sorriso/)).toBeTruthy();
    expect((screen.getByLabelText("Nome") as HTMLInputElement).value).toBe("Ana");
    expect((screen.getByLabelText("Sobrenome") as HTMLInputElement).value).toBe("Souza");
    expect((screen.getByLabelText("Celular") as HTMLInputElement).value).toBe("(34) 99999-8888");
    expect(fetchMock.mock.calls[0][0]).toBe("/api/v1/public/registration/tok");
  });

  it("410 mostra link expirado", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(410));
    render(<PublicRegistrationForm token="tok" />);
    expect(await screen.findByText("Este link expirou ou já foi usado.")).toBeTruthy();
  });

  it("404 mostra link não encontrado", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(404));
    render(<PublicRegistrationForm token="tok" />);
    expect(await screen.findByText("Link de cadastro não encontrado.")).toBeTruthy();
  });

  it("envia o POST com os campos e mostra a tela de sucesso", async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse(200, PREVIEW))
      .mockResolvedValueOnce(jsonResponse(204));
    render(<PublicRegistrationForm token="tok" />);
    await screen.findByText(/Clínica Sorriso/);

    fireEvent.change(screen.getByLabelText("CPF"), { target: { value: "52998224725" } });
    fireEvent.change(screen.getByLabelText("Data de nascimento"), {
      target: { value: "1990-05-20" },
    });
    fireEvent.change(screen.getByLabelText("E-mail"), { target: { value: "ana@x.com" } });
    fireEvent.change(screen.getByLabelText("CEP"), { target: { value: "38400000" } });
    fireEvent.change(screen.getByLabelText("Rua"), { target: { value: "Rua A" } });
    fireEvent.change(screen.getByLabelText("Número"), { target: { value: "10" } });
    fireEvent.change(screen.getByLabelText("Cidade"), { target: { value: "Uberlândia" } });
    fireEvent.change(screen.getByLabelText("UF"), { target: { value: "mg" } });
    fireEvent.click(screen.getByRole("button", { name: "Enviar cadastro" }));

    expect(await screen.findByText("Cadastro concluído")).toBeTruthy();
    const [url, init] = fetchMock.mock.calls[1];
    expect(url).toBe("/api/v1/public/registration/tok");
    expect(init.method).toBe("POST");
    expect(JSON.parse(init.body)).toEqual({
      firstName: "Ana",
      lastName: "Souza",
      cpf: "529.982.247-25",
      birthDate: "1990-05-20",
      email: "ana@x.com",
      phone: "(34) 99999-8888",
      address: {
        zipCode: "38400-000",
        street: "Rua A",
        number: "10",
        complement: null,
        neighborhood: null,
        city: "Uberlândia",
        state: "MG",
      },
      website: "",
    });
  });

  it("bloqueia CPF inválido sem chamar o POST", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(200, PREVIEW));
    render(<PublicRegistrationForm token="tok" />);
    await screen.findByText(/Clínica Sorriso/);
    fireEvent.change(screen.getByLabelText("CPF"), { target: { value: "11111111111" } });
    fireEvent.click(screen.getByRole("button", { name: "Enviar cadastro" }));
    expect(await screen.findByText("CPF inválido")).toBeTruthy();
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
  });
});
