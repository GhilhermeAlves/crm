import { beforeEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { RbacService } from "@/features/identity/rbac/services/rbac.service";
import type {
  Permission,
  UserPermissionsResponse,
} from "@/features/identity/rbac/types/rbac.types";
import type { Member } from "@/features/identity/members/types/member.types";
import { UserPermissionsDialog } from "@/features/identity/rbac/components/UserPermissionsDialog";

vi.mock("@/features/identity/rbac/services/rbac.service", () => ({
  RbacService: {
    getUserPermissions: vi.fn(),
    listAllPermissions: vi.fn(),
    setUserPermissionOverride: vi.fn(),
    removeUserPermissionOverride: vi.fn(),
  },
}));

vi.mock("sonner", () => ({
  toast: { success: vi.fn(), error: vi.fn() },
}));

const member = {
  userId: "u1",
  name: "Ana",
  email: "ana@example.com",
  role: "Administradora",
  status: "ACTIVE",
  joinedAt: null,
} as Member;

function permission(id: string, name: string, description: string): Permission {
  return {
    id,
    name,
    description,
    module: name.split(":")[0],
    resource: name.split(":")[1],
    action: "read",
    createdAt: "2026-01-01T00:00:00Z",
  };
}

const permissions = [
  permission("1", "finance:read", "Visualizar finanças"),
  permission("2", "finance:manage", "Gerenciar finanças"),
  permission("3", "patient:read", "Visualizar prontuário do paciente"),
];

const userPermissions: UserPermissionsResponse = {
  userId: "u1",
  roles: ["Administradora"],
  effective: ["finance:read", "finance:manage"],
  overrides: [
    { permissionName: "finance:manage", effect: "ALLOW" },
    { permissionName: "patient:read", effect: "DENY" },
  ],
  rolePermissionNames: ["finance:read"],
};

function renderDialog() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  render(
    <QueryClientProvider client={queryClient}>
      <UserPermissionsDialog member={member} open onOpenChange={vi.fn()} />
    </QueryClientProvider>,
  );
  return { queryClient };
}

async function renderedRows() {
  await screen.findByText("finance:read");
  return ["finance:read", "finance:manage", "patient:read"].map(
    (name) => screen.getByText(name).closest("tr") as HTMLElement,
  );
}

describe("UserPermissionsDialog", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(RbacService.listAllPermissions).mockResolvedValue(permissions);
    vi.mocked(RbacService.getUserPermissions).mockResolvedValue(userPermissions);
    vi.mocked(RbacService.setUserPermissionOverride).mockResolvedValue(undefined);
    vi.mocked(RbacService.removeUserPermissionOverride).mockResolvedValue(undefined);
  });

  it("agrupa por módulo com contador e mostra a origem em português", async () => {
    renderDialog();
    const [fromRole, allowed, denied] = await renderedRows();

    expect(screen.getByText("2 de 2")).toBeTruthy();
    expect(screen.getByText("1 de 1")).toBeTruthy();
    expect(within(fromRole).getByText("Pelo perfil")).toBeTruthy();
    expect(within(allowed).getByText("Adicionada")).toBeTruthy();
    expect(within(denied).getByText("Removida")).toBeTruthy();
  });

  it("filtra as permissões pelo nome", async () => {
    renderDialog();
    await renderedRows();

    fireEvent.change(screen.getByLabelText("Buscar permissões"), {
      target: { value: "finance:manage" },
    });

    expect(screen.getByText("finance:manage")).toBeTruthy();
    expect(screen.queryByText("finance:read")).toBeNull();
    expect(screen.getByText("1 de 2")).toBeTruthy();
  });

  it("filtra as permissões pela descrição", async () => {
    renderDialog();
    await renderedRows();

    fireEvent.change(screen.getByLabelText("Buscar permissões"), {
      target: { value: "prontuário" },
    });

    expect(screen.getByText("patient:read")).toBeTruthy();
    expect(screen.queryByText("finance:read")).toBeNull();
    expect(screen.getByText("1 de 1")).toBeTruthy();
  });

  it("permite recolher o bloco de um módulo", async () => {
    renderDialog();
    await renderedRows();

    const details = screen.getByText("Financeiro").closest("details") as HTMLDetailsElement;
    const summary = screen.getByText("Financeiro").closest("summary") as HTMLElement;
    expect(details.open).toBe(true);

    fireEvent.click(summary);
    expect(details.open).toBe(false);
  });

  it("invalida as permissões do usuário e de /me após aplicar um override", async () => {
    const { queryClient } = renderDialog();
    const invalidate = vi.spyOn(queryClient, "invalidateQueries");
    const [fromRole] = await renderedRows();

    fireEvent.click(within(fromRole).getByRole("button", { name: "Permitir" }));

    await waitFor(() =>
      expect(invalidate).toHaveBeenCalledWith({ queryKey: ["user-permissions", "u1"] }),
    );
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["me"] });
  });
});
