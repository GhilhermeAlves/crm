import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { AiMessageBubble } from "@/features/automation/ai/components/AiMessageBubble";

describe("AiMessageBubble — links de rota", () => {
  it("transforma rotas conhecidas em links na resposta do Léo", () => {
    render(
      <AiMessageBubble
        role="assistant"
        content="Abra a Agenda — /agenda e o Controle financeiro (/finance/control)."
      />,
    );
    expect(screen.getByRole("link", { name: "/agenda" }).getAttribute("href")).toBe("/agenda");
    expect(screen.getByRole("link", { name: "/finance/control" }).getAttribute("href")).toBe(
      "/finance/control",
    );
  });

  it("não cria link para rota desconhecida nem na mensagem do usuário", () => {
    render(<AiMessageBubble role="assistant" content="Veja /xyz" />);
    expect(screen.queryByRole("link")).toBeNull();
    render(<AiMessageBubble role="user" content="abre /agenda" />);
    expect(screen.queryByRole("link")).toBeNull();
  });
});
