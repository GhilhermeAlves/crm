import { describe, it, expect } from "vitest";
import { render, screen, within } from "@testing-library/react";
import PresentationPage from "@/app/apresentacao/page";

describe("Apresentação — planos", () => {
  it("mostra os 5 planos com preço e limites do cadastro", () => {
    render(<PresentationPage />);

    const plans = ["essential", "starter", "professional", "business", "enterprise"];
    plans.forEach((p) => expect(screen.getByTestId(`plan-${p}`)).toBeTruthy());

    const essential = within(screen.getByTestId("plan-essential"));
    expect(essential.getByText("R$ 49,90")).toBeTruthy();
    expect(essential.getByText("1 usuário")).toBeTruthy();
    expect(essential.getByText("500 contatos")).toBeTruthy();

    const pro = within(screen.getByTestId("plan-professional"));
    expect(pro.getByText("Mais escolhido")).toBeTruthy();
    expect(pro.getByText("5 usuários")).toBeTruthy();
    expect(pro.getByText("10.000 contatos")).toBeTruthy();
    expect(pro.getByText("25 GB de arquivos")).toBeTruthy();
  });

  it("tem link de Planos no menu", () => {
    render(<PresentationPage />);
    expect(screen.getByRole("link", { name: "Planos" }).getAttribute("href")).toBe("#planos");
  });
});
