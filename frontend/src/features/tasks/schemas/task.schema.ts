import { z } from "zod";
import { useAuthorization } from "@/features/auth/hooks/useAuthorization";

export const TASK_PRIORITIES = ["LOW", "MEDIUM", "HIGH"] as const;

export const createTaskSchema = z.object({
  title: z
    .string()
    .min(1, "Título é obrigatório")
    .max(200, "Título deve ter no máximo 200 caracteres"),
  description: z.string().max(2000, "Descrição muito longa").optional(),
  dueAt: z.string().optional(),
  priority: z.enum(TASK_PRIORITIES).optional(),
  assigneeId: z.string().uuid("Usuário inválido").optional(),
  contactId: z.string().uuid("Contato inválido").optional(),
  opportunityId: z.string().uuid("Oportunidade inválida").optional(),
});

export type CreateTaskFormValues = z.infer<typeof createTaskSchema>;

export function useTaskPermissions() {
  const { can } = useAuthorization();
  return {
    canCreate: can("task:create"),
    canUpdate: can("task:update"),
    canDelete: can("task:delete"),
    canRead: can("task:read"),
  };
}