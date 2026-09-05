import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { AiAssistantLauncher } from "./AiAssistantLauncher";

const { pushMock, pathnameMock, canChatMock, EmbeddedMock } = vi.hoisted(() => ({
  pushMock: vi.fn(),
  pathnameMock: vi.fn(() => "/customers"),
  canChatMock: vi.fn(() => true),
  EmbeddedMock: vi.fn((_props: { embedded?: boolean }) => <div data-testid="embedded-assistant" />),
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: pushMock, replace: vi.fn() }),
  usePathname: () => pathnameMock(),
}));

vi.mock("../hooks/useAi", () => ({
  useAiPermissions: () => ({ canSuggest: true, canChat: canChatMock() }),
}));

vi.mock("./AiChatAssistant", () => ({
  AiChatAssistant: (props: { embedded?: boolean }) => {
    EmbeddedMock(props);
    return <div data-testid="embedded-assistant" />;
  },
}));

beforeEach(() => {
  pushMock.mockReset();
  pathnameMock.mockReset();
  pathnameMock.mockReturnValue("/customers");
  canChatMock.mockReset();
  canChatMock.mockReturnValue(true);
  EmbeddedMock.mockReset();
  window.localStorage.clear();
  HTMLElement.prototype.setPointerCapture = vi.fn();
  HTMLElement.prototype.releasePointerCapture = vi.fn();
});

describe("AiAssistantLauncher", () => {
  it("renderiza o botão flutuante quando visível", () => {
    render(<AiAssistantLauncher />);
    expect(screen.getByRole("button", { name: "Abrir assistente Léo" })).toBeTruthy();
  });

  it("esconde o launcher sem a permissão ai:chat", () => {
    canChatMock.mockReturnValue(false);
    render(<AiAssistantLauncher />);
    expect(screen.queryByRole("button", { name: /assistente Léo/ })).toBeNull();
  });

  it("esconde o launcher na rota do assistente (tela cheia)", () => {
    pathnameMock.mockReturnValue("/assistant");
    render(<AiAssistantLauncher />);
    expect(screen.queryByRole("button", { name: /assistente Léo/ })).toBeNull();
  });

  it("abre o painel com o chat embutido ao clicar", () => {
    render(<AiAssistantLauncher />);
    fireEvent.click(screen.getByRole("button", { name: "Abrir assistente Léo" }));

    expect(screen.getByText("Léo · Assistente IA")).toBeTruthy();
    expect(screen.getByTestId("embedded-assistant")).toBeTruthy();
    expect(EmbeddedMock).toHaveBeenCalledWith({ embedded: true });
  });

  it("não alterna o painel após um arrasto (drag não atrapalha o clique)", () => {
    render(<AiAssistantLauncher />);
    const button = screen.getByRole("button", { name: "Abrir assistente Léo" });

    fireEvent.pointerDown(button, { pointerId: 1, clientX: 100, clientY: 100 });
    fireEvent.pointerMove(button, { pointerId: 1, clientX: 130, clientY: 120 });
    fireEvent.pointerUp(button, { pointerId: 1, clientX: 130, clientY: 120 });
    fireEvent.click(button);

    expect(button.getAttribute("aria-expanded")).toBe("false");
    expect(screen.queryByText("Léo · Assistente IA")).toBeNull();
  });

  it("persiste a posição do arrasto no localStorage", () => {
    render(<AiAssistantLauncher />);
    const button = screen.getByRole("button", { name: "Abrir assistente Léo" });

    fireEvent.pointerDown(button, { pointerId: 1, clientX: 100, clientY: 100 });
    fireEvent.pointerMove(button, { pointerId: 1, clientX: 200, clientY: 180 });
    fireEvent.pointerUp(button, { pointerId: 1, clientX: 200, clientY: 180 });

    const raw = window.localStorage.getItem("ai-assistant-float-position");
    expect(raw).not.toBeNull();
    const parsed = raw ? (JSON.parse(raw) as { x: number; y: number }) : null;
    expect(parsed?.x).toBeTypeOf("number");
    expect(parsed?.y).toBeTypeOf("number");
  });

  it("fecha o painel com Escape", () => {
    render(<AiAssistantLauncher />);
    fireEvent.click(screen.getByRole("button", { name: "Abrir assistente Léo" }));
    expect(screen.getByText("Léo · Assistente IA")).toBeTruthy();

    fireEvent.keyDown(window, { key: "Escape" });
    expect(screen.queryByText("Léo · Assistente IA")).toBeNull();
  });

  it("abre em tela cheia (navega para a rota do assistente)", () => {
    render(<AiAssistantLauncher />);
    fireEvent.click(screen.getByRole("button", { name: "Abrir assistente Léo" }));

    fireEvent.click(screen.getByRole("button", { name: "Abrir em tela cheia" }));
    expect(pushMock).toHaveBeenCalledWith("/assistant");
  });
});