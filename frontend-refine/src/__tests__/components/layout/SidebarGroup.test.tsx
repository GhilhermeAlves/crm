import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { CalendarDays, Settings, Users } from "lucide-react";
import { SidebarGroup, groupHasActive } from "@/components/layout/SidebarGroup";
import type { NavGroup } from "@/components/layout/navigation";

let pathname = "/crm";
vi.mock("next/navigation", () => ({ usePathname: () => pathname }));
vi.mock("@/features/identity/auth/hooks/useAuth", () => ({
  useAuth: () => ({ permissions: [], roles: [] }),
}));

const modulesState = vi.hoisted(() => ({ enabled: null as string[] | null }));
vi.mock("@/features/identity/modules/hooks/useCompanyModules", () => ({
  useActiveCompanyModules: () => ({
    enabledModules: modulesState.enabled,
    isEnabled: (m: string) => modulesState.enabled === null || modulesState.enabled.includes(m),
  }),
}));

const group: NavGroup = {
  title: "Configurações",
  icon: Settings,
  items: [
    { label: "Equipe", href: "/settings/company/users", icon: Users },
    {
      label: "Agenda",
      href: "/settings/agenda",
      icon: CalendarDays,
      children: [
        { label: "Ajustes gerais", href: "/settings/agenda" },
        { label: "Cadeiras", href: "/settings/agenda/chairs" },
      ],
    },
  ],
};

beforeEach(() => {
  pathname = "/crm";
  modulesState.enabled = null;
});

describe("SidebarGroup com a barra recolhida", () => {
  it("mostra o ícone do grupo e abre os itens e subitens ao lado", () => {
    render(<SidebarGroup group={group} collapsed />);

    const trigger = screen.getByRole("button", { name: "Menu Configurações" });
    fireEvent.keyDown(trigger, { key: "Enter" });

    expect(screen.getByRole("menuitem", { name: "Equipe" }).getAttribute("href")).toBe(
      "/settings/company/users",
    );
    expect(screen.getByRole("menuitem", { name: "Cadeiras" }).getAttribute("href")).toBe(
      "/settings/agenda/chairs",
    );
  });
});

describe("SidebarGroup com a barra aberta", () => {
  it("começa fechado fora da seção e abre ao clicar no título", () => {
    render(<SidebarGroup group={group} collapsed={false} />);
    const title = screen.getByRole("button", { name: /Configurações/ });

    expect(title.getAttribute("aria-expanded")).toBe("false");
    expect(screen.queryByText("Equipe")).toBeNull();
    fireEvent.click(title);
    expect(screen.getByText("Equipe")).toBeTruthy();
  });

  it("já abre o grupo da página atual", () => {
    pathname = "/settings/agenda/chairs";
    render(<SidebarGroup group={group} collapsed={false} />);
    expect(
      screen.getByRole("button", { name: /Configurações/ }).getAttribute("aria-expanded"),
    ).toBe("true");
  });
});

describe("groupHasActive", () => {
  it("considera itens e subitens", () => {
    expect(groupHasActive(group.items, "/settings/agenda/chairs")).toBe(true);
    expect(groupHasActive(group.items, "/settings/company/users/123")).toBe(true);
    expect(groupHasActive(group.items, "/crm")).toBe(false);
  });
});

describe("SidebarGroup e módulos da empresa", () => {
  const clinicGroup: NavGroup = {
    title: "Principal",
    icon: Settings,
    items: [
      { label: "Agenda", href: "/agenda", icon: CalendarDays, module: "AGENDA" },
      { label: "Pacientes", href: "/patients", icon: Users, module: "PATIENTS" },
      { label: "Contatos", href: "/contacts", icon: Users, module: "CRM" },
    ],
  };

  it("esconde itens de módulos desligados", () => {
    modulesState.enabled = ["AGENDA", "PATIENTS"];
    pathname = "/agenda";
    render(<SidebarGroup group={clinicGroup} collapsed={false} />);

    expect(screen.getByText("Pacientes")).toBeTruthy();
    expect(screen.queryByText("Contatos")).toBeNull();
  });

  it("mostra tudo enquanto os módulos não carregaram", () => {
    modulesState.enabled = null;
    pathname = "/agenda";
    render(<SidebarGroup group={clinicGroup} collapsed={false} />);

    expect(screen.getByText("Pacientes")).toBeTruthy();
    expect(screen.getByText("Contatos")).toBeTruthy();
  });
});
