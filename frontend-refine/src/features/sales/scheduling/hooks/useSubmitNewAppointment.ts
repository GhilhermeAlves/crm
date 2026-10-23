import { useQueryClient } from "@tanstack/react-query";
import { useUpdateContact } from "@/features/masterdata/contacts/hooks/useContacts";
import type { CreateAppointmentFormValues } from "../schemas/scheduling.schema";
import { useCreateAppointment } from "./useScheduling";

/**
 * Envia o formulário de novo agendamento. Se o celular do paciente foi alterado no
 * formulário (`contactPhone`), atualiza o contato antes e invalida as telas do paciente.
 */
export function useSubmitNewAppointment(companyId: string | null) {
  const queryClient = useQueryClient();
  const createAppointment = useCreateAppointment(companyId);
  const updateContact = useUpdateContact(companyId);

  const submit = async (values: CreateAppointmentFormValues, onSuccess?: () => void) => {
    const { contactPhone, ...rest } = values;
    if (contactPhone && rest.contactId) {
      try {
        await updateContact.mutateAsync({ id: rest.contactId, data: { mobile: contactPhone } });
      } catch {
        return; // o toast de erro já vem do useMutationDefaults
      }
      queryClient.invalidateQueries({ queryKey: ["patient", companyId, rest.contactId] });
      queryClient.invalidateQueries({ queryKey: ["patients", companyId] });
    }
    createAppointment.mutate(
      {
        ...rest,
        startAt: new Date(rest.startAt).toISOString(),
        endAt: new Date(rest.endAt).toISOString(),
      },
      { onSuccess: () => onSuccess?.() },
    );
  };

  return { submit, isPending: createAppointment.isPending || updateContact.isPending };
}
