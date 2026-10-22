import { useMutation, useQuery } from "@tanstack/react-query";
import { useMutationDefaults } from "@/lib/query/mutation-utils";
import { HolidayService } from "../services/holiday.service";
import type { CreateHolidayRequest, UpdateHolidayRequest } from "../types/holiday.types";

/**
 * Feriados da clínica. Com from/to ("yyyy-MM-dd"), o backend devolve as ocorrências no
 * período, já projetando os feriados anuais; sem período, devolve o cadastro.
 */
export function useHolidays(companyId: string | null, from?: string, to?: string) {
  return useQuery({
    queryKey: ["holidays", companyId, from ?? null, to ?? null],
    queryFn: () => HolidayService.list(companyId as string, from, to),
    enabled: !!companyId,
  });
}

const invalidate = (companyId: string | null) => [["holidays", companyId]];

export function useCreateHoliday(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (data: CreateHolidayRequest) => HolidayService.create(companyId as string, data),
    onSuccess: onSuccess({
      successMessage: "Feriado cadastrado",
      invalidateKeys: invalidate(companyId),
    }),
    onError: onError({ errorMessage: "Erro ao cadastrar feriado" }),
  });
}

export function useUpdateHoliday(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateHolidayRequest }) =>
      HolidayService.update(companyId as string, id, data),
    onSuccess: onSuccess({
      successMessage: "Feriado atualizado",
      invalidateKeys: invalidate(companyId),
    }),
    onError: onError({ errorMessage: "Erro ao atualizar feriado" }),
  });
}

export function useDeleteHoliday(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (id: string) => HolidayService.delete(companyId as string, id),
    onSuccess: onSuccess({
      successMessage: "Feriado excluído",
      invalidateKeys: invalidate(companyId),
    }),
    onError: onError({ errorMessage: "Erro ao excluir feriado" }),
  });
}
