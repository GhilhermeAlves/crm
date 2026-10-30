export type CompanyModuleName =
  | "CRM"
  | "PATIENTS"
  | "AGENDA"
  | "FINANCE"
  | "TASKS"
  | "ASSISTANT"
  | "INBOX"
  | "FOLLOW_UPS"
  | "CAMPAIGNS"
  | "WORKFLOWS"
  | "CATALOG"
  | "INVENTORY"
  | "REPORTS"
  | "STORAGE"
  | "AGENDA_SETTINGS"
  | "EMAIL_SETTINGS"
  | "AI_AGENT"
  | "DOCUMENTS"
  | "SERVICE_LIST"
  | "DATA_MIGRATION"
  | "ROLES"
  | "AUDIT";

export type CompanySegment = "GENERIC" | "DENTAL" | "OCCUPATIONAL_SAFETY" | "TRICHOLOGY";

export type CompanyModules = {
  companyId: string;
  segment: CompanySegment;
  enabledModules: CompanyModuleName[];
};

export type UpdateCompanyModulesRequest = {
  segment: CompanySegment;
  enabledModules: CompanyModuleName[];
};

export const MODULE_OPTIONS: { value: CompanyModuleName; label: string; description: string }[] = [
  { value: "CRM", label: "Comercial (CRM)", description: "Contatos, funil, oportunidades e leads" },
  {
    value: "PATIENTS",
    label: "Pacientes",
    description: "Lista e ficha de pacientes (área da saúde)",
  },
  { value: "AGENDA", label: "Agenda", description: "Agenda, cadeiras, bloqueios e retornos" },
  { value: "FINANCE", label: "Financeiro", description: "Lançamentos e contas a receber/pagar" },
  { value: "TASKS", label: "Tarefas", description: "Tarefas e atividades" },
  { value: "ASSISTANT", label: "Léo · Assistente IA", description: "Assistente de IA" },
  { value: "INBOX", label: "Inbox e Canais", description: "Atendimento omnichannel" },
  { value: "FOLLOW_UPS", label: "Follow-ups", description: "Sequências de follow-up" },
  { value: "CAMPAIGNS", label: "Campanhas", description: "Campanhas de mensagens" },
  { value: "WORKFLOWS", label: "Automações", description: "Workflows e automações" },
  { value: "CATALOG", label: "Catálogo", description: "Catálogo de serviços e produtos" },
  { value: "INVENTORY", label: "Estoque", description: "Controle de estoque" },
  { value: "REPORTS", label: "Relatórios", description: "Relatórios e exportações" },
  { value: "STORAGE", label: "Arquivos", description: "Arquivos e modelos" },
  {
    value: "AGENDA_SETTINGS",
    label: "Configurações da agenda",
    description: "Ajustes, disponibilidade, cadeiras, feriados e notificações",
  },
  { value: "EMAIL_SETTINGS", label: "E-mail", description: "Conta de e-mail da clínica" },
  {
    value: "AI_AGENT",
    label: "Agente de IA",
    description: "Configuração do agente de atendimento",
  },
  {
    value: "DOCUMENTS",
    label: "Documentos",
    description: "Modelos de documentos, anamnese e receituários",
  },
  { value: "SERVICE_LIST", label: "Lista de serviços", description: "Serviços da empresa" },
  { value: "DATA_MIGRATION", label: "Migração de dados", description: "Importação de dados" },
  { value: "ROLES", label: "Perfis e Permissões", description: "Perfis de acesso da equipe" },
  { value: "AUDIT", label: "Auditoria", description: "Histórico de ações no sistema" },
];

/**
 * Especialidade: a mesma lista nomeia a especialidade da empresa (Empresas,
 * Módulos) e a do profissional convidado (Convites, Usuários).
 */
export const SEGMENT_OPTIONS: { value: CompanySegment; label: string }[] = [
  { value: "GENERIC", label: "Geral" },
  { value: "DENTAL", label: "Odontologia" },
  { value: "OCCUPATIONAL_SAFETY", label: "Tec. Segurança do Trabalho" },
  { value: "TRICHOLOGY", label: "Tricologia" },
];

