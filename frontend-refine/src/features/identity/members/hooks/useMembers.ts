import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { MemberService } from "../services/member.service";
import { UserService } from "@/features/identity/users/services/user.service";
import { describeManagementError } from "@/features/identity/lib/management-errors";
import type { InviteMemberRequest, MemberStatus } from "../types/member.types";

/**
 * Membros da empresa. `status` entra na queryKey para que Ativos e Inativos
 * sejam caches independentes; as mutações invalidam o prefixo ["members", companyId]
 * e alcançam ambos.
 *
 * `staleTime: 0` + `refetchOnWindowFocus` vencem o padrão global do projeto
 * (5min / sem refetch) para refletir alterações feitas em outra janela.
 */
export function useMembers(companyId: string | null, status?: MemberStatus) {
  return useQuery({
    queryKey: ["members", companyId, status ?? "ACTIVE"],
    queryFn: () => MemberService.listMembers(companyId as string, status),
    enabled: !!companyId,
    staleTime: 0,
    refetchOnWindowFocus: true,
  });
}

export function useUpdateMemberRole(companyId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ userId, role }: { userId: string; role: string }) =>
      MemberService.updateRole(companyId, userId, role),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["members", companyId] });
      queryClient.invalidateQueries({ queryKey: ["users"] });
      toast.success("Papel do membro atualizado com sucesso");
    },
    onError: (error: unknown) => {
      const described = describeManagementError(error);
      toast.error(described.message);
      if (described.shouldRefetch) {
        void queryClient.invalidateQueries({ queryKey: ["members", companyId] });
      }
    },
  });
}

export function useRemoveMember(companyId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (userId: string) => MemberService.removeMember(companyId, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["members", companyId] });
      queryClient.invalidateQueries({ queryKey: ["users"] });
      toast.success("Membro desativado com sucesso");
    },
    onError: (error: unknown) => {
      const described = describeManagementError(error);
      toast.error(described.message);
      if (described.shouldRefetch) {
        void queryClient.invalidateQueries({ queryKey: ["members", companyId] });
      }
    },
  });
}

export function useInviteMember() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: InviteMemberRequest) => UserService.invite(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["members"] });
      queryClient.invalidateQueries({ queryKey: ["users"] });
      toast.success("Convite enviado com sucesso");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao enviar convite");
    },
  });
}
