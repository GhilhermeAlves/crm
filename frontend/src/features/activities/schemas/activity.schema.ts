import { z } from "zod";
import { useAuthorization } from "@/features/auth/hooks/useAuthorization";

export const ACTIVITY_TYPES = [
  "CALL",
  "MEETING",
  "EMAIL",
  "MESSAGE",
  "NOTE",
  "PROPOSAL",
  "FOLLOW_UP",
  "OTHER",
] as const;

export const createActivitySchema = z.object({
  type: z.enum(ACTIVITY_TYPES, {
    errorMap: () => ({ message: "Selecione o tipo de atividade" }),
  }),
  subject: z
    .string()
    .min(1, "Assunto é obrigatório")
    .max(255, "Assunto deve ter no máximo 255 caracteres"),
  description: z.string().max(2000, "Descrição muito longa").optional(),
  activityAt: z.string().optional(),
  contactId: z.string().uuid("Contato inválido").optional(),
  opportunityId: z.string().uuid("Oportunidade inválida").optional(),
});

export type CreateActivityFormValues = z.infer<typeof createActivitySchema>;

export function useActivityPermissions() {
  const { can } = useAuthorization();
  return {
    canCreate: can("activity:create"),
    canUpdate: can("activity:update"),
    canDelete: can("activity:delete"),
    canRead: can("activity:read"),
  };
}