export const SEGMENT_DEFAULT_MODULES: Record<CompanySegment, CompanyModuleName[]> = {
  GENERIC: [
    "CRM",
    "PATIENTS",
    "AGENDA",
    "FINANCE",
    "TASKS",
    "ASSISTANT",
    "INBOX",
    "FOLLOW_UPS",
    "CAMPAIGNS",
    "WORKFLOWS",
    "CATALOG",
    "INVENTORY",
    "REPORTS",
    "STORAGE",
    "AGENDA_SETTINGS",
    "EMAIL_SETTINGS",
    "AI_AGENT",
    "DOCUMENTS",
    "SERVICE_LIST",
    "DATA_MIGRATION",
    "ROLES",
    "AUDIT",
  ],
  DENTAL: [
    "PATIENTS",
    "AGENDA",
    "FINANCE",
    "TASKS",
    "ASSISTANT",
    "INBOX",
    "CATALOG",
    "REPORTS",
    "STORAGE",
    "AGENDA_SETTINGS",
    "EMAIL_SETTINGS",
    "AI_AGENT",
    "DOCUMENTS",
    "SERVICE_LIST",
    "DATA_MIGRATION",
    "ROLES",
    "AUDIT",
  ],
  TRICHOLOGY: [
    "PATIENTS",
    "AGENDA",
    "FINANCE",
    "TASKS",
    "ASSISTANT",
    "INBOX",
    "CATALOG",
    "REPORTS",
    "STORAGE",
    "AGENDA_SETTINGS",
    "EMAIL_SETTINGS",
    "AI_AGENT",
    "DOCUMENTS",
    "SERVICE_LIST",
    "DATA_MIGRATION",
    "ROLES",
    "AUDIT",
  ],
  OCCUPATIONAL_SAFETY: [
    "PATIENTS",
    "AGENDA",
    "FINANCE",
    "TASKS",
    "CATALOG",
    "REPORTS",
    "STORAGE",
    "AGENDA_SETTINGS",
    "EMAIL_SETTINGS",
    "DOCUMENTS",
    "SERVICE_LIST",
    "DATA_MIGRATION",
    "ROLES",
    "AUDIT",
  ],
};

/** Todos os módulos, na ordem do enum do backend. */
export const ALL_COMPANY_MODULES: CompanyModuleName[] = MODULE_OPTIONS.map(
  (option) => option.value,
);

/** Módulos agrupados como no menu lateral (navigation.ts). */
export const MODULE_GROUPS: { title: string; modules: CompanyModuleName[] }[] = [
  { title: "Principal", modules: ["AGENDA", "PATIENTS", "FINANCE", "TASKS", "ASSISTANT"] },
  { title: "Comercial", modules: ["CRM"] },
  { title: "Atendimento", modules: ["INBOX", "FOLLOW_UPS", "CAMPAIGNS", "WORKFLOWS"] },
  { title: "Operação", modules: ["CATALOG", "INVENTORY"] },
  { title: "Análise", modules: ["REPORTS", "STORAGE"] },
  {
    title: "Configurações",
    modules: [
      "AGENDA_SETTINGS",
      "EMAIL_SETTINGS",
      "AI_AGENT",
      "DOCUMENTS",
      "SERVICE_LIST",
      "DATA_MIGRATION",
      "ROLES",
    ],
  },
  { title: "Sistema", modules: ["AUDIT"] },
];

/**
 * Módulo de cada recurso de permissão (coluna permissions.resource). Recursos fora
 * do mapa (contatos, empresa, perfis...) não dependem de módulo e aparecem sempre.
 */
export const PERMISSION_RESOURCE_MODULES: Record<string, CompanyModuleName> = {
  lead: "CRM",
  opportunity: "CRM",
  pipeline: "CRM",
  activity: "CRM",
  patient: "PATIENTS",
  appointment: "AGENDA",
  finance: "FINANCE",
  task: "TASKS",
  assistant: "ASSISTANT",
  suggestion: "ASSISTANT",
  "agent-config": "ASSISTANT",
  conversation: "INBOX",
  omnichannel: "INBOX",
  template: "INBOX",
  followup: "FOLLOW_UPS",
  "followup-sequence": "FOLLOW_UPS",
  campaign: "CAMPAIGNS",
  workflow: "WORKFLOWS",
  catalog: "CATALOG",
  inventory: "INVENTORY",
  report: "REPORTS",
  analytics: "REPORTS",
};
