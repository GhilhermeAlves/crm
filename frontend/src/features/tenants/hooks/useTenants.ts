"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { TenantService } from "../services/tenant.service";
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
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateTenantRequest) => TenantService.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEY] });
      toast.success("Empresa criada com sucesso");
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      const message = error.response?.data?.message || "Erro ao criar empresa";
      toast.error(message);
    },
  });
}

export function useUpdateTenant() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateTenantRequest }) =>
      TenantService.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEY] });
      toast.success("Empresa atualizada com sucesso");
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      const message =
        error.response?.data?.message || "Erro ao atualizar empresa";
      toast.error(message);
    },
  });
}

export function useDeleteTenant() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => TenantService.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEY] });
      toast.success("Empresa excluída com sucesso");
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      const message =
        error.response?.data?.message || "Erro ao excluir empresa";
      toast.error(message);
    },
  });
}
