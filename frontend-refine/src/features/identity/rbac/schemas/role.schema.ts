import { z } from "zod";

export const roleSchema = z.object({
  name: z
    .string()
    .min(1, "Nome é obrigatório")
    .max(50, "Nome deve ter no máximo 50 caracteres")
    .regex(/^[A-Z_]+$/, "Nome deve conter apenas letras maiúsculas e underscores"),
  description: z.string().max(500, "Descrição deve ter no máximo 500 caracteres").optional(),
  permissionIds: z.array(z.string()).optional(),
});

export const updateRoleSchema = z.object({
  description: z.string().max(500, "Descrição deve ter no máximo 500 caracteres").optional(),
  isActive: z.boolean().optional(),
});

export type RoleFormData = z.infer<typeof roleSchema>;
export type UpdateRoleFormData = z.infer<typeof updateRoleSchema>;

export const roleModuleName: Record<string, string> = {
  identity: "Identidade e acesso",
  dashboard: "Painel",
  leads: "Leads",
  contacts: "Contatos e pacientes",
  pipeline: "Funil",
  chat: "Chat",
  campaigns: "Campanhas",
  campaign: "Campanhas",
  reports: "Relatórios",
  settings: "Configurações",
  activities: "Atividades",
  tasks: "Tarefas",
  agenda: "Agenda",
  ai: "Inteligência artificial",
  anamnesis: "Anamnese",
  audit: "Auditoria",
  catalog: "Catálogo",
  crm: "CRM",
  finance: "Financeiro",
  inventory: "Estoque",
  notifications: "Notificações",
  omnichannel: "Atendimento (omnichannel)",
  template: "Modelos de mensagem",
  workflow: "Automações",
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
  manage: "Gerenciar",
  execute: "Executar",
  move: "Mover",
  win: "Ganhar",
  lose: "Perder",
  configure: "Configurar",
  takeover: "Assumir",
  suggest: "Sugerir",
  chat: "Conversar",
  operational: "Operacional",
};
