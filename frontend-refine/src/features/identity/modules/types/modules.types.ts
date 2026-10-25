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
  | "STORAGE";

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
];

export const SEGMENT_OPTIONS: { value: CompanySegment; label: string }[] = [
  { value: "GENERIC", label: "Genérico" },
  { value: "DENTAL", label: "Odontologia / Saúde" },
  { value: "OCCUPATIONAL_SAFETY", label: "Segurança do trabalho" },
  { value: "TRICHOLOGY", label: "Tricologia / Medicina capilar" },
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
  ],
  OCCUPATIONAL_SAFETY: ["PATIENTS", "AGENDA", "FINANCE", "TASKS", "CATALOG", "REPORTS", "STORAGE"],
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
