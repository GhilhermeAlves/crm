"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { AuthService } from "../services/auth.service";
import type {
  RegisterRequest,
  ForgotPasswordRequest,
  ResetPasswordRequest,
} from "../types/auth.types";

export function useRegister() {
  const router = useRouter();

  return useMutation({
    mutationFn: (data: RegisterRequest) => AuthService.register(data),
    onSuccess: () => {
      toast.success("Conta criada com sucesso. Faça login para continuar.");
      router.push("/login");
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      const message = error.response?.data?.message || "Erro ao criar conta";
      toast.error(message);
    },
  });
}

export function useForgotPassword() {
  return useMutation({
    mutationFn: (data: ForgotPasswordRequest) => AuthService.forgotPassword(data),
    onSuccess: () => {
      toast.success("Se o email existir, você receberá um link de recuperação");
    },
    onError: () => {
      toast.success("Se o email existir, você receberá um link de recuperação");
    },
  });
}

export function useResetPassword() {
  const router = useRouter();

  return useMutation({
    mutationFn: (data: ResetPasswordRequest) => AuthService.resetPassword(data),
    onSuccess: () => {
      toast.success("Senha redefinida com sucesso");
      router.push("/login");
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      const message = error.response?.data?.message || "Erro ao redefinir senha";
      toast.error(message);
    },
  });
}

/**
 * Busca a identidade de negócio no crm-backend (`/api/v1/auth/me` — Sprint 1).
 * Só é habilitado depois que o Keycloak está inicializado e autenticado, para
 * não disparar chamadas com token antigo ou antes do init (race condition).
 * Dependência futura: migrar para o CurrentUser público do crm-auth-service.
 */
export function useMe(enabled: boolean) {
  return useQuery({
    queryKey: ["me"],
    queryFn: () => AuthService.me(),
    enabled,
    retry: false,
    staleTime: 5 * 60 * 1000,
  });
}

/**
 * Empresas do usuário com membership ativa (Company Switcher, Sprint 8.4).
 * Desabilitado para usuário sem empresa (onboarding).
 */
export function useMyCompanies(enabled = true) {
  return useQuery({
    queryKey: ["me", "companies"],
    queryFn: () => AuthService.myCompanies(),
    enabled,
    staleTime: 2 * 60 * 1000,
  });
}

/**
 * Alterna a empresa ativa (Sprint 8.4). Sem logout/login: ao concluir, invalida
 * a identidade corrente /me (fonte de `user.companyId`) e as queries escopadas
 * por empresa (tenants/users/roles/permissions/audit), de modo que o novo
 * CurrentUser e o contexto da aplicação reflitam a empresa trocada.
 */
export function useSwitchCompany() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (companyId: string) => AuthService.switchCompany(companyId),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["me"] }),
        queryClient.invalidateQueries({ queryKey: ["me", "companies"] }),
        queryClient.invalidateQueries({ queryKey: ["tenants"] }),
        queryClient.invalidateQueries({ queryKey: ["users"] }),
        queryClient.invalidateQueries({ queryKey: ["roles"] }),
        queryClient.invalidateQueries({ queryKey: ["permissions"] }),
        queryClient.invalidateQueries({ queryKey: ["audit"] }),
      ]);
      toast.success("Empresa ativa alterada.");
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      const message = error.response?.data?.message || "Erro ao alternar empresa";
      toast.error(message);
    },
  });
}
