import { describe, it, expect, vi, beforeEach } from "vitest";
import { act, renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import * as React from "react";
import {
  useAssignPermission,
  useAssignRoleToUser,
  useCreateRole,
  useDeleteRole,
  useRemovePermission,
  useRemoveRoleFromUser,
  useUpdateRole,
} from "@/features/identity/rbac/hooks/useRoles";

const { rbacMock } = vi.hoisted(() => ({
  rbacMock: {
    createRole: vi.fn(),
    updateRole: vi.fn(),
    deleteRole: vi.fn(),
    assignPermission: vi.fn(),
    removePermission: vi.fn(),
    assignRoleToUser: vi.fn(),
    removeRoleFromUser: vi.fn(),
  },
}));

vi.mock("@/features/identity/rbac/services/rbac.service", () => ({ RbacService: rbacMock }));

type MutationLike = {
  mutate: (variables: never) => void;
  isSuccess: boolean;
};

async function expectMeInvalidated(useHook: () => MutationLike, variables: unknown) {
  const invalidateSpy = vi.spyOn(QueryClient.prototype, "invalidateQueries");
  const client = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
  const wrapper = ({ children }: { children: React.ReactNode }) =>
    React.createElement(QueryClientProvider, { client }, children);

  try {
    const { result } = renderHook(useHook, { wrapper });
    await act(async () => {
      result.current.mutate(variables as never);
    });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ["me"] });
  } finally {
    invalidateSpy.mockRestore();
  }
}

describe("mutations de papel/override — revalidação de /me", () => {
  beforeEach(() => {
    Object.values(rbacMock).forEach((fn) => fn.mockReset());
  });

  it("useCreateRole invalida ['me']", async () => {
    rbacMock.createRole.mockResolvedValue({ id: "role-1" });
    await expectMeInvalidated(useCreateRole, { name: "VIEWER" });
  });

  it("useUpdateRole invalida ['me']", async () => {
    rbacMock.updateRole.mockResolvedValue({ id: "role-1" });
    await expectMeInvalidated(useUpdateRole, { id: "role-1", data: { isActive: false } });
  });

  it("useDeleteRole invalida ['me']", async () => {
    rbacMock.deleteRole.mockResolvedValue(undefined);
    await expectMeInvalidated(useDeleteRole, "role-1");
  });

  it("useAssignPermission invalida ['me']", async () => {
    rbacMock.assignPermission.mockResolvedValue(undefined);
    await expectMeInvalidated(useAssignPermission, {
      roleId: "role-1",
      data: { permissionId: "perm-1" },
    });
  });

  it("useRemovePermission invalida ['me']", async () => {
    rbacMock.removePermission.mockResolvedValue(undefined);
    await expectMeInvalidated(useRemovePermission, { roleId: "role-1", permissionId: "perm-1" });
  });

  it("useAssignRoleToUser invalida ['me']", async () => {
    rbacMock.assignRoleToUser.mockResolvedValue(undefined);
    await expectMeInvalidated(useAssignRoleToUser, {
      userId: "user-1",
      data: { roleId: "role-1" },
    });
  });

  it("useRemoveRoleFromUser invalida ['me']", async () => {
    rbacMock.removeRoleFromUser.mockResolvedValue(undefined);
    await expectMeInvalidated(useRemoveRoleFromUser, { userId: "user-1", roleId: "role-1" });
  });
});
