import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { LoginBrand } from "./LoginBrand";

describe("LoginBrand (Sprint 7.0)", () => {
  it("renders the textual fallback with the wordmark initials when no logo is set", () => {
    render(<LoginBrand wordmark="CRM" />);
    expect(screen.queryByTestId("login-brand-fallback")?.textContent).toBe("CR");
    expect(screen.queryByTestId("login-brand")).not.toBeNull();
  });

  it("renders the logo image when logoSrc is provided", () => {
    render(<LoginBrand logoSrc="/logo.svg" logoAlt="CRM logo" />);
    const img = screen.queryByTestId("login-brand-logo") as HTMLImageElement | null;
    expect(img).not.toBeNull();
    expect(img?.getAttribute("src")).toBe("/logo.svg");
    expect(img?.getAttribute("alt")).toBe("CRM logo");
    expect(screen.queryByTestId("login-brand-fallback")).toBeNull();
  });

  it("renders the wordmark next to the mark in desktop variant", () => {
    render(<LoginBrand wordmark="MeuCRM" variant="desktop" />);
    expect(screen.queryByTestId("login-brand")?.textContent).toContain("MeuCRM");
  });

  it("applies size classes to the mark slot", () => {
    const { container } = render(<LoginBrand size="lg" />);
    const slot = container.querySelector("[data-logo-slot]");
    expect(slot?.getAttribute("class")).toContain("h-20");
  });
});
