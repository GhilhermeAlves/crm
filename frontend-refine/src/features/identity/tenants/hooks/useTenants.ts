"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import { useMutationDefaults } from "@/lib/query/mutation-utils";
import { TenantService, CompanyPreferencesService } from "../services/tenant.service";
import type {
  CreateTenantRequest,
  UpdateTenantRequest,
  ListTenantsParams,
} from "../types/tenant.types";

const QUERY_KEY = "tenants";

export function useTenants(params?: ListTenantsParams) {
  return useQuery({
    queryKey: [QUERY_KEY, params],
    queryFn: () => TenantService.list(params),
    retry: false,
  });
}

export function useTenant(id: string) {
  return useQuery({
    queryKey: [QUERY_KEY, id],
    queryFn: () => TenantService.findById(id),
    enabled: !!id,
    retry: false,
  });
}

export function useCreateTenant() {
  const { onSuccess, onError } = useMutationDefaults();

  return useMutation({
    mutationFn: (data: CreateTenantRequest) => TenantService.create(data),
    onSuccess: onSuccess({
      successMessage: "Empresa criada com sucesso",
      invalidateKeys: [[QUERY_KEY]],
    }),
    onError: onError({ errorMessage: "Erro ao criar empresa" }),
  });
}

export function useUpdateTenant() {
  const { onSuccess, onError } = useMutationDefaults();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateTenantRequest }) =>
      TenantService.update(id, data),
    onSuccess: onSuccess({
      successMessage: "Empresa atualizada com sucesso",
      invalidateKeys: [[QUERY_KEY]],
    }),
    onError: onError({ errorMessage: "Erro ao atualizar empresa" }),
  });
}

export function useDeleteTenant() {
  const { onSuccess, onError } = useMutationDefaults();

  return useMutation({
    mutationFn: (id: string) => TenantService.delete(id),
    onSuccess: onSuccess({
      successMessage: "Empresa excluída com sucesso",
      invalidateKeys: [[QUERY_KEY]],
    }),
    onError: onError({ errorMessage: "Erro ao excluir empresa" }),
  });
}

// --- Preferências: regras de cadastro de contato ---

export function useContactRules(companyId: string | null | undefined) {
  return useQuery({
    queryKey: ["contact-rules", companyId],
    queryFn: () => CompanyPreferencesService.getContactRules(companyId as string),
    enabled: !!companyId,
    staleTime: 60_000,
  });
}

export function useSetRequireContactCpf(companyId: string | null | undefined) {
  const { onSuccess, onError } = useMutationDefaults();

  return useMutation({
    mutationFn: (requireContactCpf: boolean) =>
      CompanyPreferencesService.setRequireContactCpf(companyId as string, requireContactCpf),
    onSuccess: onSuccess({
      successMessage: "Preferência salva",
      invalidateKeys: [["contact-rules", companyId]],
    }),
    onError: onError({ errorMessage: "Erro ao salvar preferência" }),
  });
}
