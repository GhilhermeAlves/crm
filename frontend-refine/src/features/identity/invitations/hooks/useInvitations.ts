import { useQuery, useMutation, useQueryClient, type QueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { InvitationService } from "../services/invitation.service";
import { describeManagementError } from "@/features/identity/lib/management-errors";
import type { CreateInvitationRequest } from "../types/invitation.types";

/**
 * Lista de convites. `staleTime: 0` + `refetchOnWindowFocus` vencem o padrão
 * global do projeto (5min / sem refetch) para que a aba Inativos/Pendentes
 * reflita alterações feitas em outra janela — sem polling contínuo.
 */
export function useInvitations(companyId: string | null) {
  return useQuery({
    queryKey: ["invitations", companyId],
    queryFn: () => InvitationService.list(companyId as string),
    enabled: !!companyId,
    staleTime: 0,
    refetchOnWindowFocus: true,
  });
}

export function useCreateInvitation(companyId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateInvitationRequest) => InvitationService.create(companyId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["invitations", companyId] });
      toast.success("Convite criado com sucesso");
    },
    onError: handleInvitationError(queryClient, companyId),
  });
}

export function useRevokeInvitation(companyId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (invitationId: string) => InvitationService.revoke(companyId, invitationId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["invitations", companyId] });
      toast.success("Convite revogado com sucesso");
    },
    onError: handleInvitationError(queryClient, companyId),
  });
}

/**
 * Reenviar (`send=true`) ou copiar link (`send=false`) de um convite.
 * Nos dois casos o backend gera um novo token — o link anterior deixa de
 * valer. A URL só é lida da resposta; o frontend nunca guarda token.
 *
 * Sem toast de sucesso aqui: quem chama decide a mensagem ("Convite reenviado"
 * vs. "Link do convite copiado.").
 */
export function useRegenerateInvitation(companyId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ invitationId, send }: { invitationId: string; send: boolean }) =>
      InvitationService.regenerate(companyId, invitationId, send),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["invitations", companyId] });
    },
    onError: handleInvitationError(queryClient, companyId),
  });
}

function handleInvitationError(queryClient: QueryClient, companyId: string) {
  return (error: unknown) => {
    const described = describeManagementError(error);
    toast.error(described.message);
    if (described.shouldRefetch) {
      void queryClient.invalidateQueries({ queryKey: ["invitations", companyId] });
    }
  };
}
