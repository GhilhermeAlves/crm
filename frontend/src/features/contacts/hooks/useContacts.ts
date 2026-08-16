import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useAuthorization } from "@/features/auth/hooks/useAuthorization";
import { ContactService } from "../services/contact.service";
import type { CreateContactRequest, UpdateContactRequest } from "../types/contact.types";

export function useContacts(companyId: string | null) {
  return useQuery({
    queryKey: ["contacts", companyId],
    queryFn: () => ContactService.list(companyId as string),
    enabled: !!companyId,
  });
}

export function useContact(companyId: string | null, contactId: string | null) {
  return useQuery({
    queryKey: ["contact", companyId, contactId],
    queryFn: () => ContactService.findById(companyId as string, contactId as string),
    enabled: !!companyId && !!contactId,
  });
}

export function useCustomer360(companyId: string | null, contactId: string | null) {
  return useQuery({
    queryKey: ["customer360", companyId, contactId],
    queryFn: () => ContactService.customer360(companyId as string, contactId as string),
    enabled: !!companyId && !!contactId,
  });
}

function invalidateContacts(
  queryClient: ReturnType<typeof useQueryClient>,
  companyId: string | null,
) {
  queryClient.invalidateQueries({ queryKey: ["contacts", companyId] });
  queryClient.invalidateQueries({ queryKey: ["contact", companyId] });
  queryClient.invalidateQueries({ queryKey: ["customer360", companyId] });
}

export function useCreateContact(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateContactRequest) => ContactService.create(companyId as string, data),
    onSuccess: () => {
      invalidateContacts(queryClient, companyId);
      toast.success("Contato criado");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao criar contato");
    },
  });
}

export function useUpdateContact(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateContactRequest }) =>
      ContactService.update(companyId as string, id, data),
    onSuccess: () => {
      invalidateContacts(queryClient, companyId);
      toast.success("Contato atualizado");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao atualizar contato");
    },
  });
}

export function useDeleteContact(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => ContactService.delete(companyId as string, id),
    onSuccess: () => {
      invalidateContacts(queryClient, companyId);
      toast.success("Contato excluído");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao excluir contato");
    },
  });
}

export function useContactPermissions() {
  const { can } = useAuthorization();
  return {
    canCreate: can("contact:create"),
    canUpdate: can("contact:update"),
    canDelete: can("contact:delete"),
  };
}
