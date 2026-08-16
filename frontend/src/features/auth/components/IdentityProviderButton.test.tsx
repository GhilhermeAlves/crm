import { describe, it, expect, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { IdentityProviderButton } from "./IdentityProviderButton";
import type { IdentityProviderInfo } from "../types/identity-provider";

const google: IdentityProviderInfo = {
  alias: "google",
  label: "Google",
  available: true,
};

function renderButton(
  overrides: Partial<IdentityProviderInfo> = {},
  props = {},
) {
  const provider = { ...google, ...overrides };
  const onSelect = vi.fn();
  render(
    <IdentityProviderButton
      provider={provider}
      onSelect={onSelect}
      {...props}
    />,
  );
  return { onSelect, provider };
}

describe("IdentityProviderButton (Sprint 7.0)", () => {
  it("renders the provider label and its icon", () => {
    renderButton();
    const button = screen.queryByRole("button", { name: "Entrar com Google" });
    expect(button).not.toBeNull();
    expect(screen.queryByTestId("provider-icon")).not.toBeNull();
  });

  it("calls onSelect with the provider when clicked", () => {
    const { onSelect, provider } = renderButton();
    const button = screen.getByRole("button", { name: "Entrar com Google" });
    fireEvent.click(button);
    expect(onSelect).toHaveBeenCalledWith(provider);
  });

  it("shows 'Em breve' and disables the button when unavailable", () => {
    renderButton({ available: false });
    const button = screen.getByRole("button", { name: /Entrar com Google/ });
    expect((button as HTMLButtonElement).disabled).toBe(true);
    expect(screen.queryByText("Em breve")).not.toBeNull();
  });

  it("renders a spinner while loading and blocks clicks", () => {
    const { onSelect } = renderButton({}, { loading: true });
    const button = screen.getByRole("button", { name: "Entrar com Google" });
    expect((button as HTMLButtonElement).disabled).toBe(true);
    expect(screen.queryByTestId("provider-loading")).not.toBeNull();
    fireEvent.click(button);
    expect(onSelect).not.toHaveBeenCalled();
  });

  it("renders the error message when provided", () => {
    renderButton({}, { error: "Falha na autenticação" });
    expect(screen.queryByRole("alert")?.textContent).toContain(
      "Falha na autenticação",
    );
  });

  it("respects an explicit disabled prop", () => {
    renderButton({}, { disabled: true });
    const button = screen.getByRole("button", { name: "Entrar com Google" });
    expect((button as HTMLButtonElement).disabled).toBe(true);
  });
});
