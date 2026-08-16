import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { MemberService } from "../services/member.service";
import { UserService } from "@/features/users/services/user.service";
import type { InviteMemberRequest } from "../types/member.types";

export function useMembers(companyId: string | null) {
  return useQuery({
    queryKey: ["members", companyId],
    queryFn: () => MemberService.listMembers(companyId as string),
    enabled: !!companyId,
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
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao atualizar papel do membro");
    },
  });
}

export function useRemoveMember(companyId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (userId: string) =>
      MemberService.removeMember(companyId, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["members", companyId] });
      queryClient.invalidateQueries({ queryKey: ["users"] });
      toast.success("Membro removido com sucesso");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao remover membro");
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
