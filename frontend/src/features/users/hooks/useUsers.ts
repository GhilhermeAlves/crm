import { useQuery, useMutation } from "@tanstack/react-query";
import { useMutationDefaults } from "@/lib/query/mutation-utils";
import { UserService } from "../services/user.service";
import type {
  ListUsersParams,
  UpdateUserRequest,
  CreateUserRequest,
  InviteUserRequest,
  UpdateProfileRequest,
} from "../types/user.types";

export function useUsers(params?: ListUsersParams) {
  return useQuery({
    queryKey: ["users", params],
    queryFn: () => UserService.list(params),
  });
}

export function useUser(id: string) {
  return useQuery({
    queryKey: ["users", id],
    queryFn: () => UserService.findById(id),
    enabled: !!id,
  });
}

export function useCreateUser() {
  const { onSuccess, onError } = useMutationDefaults();

  return useMutation({
    mutationFn: (data: CreateUserRequest) => UserService.create(data),
    onSuccess: onSuccess({
      successMessage: "Usuário criado com sucesso",
      invalidateKeys: [["users"]],
    }),
    onError: onError({ errorMessage: "Erro ao criar usuário" }),
  });
}

export function useUpdateUser() {
  const { onSuccess, onError } = useMutationDefaults<
    unknown,
    { id: string; data: UpdateUserRequest }
  >();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateUserRequest }) =>
      UserService.update(id, data),
    onSuccess: onSuccess({
      successMessage: "Usuário atualizado com sucesso",
      invalidateKeys: [["users"]],
      extraInvalidate: (qc, _d, { id }) => qc.invalidateQueries({ queryKey: ["users", id] }),
    }),
    onError: onError({ errorMessage: "Erro ao atualizar usuário" }),
  });
}

export function useDeleteUser() {
  const { onSuccess, onError } = useMutationDefaults();

  return useMutation({
    mutationFn: (id: string) => UserService.delete(id),
    onSuccess: onSuccess({
      successMessage: "Usuário excluído com sucesso",
      invalidateKeys: [["users"]],
    }),
    onError: onError({ errorMessage: "Erro ao excluir usuário" }),
  });
}

export function useActivateUser() {
  const { onSuccess, onError } = useMutationDefaults();

  return useMutation({
    mutationFn: (id: string) => UserService.activate(id),
    onSuccess: onSuccess({
      successMessage: "Usuário ativado com sucesso",
      invalidateKeys: [["users"]],
    }),
    onError: onError({ errorMessage: "Erro ao ativar usuário" }),
  });
}

export function useDeactivateUser() {
  const { onSuccess, onError } = useMutationDefaults();

  return useMutation({
    mutationFn: (id: string) => UserService.deactivate(id),
    onSuccess: onSuccess({
      successMessage: "Usuário desativado com sucesso",
      invalidateKeys: [["users"]],
    }),
    onError: onError({ errorMessage: "Erro ao desativar usuário" }),
  });
}

export function useInviteUser() {
  const { onSuccess, onError } = useMutationDefaults();

  return useMutation({
    mutationFn: (data: InviteUserRequest) => UserService.invite(data),
    onSuccess: onSuccess({
      successMessage: "Convite enviado com sucesso",
      invalidateKeys: [["users"]],
    }),
    onError: onError({ errorMessage: "Erro ao enviar convite" }),
  });
}

export function useProfile() {
  return useQuery({
    queryKey: ["profile"],
    queryFn: () => UserService.getProfile(),
  });
}

export function useUpdateProfile() {
  const { onSuccess, onError } = useMutationDefaults();

  return useMutation({
    mutationFn: (data: UpdateProfileRequest) => UserService.updateProfile(data),
    onSuccess: onSuccess({
      successMessage: "Perfil atualizado com sucesso",
      invalidateKeys: [["profile"]],
    }),
    onError: onError({ errorMessage: "Erro ao atualizar perfil" }),
  });
}
