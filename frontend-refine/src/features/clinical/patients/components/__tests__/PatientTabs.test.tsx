import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { PatientTabs } from "@/features/clinical/patients/components/PatientTabs";

const { pathState, permsState, segmentState } = vi.hoisted(() => ({
  segmentState: { value: "DENTAL" },
  pathState: { value: "/patients/c1" },
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

vi.mock("@/features/identity/auth/hooks/useCompanyId", () => ({
  useCompanyId: () => "co1",
}));

vi.mock("@/features/identity/modules/hooks/useCompanyModules", () => ({
  useCompanyModules: () => ({ data: { segment: segmentState.value } }),
}));

describe("PatientTabs", () => {
  it("renders the tabs and marks Consultas (default) active", () => {
    pathState.value = "/patients/c1";
    permsState.value = ["contact:read", "finance:read"];

    render(<PatientTabs contactId="c1" />);

    expect(screen.getByText("Consultas")).toBeTruthy();
    expect(screen.getByText("Financeiro")).toBeTruthy();
    expect(screen.getByText("Arquivos")).toBeTruthy();
    expect(screen.getByText("Anotações")).toBeTruthy();
    expect(screen.getByText("Consultas").closest("a")?.getAttribute("aria-current")).toBe("page");
    expect(screen.getByText("Consultas").closest("a")?.getAttribute("href")).toBe("/patients/c1");
  });

  it("hides the Financeiro tab without finance:read", () => {
    pathState.value = "/patients/c1";
    permsState.value = ["contact:read"];

    render(<PatientTabs contactId="c1" />);

    expect(screen.queryByText("Financeiro")).toBeNull();
  });

  it("shows Tricoscopia only for trichology clinics", () => {
    pathState.value = "/patients/c1";
    permsState.value = ["contact:read"];

    segmentState.value = "DENTAL";
    const { unmount } = render(<PatientTabs contactId="c1" />);
    expect(screen.queryByText("Tricoscopia")).toBeNull();
    expect(screen.getByText("Evoluções")).toBeTruthy();
    unmount();

    segmentState.value = "TRICHOLOGY";
    render(<PatientTabs contactId="c1" />);
    expect(screen.getByText("Tricoscopia").closest("a")?.getAttribute("href")).toBe(
      "/patients/c1/tricoscopia",
    );
  });

  it("marks the child tab active based on the pathname", () => {
    pathState.value = "/patients/c1/arquivos";
    permsState.value = ["contact:read", "finance:read"];

    render(<PatientTabs contactId="c1" />);

    expect(screen.getByText("Arquivos").closest("a")?.getAttribute("aria-current")).toBe("page");
    expect(screen.getByText("Consultas").closest("a")?.getAttribute("aria-current")).toBeNull();
  });
});
