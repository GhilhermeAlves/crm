"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
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
