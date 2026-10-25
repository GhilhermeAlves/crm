import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import { ModuleGate, moduleForPath } from "@/components/layout/ModuleGate";

let pathname = "/finance";
vi.mock("next/navigation", () => ({ usePathname: () => pathname }));

const modulesState = vi.hoisted(() => ({ enabled: null as string[] | null }));
vi.mock("@/features/identity/modules/hooks/useCompanyModules", () => ({
  useActiveCompanyModules: () => ({
    isEnabled: (m: string) => modulesState.enabled === null || modulesState.enabled.includes(m),
  }),
}));

describe("moduleForPath", () => {
  it("acha o módulo pelo item e pelos submenus do menu", () => {
    expect(moduleForPath("/tasks")).toBe("TASKS");
    expect(moduleForPath("/reports/export/cash-flow")).toBe("REPORTS");
  });

  it("não exige módulo para Início e Configurações", () => {
    expect(moduleForPath("/crm")).toBeUndefined();
    expect(moduleForPath("/settings/company")).toBeUndefined();
  });
});

describe("ModuleGate", () => {
  beforeEach(() => {
    modulesState.enabled = null;
  });

  it("mostra o aviso quando o módulo da página está desligado", () => {
    pathname = "/tasks";
    modulesState.enabled = ["CRM"];
    render(<ModuleGate>conteúdo</ModuleGate>);
    expect(screen.getByText("Módulo não habilitado para esta empresa")).toBeTruthy();
    expect(screen.queryByText("conteúdo")).toBeNull();
  });

  it("mostra a página quando o módulo está ligado ou ainda carregando", () => {
    pathname = "/tasks";
    render(<ModuleGate>conteúdo</ModuleGate>);
    expect(screen.getByText("conteúdo")).toBeTruthy();
  });
});
