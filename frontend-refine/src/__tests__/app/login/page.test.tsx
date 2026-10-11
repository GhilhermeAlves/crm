import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import LoginPage from "@/app/login/page";

const { loginWithGatewayMock } = vi.hoisted(() => ({
  loginWithGatewayMock: vi.fn(),
}));

vi.mock("@/lib/gateway-auth", () => ({
  loginWithGateway: loginWithGatewayMock,
}));

vi.mock("next/navigation", () => ({
  useSearchParams: () => new URLSearchParams("redirect=/crm/agenda"),
}));

describe("LoginPage", () => {
  it("redireciona direto para o Keycloak preservando o redirect", () => {
    render(<LoginPage />);
    expect(loginWithGatewayMock).toHaveBeenCalledWith("/crm/agenda");
    expect(screen.getByText(/Redirecionando para o login seguro/)).not.toBeNull();
  });

  it("volta à apresentação quando restaurada do bfcache", () => {
    const replace = vi.fn();
    Object.defineProperty(window, "location", { configurable: true, value: { replace } });
    render(<LoginPage />);
    const event = new Event("pageshow") as PageTransitionEvent;
    Object.defineProperty(event, "persisted", { value: true });
    window.dispatchEvent(event);
    expect(replace).toHaveBeenCalledWith("/");
  });
});
