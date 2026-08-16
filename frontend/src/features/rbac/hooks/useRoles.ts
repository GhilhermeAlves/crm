import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { RbacService } from "../services/rbac.service";
import type {
  CreateRoleRequest,
  UpdateRoleRequest,
  AssignRoleRequest,
  AssignPermissionRequest,
} from "../types/rbac.types";

export function useRoles() {
  return useQuery({
    queryKey: ["roles"],
    queryFn: () => RbacService.listRoles(),
  });
}

export function useRole(id: string) {
  return useQuery({
    queryKey: ["roles", id],
    queryFn: () => RbacService.getRoleById(id),
    enabled: !!id,
  });
}

export function useCreateRole() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateRoleRequest) => RbacService.createRole(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["roles"] });
      toast.success("Role criada com sucesso");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao criar role");
    },
  });
}

export function useUpdateRole() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateRoleRequest }) =>
      RbacService.updateRole(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: ["roles"] });
      queryClient.invalidateQueries({ queryKey: ["roles", id] });
      toast.success("Role atualizada com sucesso");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao atualizar role");
    },
  });
}

export function useDeleteRole() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => RbacService.deleteRole(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["roles"] });
      toast.success("Role excluída com sucesso");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao excluir role");
    },
  });
}

export function useAssignPermission() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      roleId,
      data,
    }: {
      roleId: string;
      data: AssignPermissionRequest;
    }) => RbacService.assignPermission(roleId, data),
    onSuccess: (_, { roleId }) => {
      queryClient.invalidateQueries({ queryKey: ["roles"] });
      queryClient.invalidateQueries({ queryKey: ["roles", roleId] });
      toast.success("Permissão atribuída com sucesso");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao atribuir permissão");
    },
  });
}

export function useRemovePermission() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      roleId,
      permissionId,
    }: {
      roleId: string;
      permissionId: string;
    }) => RbacService.removePermission(roleId, permissionId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["roles"] });
      toast.success("Permissão removida com sucesso");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao remover permissão");
    },
  });
}

export function useAssignRoleToUser() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      userId,
      data,
    }: {
      userId: string;
      data: AssignRoleRequest;
    }) => RbacService.assignRoleToUser(userId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["users"] });
      queryClient.invalidateQueries({ queryKey: ["roles"] });
      toast.success("Role atribuída ao usuário com sucesso");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao atribuir role ao usuário");
    },
  });
}

export function useRemoveRoleFromUser() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ userId, roleId }: { userId: string; roleId: string }) =>
      RbacService.removeRoleFromUser(userId, roleId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["users"] });
      queryClient.invalidateQueries({ queryKey: ["roles"] });
      toast.success("Role removida do usuário com sucesso");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao remover role do usuário");
    },
  });
}

export function useUserRoles(userId: string) {
  return useQuery({
    queryKey: ["roles", "user", userId],
    queryFn: () => RbacService.getUserRoles(userId),
    enabled: !!userId,
  });
}

export function usePermissions() {
  return useQuery({
    queryKey: ["permissions"],
    queryFn: () => RbacService.listAllPermissions(),
  });
}

export function usePermissionsByModule(module: string) {
  return useQuery({
    queryKey: ["permissions", module],
    queryFn: () => RbacService.listPermissionsByModule(module),
    enabled: !!module,
  });
}
