import { z } from "zod";
import { useAuthorization } from "@/features/identity/auth/hooks/useAuthorization";

export const createRecallSchema = z.object({
  contactId: z.string().uuid("Paciente é obrigatório"),
  phone: z
    .string()
    .min(1, "Celular é obrigatório")
    .max(20, "Celular deve ter no máximo 20 caracteres"),
  hostId: z.string().uuid("Dentista é obrigatório"),
  tagId: z.string().uuid().optional(),
  dueInAmount: z.coerce
    .number()
    .int("Prazo deve ser um número inteiro")
    .min(1, "Prazo deve ser maior que zero"),
  dueInUnit: z.enum(["DAYS", "WEEKS", "MONTHS", "YEARS"], {
    errorMap: () => ({ message: "Unidade do prazo é obrigatória" }),
  }),
});

export type CreateRecallFormValues = z.infer<typeof createRecallSchema>;

export function useRecallPermissions() {
  const { can } = useAuthorization();
  return {
    canRead: can("appointment:read"),
    canCreate: can("appointment:create"),
    canUpdate: can("appointment:update"),
  };
}
