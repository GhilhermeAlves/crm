import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { FinanceService } from "../services/finance.service";
import type {
  CreateFinancialTransactionRequest,
  ListTransactionsFilters,
  ReportGranularity,
  TransactionFilters,
  UpdateFinancialTransactionRequest,
} from "../types/finance.types";

type RangeParams = {
  from?: string;
  to?: string;
  granularity?: ReportGranularity;
};

function useInvalidateFinance(companyId: string | null) {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: ["finance", companyId] });
}

export function useTransactions(companyId: string | null, filters: ListTransactionsFilters = {}) {
  return useQuery({
    queryKey: ["finance", companyId, "transactions", filters],
    queryFn: () => FinanceService.listTransactions(companyId as string, filters),
    enabled: !!companyId,
  });
}

export function useTransaction(companyId: string | null, id: string | null) {
  return useQuery({
    queryKey: ["finance", companyId, "transaction", id],
    queryFn: () => FinanceService.getTransaction(companyId as string, id as string),
    enabled: !!companyId && !!id,
  });
}

export function useFinanceSummary(companyId: string | null, filters: TransactionFilters = {}) {
  return useQuery({
    queryKey: ["finance", companyId, "summary", filters],
    queryFn: () => FinanceService.getSummary(companyId as string, filters),
    enabled: !!companyId,
  });
}

export function useFinanceHealth(companyId: string | null, months: number) {
  return useQuery({
    queryKey: ["finance", companyId, "health", months],
    queryFn: () => FinanceService.getHealth(companyId as string, months),
    enabled: !!companyId,
  });
}

export function useCashFlow(companyId: string | null, params: RangeParams = {}) {
  return useQuery({
    queryKey: ["finance", companyId, "cash-flow", params],
    queryFn: () => FinanceService.getCashFlow(companyId as string, params),
    enabled: !!companyId,
  });
}

export function useRevenueDistribution(
  companyId: string | null,
  params: RangeParams & { catalogItemIds?: string[] } = {},
) {
  return useQuery({
    queryKey: ["finance", companyId, "revenue-distribution", params],
    queryFn: () => FinanceService.getRevenueDistribution(companyId as string, params),
    enabled: !!companyId,
  });
}

export function useFinanceOverview(
  companyId: string | null,
  params: RangeParams & { professionalUserIds?: string[] } = {},
) {
  return useQuery({
    queryKey: ["finance", companyId, "overview", params],
    queryFn: () => FinanceService.getOverview(companyId as string, params),
    enabled: !!companyId,
  });
}

export function useAgendaReport(
  companyId: string | null,
  params: RangeParams & {
    appointmentType?: string;
    statuses?: string[];
    professionalUserIds?: string[];
  } = {},
) {
  return useQuery({
    queryKey: ["finance", companyId, "agenda-report", params],
    queryFn: () => FinanceService.getAgendaReport(companyId as string, params),
    enabled: !!companyId,
  });
}

export function useHeadline(companyId: string | null) {
  return useQuery({
    queryKey: ["finance", companyId, "headline"],
    queryFn: () => FinanceService.getHeadline(companyId as string),
    enabled: !!companyId,
  });
}

export function useAvailableMonths(companyId: string | null) {
  return useQuery({
    queryKey: ["finance", companyId, "available-months"],
    queryFn: () => FinanceService.getAvailableMonths(companyId as string),
    enabled: !!companyId,
  });
}

export function useCreateTransaction(companyId: string | null) {
  const invalidate = useInvalidateFinance(companyId);
  return useMutation({
    mutationFn: (body: CreateFinancialTransactionRequest) =>
      FinanceService.createTransaction(companyId as string, body),
    onSuccess: () => invalidate(),
    onError: (error: Error) => toast.error(error.message || "Erro ao criar lançamento"),
  });
}

export function useUpdateTransaction(companyId: string | null) {
  const invalidate = useInvalidateFinance(companyId);
  return useMutation({
    mutationFn: ({ id, body }: { id: string; body: UpdateFinancialTransactionRequest }) =>
      FinanceService.updateTransaction(companyId as string, id, body),
    onSuccess: () => invalidate(),
    onError: (error: Error) => toast.error(error.message || "Erro ao atualizar lançamento"),
  });
}

export function usePayTransaction(companyId: string | null) {
  const invalidate = useInvalidateFinance(companyId);
  return useMutation({
    mutationFn: ({ id, paidAt }: { id: string; paidAt: string }) =>
      FinanceService.payTransaction(companyId as string, id, paidAt),
    onSuccess: () => invalidate(),
    onError: (error: Error) => toast.error(error.message || "Erro ao registrar pagamento"),
  });
}

export function useCancelTransaction(companyId: string | null) {
  const invalidate = useInvalidateFinance(companyId);
  return useMutation({
    mutationFn: (id: string) => FinanceService.cancelTransaction(companyId as string, id),
    onSuccess: () => invalidate(),
    onError: (error: Error) => toast.error(error.message || "Erro ao cancelar lançamento"),
  });
}

export function useDeleteTransaction(companyId: string | null) {
  const invalidate = useInvalidateFinance(companyId);
  return useMutation({
    mutationFn: (id: string) => FinanceService.deleteTransaction(companyId as string, id),
    onSuccess: () => invalidate(),
    onError: (error: Error) => toast.error(error.message || "Erro ao excluir lançamento"),
  });
}

export function useImportTransactions(companyId: string | null) {
  const invalidate = useInvalidateFinance(companyId);
  return useMutation({
    mutationFn: (file: File) => FinanceService.importTransactions(companyId as string, file),
    onSuccess: () => invalidate(),
    onError: (error: Error) => toast.error(error.message || "Erro ao importar lançamentos"),
  });
}
