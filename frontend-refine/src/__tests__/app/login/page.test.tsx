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
});
