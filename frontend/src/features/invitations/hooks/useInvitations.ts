import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { InvitationService } from "../services/invitation.service";
import type { CreateInvitationRequest } from "../types/invitation.types";

export function useInvitations(companyId: string | null) {
  return useQuery({
    queryKey: ["invitations", companyId],
    queryFn: () => InvitationService.list(companyId as string),
    enabled: !!companyId,
  });
}

export function useCreateInvitation(companyId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateInvitationRequest) =>
      InvitationService.create(companyId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["invitations", companyId] });
      toast.success("Convite criado com sucesso");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao criar convite");
    },
  });
}

export function useRevokeInvitation(companyId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (invitationId: string) =>
      InvitationService.revoke(companyId, invitationId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["invitations", companyId] });
      toast.success("Convite revogado com sucesso");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao revogar convite");
    },
  });
}
