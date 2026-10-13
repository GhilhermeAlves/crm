import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { PatientTabs } from "@/features/masterdata/contacts/components/PatientTabs";

const { pathState, permsState } = vi.hoisted(() => ({
  pathState: { value: "/contacts/c1" },
  permsState: { value: ["contact:read", "finance:read"] as string[] },
}));

vi.mock("next/navigation", () => ({
  usePathname: () => pathState.value,
}));

vi.mock("next/link", () => ({
  default: ({ href, children, ...rest }: Record<string, unknown>) => (
    <a href={String(href)} {...rest}>
      {children as React.ReactNode}
    </a>
  ),
}));

vi.mock("@/features/identity/auth/hooks/useAuthorization", () => ({
  useAuthorization: () => ({
    can: (permission?: string) => !permission || permsState.value.includes(permission),
  }),
}));

describe("PatientTabs", () => {
  it("renders the tabs and marks the overview tab active", () => {
    pathState.value = "/contacts/c1";
    permsState.value = ["contact:read", "finance:read"];

    render(<PatientTabs contactId="c1" />);

    expect(screen.getByText("Visão geral")).toBeTruthy();
    expect(screen.getByText("Consultas")).toBeTruthy();
    expect(screen.getByText("Financeiro")).toBeTruthy();
    expect(screen.getByText("Arquivos")).toBeTruthy();
    expect(screen.getByText("Anotações")).toBeTruthy();
    expect(screen.getByText("Visão geral").closest("a")?.getAttribute("aria-current")).toBe("page");
  });

  it("hides the Financeiro tab without finance:read", () => {
    pathState.value = "/contacts/c1";
    permsState.value = ["contact:read"];

    render(<PatientTabs contactId="c1" />);

    expect(screen.queryByText("Financeiro")).toBeNull();
  });

  it("marks the child tab active based on the pathname", () => {
    pathState.value = "/contacts/c1/consultas";
    permsState.value = ["contact:read", "finance:read"];

    render(<PatientTabs contactId="c1" />);

    expect(screen.getByText("Consultas").closest("a")?.getAttribute("aria-current")).toBe("page");
    expect(screen.getByText("Visão geral").closest("a")?.getAttribute("aria-current")).toBeNull();
  });
});
