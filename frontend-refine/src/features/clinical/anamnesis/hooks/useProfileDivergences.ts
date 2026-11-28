import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { profileDivergenceService } from "../services/profile-divergence.service";

export const profileDivergenceKeys = {
  list: (companyId: string, contactId: string) =>
    ["profile-divergences", companyId, contactId] as const,
};

export function useProfileDivergences(companyId: string, contactId: string) {
  return useQuery({
    queryKey: profileDivergenceKeys.list(companyId, contactId),
    queryFn: () => profileDivergenceService.list(companyId, contactId),
    enabled: Boolean(companyId && contactId),
  });
}

export function useResolveDivergence(companyId: string, contactId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, action }: { id: string; action: "accept" | "dismiss" }) =>
      profileDivergenceService.resolve(companyId, contactId, id, action),
    onSuccess: (_data, { action }) => {
      // aceitar muda o cadastro: atualiza tudo que mostra o paciente
      queryClient.invalidateQueries({
        predicate: (query) => query.queryKey.some((part) => part === contactId),
      });
      toast.success(action === "accept" ? "Cadastro atualizado" : "Divergência ignorada");
    },
    onError: (error: unknown) => {
      const message = (error as { response?: { data?: { message?: string } } })?.response?.data
        ?.message;
      toast.error(message || "Não foi possível resolver a divergência");
    },
  });
}
