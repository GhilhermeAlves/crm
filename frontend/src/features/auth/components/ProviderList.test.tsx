import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { ProviderList } from "./ProviderList";
import type { IdentityProviderInfo } from "../types/identity-provider";

const { loginWithGatewayMock } = vi.hoisted(() => ({
  loginWithGatewayMock: vi.fn(),
}));

const { providersState } = vi.hoisted(() => ({
  providersState: {
    data: undefined as IdentityProviderInfo[] | undefined,
    isLoading: false,
    isError: false,
  },
}));

vi.mock("next/navigation", () => ({
  useSearchParams: () => ({ get: (key: string) => (key === "redirect" ? "/leads" : null) }),
}));

vi.mock("../hooks/useIdentityProviders", () => ({
  useIdentityProviders: () => providersState,
}));

vi.mock("@/lib/gateway-auth", () => ({
  loginWithGateway: loginWithGatewayMock,
  IDENTITY_PROVIDERS: {
    GOOGLE: "google",
    MICROSOFT: "microsoft",
    APPLE: "apple",
    PHONE: "phone",
  } as const,
}));

function serverCatalog(overrides: Partial<Record<string, boolean>> = {}) {
  const labels: Record<string, string> = {
    google: "Google",
    microsoft: "Microsoft",
    apple: "Apple",
    phone: "Telefone",
  };
  return (["google", "microsoft", "apple", "phone"] as const).map((alias) => ({
    alias,
    label: labels[alias],
    available: overrides[alias] ?? false,
  }));
}

describe("ProviderList (Sprint 7.0)", () => {
  beforeEach(() => {
    loginWithGatewayMock.mockClear();
    providersState.data = serverCatalog();
    providersState.isLoading = false;
    providersState.isError = false;
  });

  it("renders the four providers in a fixed order", () => {
    render(<ProviderList />);
    const buttons = screen.getAllByRole("button").map((b) => b.getAttribute("data-provider"));
    expect(buttons).toEqual(["google", "microsoft", "apple", "phone"]);
  });

  it("enables providers the server marks as available", () => {
    providersState.data = serverCatalog({ google: true, apple: true });
    render(<ProviderList />);
    const google = screen.getByRole("button", { name: "Entrar com Google" });
    const apple = screen.getByRole("button", { name: "Entrar com Apple" });
    const microsoft = screen.getByRole("button", { name: "Entrar com Microsoft" });
    const phone = screen.getByRole("button", { name: "Entrar com Telefone" });
    expect((google as HTMLButtonElement).disabled).toBe(false);
    expect((apple as HTMLButtonElement).disabled).toBe(false);
    expect((microsoft as HTMLButtonElement).disabled).toBe(true);
    expect((phone as HTMLButtonElement).disabled).toBe(true);
  });

  it("navigates with the provider alias preserving the redirect", () => {
    providersState.data = serverCatalog({ google: true });
    render(<ProviderList />);
    fireEvent.click(screen.getByRole("button", { name: "Entrar com Google" }));
    expect(loginWithGatewayMock).toHaveBeenCalledWith("/leads", "google");
  });

  it("does not navigate when the provider is unavailable (defense in depth)", () => {
    render(<ProviderList />);
    fireEvent.click(screen.getByRole("button", { name: "Entrar com Google" }));
    expect(loginWithGatewayMock).not.toHaveBeenCalled();
  });

  it("shows the loading message while the catalog is being fetched", () => {
    providersState.isLoading = true;
    render(<ProviderList />);
    expect(screen.queryByTestId("providers-loading")).not.toBeNull();
  });

  it("shows an error message and keeps every provider disabled when the catalog fails", () => {
    providersState.isError = true;
    providersState.data = undefined;
    render(<ProviderList />);
    expect(screen.queryByTestId("providers-error")).not.toBeNull();
    for (const button of screen.getAllByRole("button")) {
      expect((button as HTMLButtonElement).disabled).toBe(true);
    }
  });
});
