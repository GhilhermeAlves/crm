"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { OnboardingService } from "../services/onboarding.service";
import type { OnboardingCompanyRequest } from "../types/onboarding.types";

/**
 * Cria a primeira empresa no onboarding. Após sucesso, invalida a query `me`
 * para que o AuthProvider refaça o fetch e o usuário passe a ter companyId.
 */
export function useCreateCompany() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: OnboardingCompanyRequest) =>
      OnboardingService.createCompany(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["me"] });
      toast.success("Empresa criada com sucesso. Bem-vindo ao CRM!");
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      const message = error.response?.data?.message || "Erro ao criar empresa";
      toast.error(message);
    },
  });
}
