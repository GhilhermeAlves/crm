"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { AuthService } from "../services/auth.service";
import { TokenManager } from "@/store/token-manager";
import type {
  LoginRequest,
  RegisterRequest,
  ForgotPasswordRequest,
  ResetPasswordRequest,
} from "../types/auth.types";

export function useLogin() {
  const router = useRouter();

  return useMutation({
    mutationFn: (data: LoginRequest) => AuthService.login(data),
    onSuccess: (response) => {
      TokenManager.setTokens(response.accessToken, response.refreshToken);
      toast.success("Login realizado com sucesso");
      router.push("/dashboard");
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      const message = error.response?.data?.message || "Credenciais inválidas";
      toast.error(message);
    },
  });
}

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

export function useLogout() {
  const router = useRouter();

  return useMutation({
    mutationFn: () => AuthService.logout(),
    onSuccess: () => {
      TokenManager.clearTokens();
      toast.success("Logout realizado");
      router.push("/login");
    },
    onError: () => {
      TokenManager.clearTokens();
      toast.success("Logout realizado");
      router.push("/login");
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

export function useMe() {
  return useQuery({
    queryKey: ["me"],
    queryFn: () => AuthService.me(),
    enabled: typeof window !== "undefined" && TokenManager.hasTokens(),
    retry: false,
    staleTime: 5 * 60 * 1000,
  });
}
