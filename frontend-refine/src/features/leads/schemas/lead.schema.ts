import { z } from "zod";
import type { LeadClassification, LeadSource, LeadStatus } from "../types/lead.types";
import { LEAD_STATUSES, LEAD_SOURCES, LEAD_CLASSIFICATIONS } from "../types/lead.types";

export const leadStatusLabels: Record<LeadStatus, string> = {
  NEW: "Novo",
  CONTACTED: "Contatado",
  QUALIFIED: "Qualificado",
  UNQUALIFIED: "Não qualificado",
  CONVERTED: "Convertido",
  LOST: "Perdido",
};

export const leadSourceLabels: Record<LeadSource, string> = {
  WHATSAPP: "WhatsApp",
  FORM: "Formulário",
  API: "API",
  IMPORT: "Importação",
  MANUAL: "Manual",
};

export const leadClassificationLabels: Record<LeadClassification, string> = {
  HOT: "Quente",
  WARM: "Morno",
  COLD: "Frio",
  DISQUALIFIED: "Desqualificado",
};

export const leadStatuses = LEAD_STATUSES;
export const leadSources = LEAD_SOURCES;
export const leadClassifications = LEAD_CLASSIFICATIONS;

/**
 * Valores do formulário. Score/classificação são mantidos como string/string
 * (vazias) no formulário; a conversão para o DTO acontece nas páginas.
 */
export const leadFormSchema = z.object({
  contactId: z.string().min(1, "Contato é obrigatório").uuid("Contato inválido (UUID)"),
  status: z.enum(leadStatuses),
  source: z.enum(leadSources),
  score: z.string().refine(
    (val) => {
      const n = Number(val);
      return !isNaN(n) && n >= 0 && n <= 100;
    },
    { message: "Score deve ser um número entre 0 e 100" },
  ),
  classification: z.enum(LEAD_CLASSIFICATIONS).or(z.literal("")),
  assignedTo: z.string(),
  notes: z.string().optional(),
});

export type LeadFormValues = z.infer<typeof leadFormSchema>;
