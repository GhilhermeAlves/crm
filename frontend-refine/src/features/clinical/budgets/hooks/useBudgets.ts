import { useMutation, useQuery } from "@tanstack/react-query";
import { useMutationDefaults } from "@/lib/query/mutation-utils";
import { procedureKeys } from "@/features/clinical/procedures/hooks/useProcedures";
import { budgetService } from "../services/budget.service";
import type { ApproveBudgetRequest, BudgetDetail, BudgetRequest } from "../types/budget.types";

export const budgetKeys = {
  all: (companyId: string, contactId: string) => ["patient-budgets", companyId, contactId],
};

export function useBudgets(companyId: string, contactId: string) {
  return useQuery({
    queryKey: budgetKeys.all(companyId, contactId),
    queryFn: () => budgetService.list(companyId, contactId),
    enabled: Boolean(companyId && contactId),
  });
}

export function useBudget(companyId: string, contactId: string, budgetId: string | null) {
  return useQuery({
    queryKey: [...budgetKeys.all(companyId, contactId), budgetId],
    queryFn: () => budgetService.get(companyId, contactId, budgetId as string),
    enabled: Boolean(companyId && contactId && budgetId),
  });
}

// A chave raiz real do financeiro é ["finance", companyId] (ver useFinance.ts).
const invalidate = (companyId: string, contactId: string) => [
  budgetKeys.all(companyId, contactId),
  procedureKeys.all(companyId, contactId),
  ["finance", companyId],
];

export function useCreateBudget(companyId: string, contactId: string) {
  const { onSuccess, onError } = useMutationDefaults<BudgetDetail, BudgetRequest>();
  return useMutation({
    mutationFn: (payload: BudgetRequest) => budgetService.create(companyId, contactId, payload),
    onSuccess: onSuccess({
      successMessage: "Orçamento salvo",
      invalidateKeys: invalidate(companyId, contactId),
    }),
    onError: onError({ errorMessage: "Não foi possível salvar o orçamento" }),
  });
}

type UpdateVars = { budgetId: string; payload: BudgetRequest };

export function useUpdateBudget(companyId: string, contactId: string) {
  const { onSuccess, onError } = useMutationDefaults<BudgetDetail, UpdateVars>();
  return useMutation({
    mutationFn: ({ budgetId, payload }: UpdateVars) =>
      budgetService.update(companyId, contactId, budgetId, payload),
    onSuccess: onSuccess({
      successMessage: "Orçamento salvo",
      invalidateKeys: invalidate(companyId, contactId),
    }),
    onError: onError({ errorMessage: "Não foi possível salvar o orçamento" }),
  });
}

type ApproveVars = { budgetId: string; payload: ApproveBudgetRequest };

export function useApproveBudget(companyId: string, contactId: string) {
  const { onSuccess, onError } = useMutationDefaults<BudgetDetail, ApproveVars>();
  return useMutation({
    mutationFn: ({ budgetId, payload }: ApproveVars) =>
      budgetService.approve(companyId, contactId, budgetId, payload),
    onSuccess: onSuccess({
      successMessage: "Orçamento aprovado",
      invalidateKeys: invalidate(companyId, contactId),
    }),
    onError: onError({ errorMessage: "Não foi possível aprovar o orçamento" }),
  });
}

export function useLoseBudget(companyId: string, contactId: string) {
  const { onSuccess, onError } = useMutationDefaults<BudgetDetail, string>();
  return useMutation({
    mutationFn: (budgetId: string) => budgetService.lose(companyId, contactId, budgetId),
    onSuccess: onSuccess({
      successMessage: "Orçamento marcado como perdido",
      invalidateKeys: invalidate(companyId, contactId),
    }),
    onError: onError({ errorMessage: "Não foi possível marcar o orçamento como perdido" }),
  });
}

export function useDeleteBudget(companyId: string, contactId: string) {
  const { onSuccess, onError } = useMutationDefaults<void, string>();
  return useMutation({
    mutationFn: (budgetId: string) => budgetService.delete(companyId, contactId, budgetId),
    onSuccess: onSuccess({
      successMessage: "Orçamento excluído",
      invalidateKeys: invalidate(companyId, contactId),
    }),
    onError: onError({ errorMessage: "Não foi possível excluir o orçamento" }),
  });
}
