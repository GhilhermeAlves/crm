import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { describeManagementError } from "@/features/identity/lib/management-errors";
import { CompanyInvitationService } from "./service";
import type { CreateCompanyInvitationRequest } from "./types";

const KEY = ["company-invitations"];

function showError(error: unknown) {
  toast.error(describeManagementError(error).message);
}

/** Convites de criação de empresa enviados pelo CRM_ADMIN. */
export function useCompanyInvitations(enabled: boolean) {
  return useQuery({
    queryKey: KEY,
    queryFn: () => CompanyInvitationService.list(),
    enabled,
    staleTime: 0,
  });
}

export function useCreateCompanyInvitation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateCompanyInvitationRequest) => CompanyInvitationService.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: KEY });
      toast.success("Convite de empresa criado");
    },
    onError: showError,
  });
}

export function useRevokeCompanyInvitation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => CompanyInvitationService.revoke(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: KEY });
      toast.success("Convite revogado");
    },
    onError: showError,
  });
}

export function useRegenerateCompanyInvitation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, send }: { id: string; send: boolean }) =>
      CompanyInvitationService.regenerate(id, send),
    onSuccess: (_data, { send }) => {
      queryClient.invalidateQueries({ queryKey: KEY });
      if (send) toast.success("Novo link enviado por e-mail");
    },
    onError: showError,
  });
}
