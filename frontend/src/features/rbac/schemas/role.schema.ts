import { z } from "zod";

export const roleSchema = z.object({
  name: z
    .string()
    .min(1, "Nome é obrigatório")
    .max(50, "Nome deve ter no máximo 50 caracteres")
    .regex(/^[A-Z_]+$/, "Nome deve conter apenas letras maiúsculas e underscores"),
  description: z
    .string()
    .max(500, "Descrição deve ter no máximo 500 caracteres")
    .optional(),
});

export const updateRoleSchema = z.object({
  description: z
    .string()
    .max(500, "Descrição deve ter no máximo 500 caracteres")
    .optional(),
  isActive: z.boolean().optional(),
});

export type RoleFormData = z.infer<typeof roleSchema>;
export type UpdateRoleFormData = z.infer<typeof updateRoleSchema>;

export const roleModuleName: Record<string, string> = {
  identity: "Identidade",
  dashboard: "Dashboard",
  leads: "Leads",
  contacts: "Contatos",
  pipeline: "Pipeline",
  chat: "Chat",
  campaigns: "Campanhas",
  reports: "Relatórios",
  settings: "Configurações",
};

export const actionName: Record<string, string> = {
  view: "Visualizar",
  create: "Criar",
  read: "Ler",
  update: "Atualizar",
  delete: "Excluir",
  invite: "Convidar",
  assign: "Atribuir",
  send: "Enviar",
  export: "Exportar",
};
