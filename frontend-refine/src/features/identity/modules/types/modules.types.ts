export type CompanyModuleName = "CRM" | "PATIENTS" | "AGENDA" | "FINANCE";

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
];

export const SEGMENT_OPTIONS: { value: CompanySegment; label: string }[] = [
  { value: "GENERIC", label: "Genérico" },
  { value: "DENTAL", label: "Odontologia / Saúde" },
  { value: "OCCUPATIONAL_SAFETY", label: "Segurança do trabalho" },
  { value: "TRICHOLOGY", label: "Tricologia / Medicina capilar" },
];
