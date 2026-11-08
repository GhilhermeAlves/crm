"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMutationDefaults } from "@/lib/query/mutation-utils";
import { BillingService } from "./service";
import type { BillingView } from "./types";

export const BILLING_KEYS = {
  my: ["billing", "my"] as const,
  myInvoices: ["billing", "my", "invoices"] as const,
  admin: (id: string) => ["billing", "admin", id] as const,
  adminInvoices: (id: string) => ["billing", "admin", id, "invoices"] as const,
  statuses: ["billing", "statuses"] as const,
};

export function useMyBilling(enabled = true) {
  return useQuery({
    queryKey: BILLING_KEYS.my,
    queryFn: BillingService.my,
    enabled,
    staleTime: 5 * 60 * 1000,
    retry: false,
  });
}

export function useMyInvoices() {
  return useQuery({ queryKey: BILLING_KEYS.myInvoices, queryFn: BillingService.myInvoices });
}

/** "Já paguei": consulta o Asaas na hora; se liberou, recarrega a aplicação no início. */
export function useCheckPayment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: BillingService.checkPayment,
    onSuccess: (view: BillingView) => {
      queryClient.setQueryData(BILLING_KEYS.my, view);
      queryClient.invalidateQueries({ queryKey: BILLING_KEYS.myInvoices });
    },
  });
}

export function useAdminBilling(companyId: string) {
  return useQuery({
    queryKey: BILLING_KEYS.admin(companyId),
    queryFn: () => BillingService.adminView(companyId),
    enabled: Boolean(companyId),
  });
}

export function useAdminInvoices(companyId: string) {
  return useQuery({
    queryKey: BILLING_KEYS.adminInvoices(companyId),
    queryFn: () => BillingService.adminInvoices(companyId),
    enabled: Boolean(companyId),
  });
}

function useAdminMutation<TVars>(
  companyId: string,
  fn: (vars: TVars) => Promise<BillingView>,
  successMessage: string,
  errorMessage: string,
) {
  const { onSuccess, onError } = useMutationDefaults<BillingView, TVars>();
  return useMutation({
    mutationFn: fn,
    onSuccess: onSuccess({
      successMessage,
      invalidateKeys: [[...BILLING_KEYS.admin(companyId)], [...BILLING_KEYS.statuses]],
    }),
    onError: onError({ errorMessage }),
  });
}

export function useActivateBilling(companyId: string) {
  return useAdminMutation<void>(
    companyId,
    () => BillingService.activate(companyId),
    "Cobrança enviada ao Asaas",
    "Não foi possível ativar a cobrança",
  );
}

export function useExemptBilling(companyId: string) {
  return useAdminMutation<void>(
    companyId,
    () => BillingService.exempt(companyId),
    "Empresa marcada como isenta",
    "Não foi possível isentar a empresa",
  );
}

export function useChangeBillingPrice(companyId: string) {
  return useAdminMutation<number>(
    companyId,
    (cents) => BillingService.changePrice(companyId, cents),
    "Valor mensal atualizado",
    "Não foi possível alterar o valor",
  );
}

export function useGrantBilling(companyId: string) {
  return useAdminMutation<number>(
    companyId,
    (days) => BillingService.grant(companyId, days),
    "Empresa liberada",
    "Não foi possível liberar a empresa",
  );
}

export function useBillingStatuses(enabled: boolean) {
  return useQuery({
    queryKey: BILLING_KEYS.statuses,
    queryFn: BillingService.statuses,
    enabled,
    staleTime: 60 * 1000,
  });
}
