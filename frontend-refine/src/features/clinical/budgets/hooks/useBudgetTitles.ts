import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import api from "@/lib/api";
import { useMutationDefaults } from "@/lib/query/mutation-utils";

export type BudgetTitle = { id: string; title: string };

const budgetTitleKeys = {
  all: (companyId: string) => ["budget-titles", companyId] as const,
};

const base = (companyId: string) => `/companies/${companyId}/budget-titles`;

/** Títulos de orçamento salvos da empresa, oferecidos no formulário do orçamento. */
export function useBudgetTitles(companyId: string) {
  return useQuery({
    queryKey: budgetTitleKeys.all(companyId),
    queryFn: async () => (await api.get<BudgetTitle[]>(base(companyId))).data,
    enabled: Boolean(companyId),
  });
}

export function useCreateBudgetTitle(companyId: string) {
  const queryClient = useQueryClient();
  const { onError } = useMutationDefaults();
  return useMutation({
    mutationFn: async (title: string) =>
      (await api.post<BudgetTitle>(base(companyId), { title })).data,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: budgetTitleKeys.all(companyId) }),
    onError: onError({ errorMessage: "Erro ao salvar título" }),
  });
}

export function useDeleteBudgetTitle(companyId: string) {
  const queryClient = useQueryClient();
  const { onError } = useMutationDefaults();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`${base(companyId)}/${id}`);
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: budgetTitleKeys.all(companyId) }),
    onError: onError({ errorMessage: "Erro ao remover título" }),
  });
}
