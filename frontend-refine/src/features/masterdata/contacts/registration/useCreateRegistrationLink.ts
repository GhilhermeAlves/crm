import { useMutation } from "@tanstack/react-query";
import { useMutationDefaults } from "@/lib/query/mutation-utils";
import { RegistrationService } from "./registration.service";

/** Gera um link de cadastro para o paciente (cada geração invalida o anterior). */
export function useCreateRegistrationLink(companyId: string | null, contactId: string | null) {
  const { onError } = useMutationDefaults();
  return useMutation({
    mutationFn: () => RegistrationService.createLink(companyId as string, contactId as string),
    onError: onError({ errorMessage: "Erro ao gerar o link de cadastro" }),
  });
}
