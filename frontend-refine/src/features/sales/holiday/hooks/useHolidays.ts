import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { HolidayService } from "../services/holiday.service";
import type { CreateHolidayRequest, UpdateHolidayRequest } from "../types/holiday.types";

export function useHolidays(companyId: string | null, from?: string, to?: string) {
  return useQuery({
    queryKey: ["company-holidays", companyId, from, to],
    queryFn: () => HolidayService.list(companyId!, from, to),
    enabled: !!companyId,
  });
}

export function useCreateHoliday(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateHolidayRequest) => HolidayService.create(companyId!, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["company-holidays", companyId] });
    },
  });
}

export function useUpdateHoliday(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateHolidayRequest }) =>
      HolidayService.update(companyId!, id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["company-holidays", companyId] });
    },
  });
}

export function useDeleteHoliday(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => HolidayService.delete(companyId!, id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["company-holidays", companyId] });
    },
  });
}
