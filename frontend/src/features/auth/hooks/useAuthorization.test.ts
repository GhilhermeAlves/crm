import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderHook } from "@testing-library/react";

const { authContextMock } = vi.hoisted(() => ({
  authContextMock: vi.fn(),
}));

vi.mock("./useAuth", () => ({
  useAuth: () => authContextMock(),
}));

// Importar depois do mock via vi.isolateModules não é necessário: como o mock
// é aplicado acima, o caminho real de useAuth não é executado.
import { useAuthorization, usePermission } from "./useAuthorization";

function mockAuth(overrides: {
  permissions?: string[];
  roles?: string[];
} = {}) {
  authContextMock.mockReturnValue({
    user: null,
    isAuthenticated: true,
    isLoading: false,
    logout: vi.fn(),
    loginKeycloak: vi.fn(),
    roles: overrides.roles ?? [],
    permissions: overrides.permissions ?? [],
  });
}

function renderAuthorization() {
  const view = renderHook(() => useAuthorization());
  return view;
}

describe("useAuthorization (Sprint 9 — permissões da empresa ativa)", () => {
  beforeEach(() => {
    authContextMock.mockReset();
  });

  it("can() libera exatamente as permissões do CurrentUser", () => {
    mockAuth({
      permissions: ["contact:read", "contact:create", "user:read"],
    });
    const { result } = renderAuthorization();

    expect(result.current.can("contact:read")).toBe(true);
    expect(result.current.can("user:read")).toBe(true);
    // Não concedida → negada (permite guardar caixas "unchecked" por padrão).
    expect(result.current.can("audit:read")).toBe(false);
    expect(result.current.cannot("audit:read")).toBe(true);
  });

  it("can() sem permission é liberado por conveniência", () => {
    mockAuth();
    const { result } = renderAuthorization();
    expect(result.current.can()).toBe(true);
  });

  it("expõe permissions/roles do usuário e os helpers hasRole/isSuperAdmin", () => {
    mockAuth({ roles: ["VIEWER"], permissions: ["user:read"] });
    const { result } = renderAuthorization();

    expect(result.current.permissions).toEqual(["user:read"]);
    expect(result.current.roles).toEqual(["VIEWER"]);
    expect(result.current.hasRole("VIEWER")).toBe(true);
    expect(result.current.isSuperAdmin).toBe(false);
    expect(result.current.can("user:read")).toBe(true);
    expect(result.current.can("role:manage")).toBe(false);
  });

  it("refaz o cálculo de can() quando as permissões mudam (Company Switcher)", () => {
    mockAuth({ permissions: ["role:manage"] });
    const { result, rerender } = renderAuthorization();

    expect(result.current.can("role:manage")).toBe(true);
    expect(result.current.can("user:read")).toBe(false);

    // Troca de empresa ativa → CurrentUser refeito → novas permissões. O hook
    // relê useAuth() (contexto) e, com as permissões de dependência mudando,
    // o useCallback de can() é recriado no próxima render.
    mockAuth({ permissions: ["user:read"] });
    rerender();

    expect(result.current.can("user:read")).toBe(true);
    expect(result.current.can("role:manage")).toBe(false);
  });

  it("usePermission retorna a função can()", () => {
    mockAuth({ permissions: ["contact:read"] });
    const { result } = renderHook(() => usePermission());
    expect(typeof result.current).toBe("function");
    expect(result.current("contact:read")).toBe(true);
  });
});