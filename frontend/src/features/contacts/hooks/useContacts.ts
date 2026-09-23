import { useQuery, useMutation } from "@tanstack/react-query";
import { useMutationDefaults } from "@/lib/query/mutation-utils";
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

function invalidateContactsKeys(companyId: string | null): unknown[][] {
  return [
    ["contacts", companyId],
    ["contact", companyId],
    ["customer360", companyId],
  ];
}

export function useCreateContact(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (data: CreateContactRequest) => ContactService.create(companyId as string, data),
    onSuccess: onSuccess({
      successMessage: "Contato criado",
      invalidateKeys: invalidateContactsKeys(companyId),
    }),
    onError: onError({ errorMessage: "Erro ao criar contato" }),
  });
}

export function useUpdateContact(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateContactRequest }) =>
      ContactService.update(companyId as string, id, data),
    onSuccess: onSuccess({
      successMessage: "Contato atualizado",
      invalidateKeys: invalidateContactsKeys(companyId),
    }),
    onError: onError({ errorMessage: "Erro ao atualizar contato" }),
  });
}

export function useDeleteContact(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (id: string) => ContactService.delete(companyId as string, id),
    onSuccess: onSuccess({
      successMessage: "Contato excluído",
      invalidateKeys: invalidateContactsKeys(companyId),
    }),
    onError: onError({ errorMessage: "Erro ao excluir contato" }),
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
