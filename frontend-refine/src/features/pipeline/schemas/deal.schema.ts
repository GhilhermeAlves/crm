import { z } from "zod";
import type { DealFormValues, DealStage } from "../types/deal.types";

export const dealGroups: { value: string; label: string }[] = [
  { value: "active", label: "Oportunidades Ativas" },
  { value: "won", label: "Fechado/Ganho" },
];

export const dealStages = [
  "Novo",
  "Descoberta",
  "Proposta",
  "Negociação",
  "Fechado/Ganho",
  "Perdido",
] as const satisfies readonly DealStage[];

export const dealStageLabels: Record<DealStage, string> = {
  Novo: "Novo",
  Descoberta: "Descoberta",
  Proposta: "Proposta",
  Negociação: "Negociação",
  "Fechado/Ganho": "Fechado/Ganho",
  Perdido: "Perdido",
};

export const forecastCategories = ["Melhor cenário", "Comprometido", "Pipeline"] as const;

export const formatCurrency = (value: number | null | undefined): string =>
  value == null
    ? "—"
    : new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(value);

export const formatDate = (value: string | null | undefined): string => {
  if (!value) return "—";
  const [y, m, d] = value.split("-");
  if (!y || !m || !d) return value;
  return `${d}/${m}/${y}`;
};

const numberField = (label: string, { min = 0, allowEmpty = false } = {}) =>
  z.string().refine(
    (val) => {
      if (allowEmpty && val.trim() === "") return true;
      const n = Number(val.replace(",", "."));
      return !isNaN(n) && n >= min;
    },
    { message: `${label} inválido` },
  );

const valueField = numberField("Valor", { min: 0, allowEmpty: true });
const probabilityField = z.string().refine(
  (val) => {
    const n = Number(val);
    return !isNaN(n) && n >= 0 && n <= 100;
  },
  { message: "Probabilidade deve ser entre 0 e 100" },
);

export const dealFormSchema = z.object({
  name: z.string().min(1, "Nome da oportunidade é obrigatório"),
  stage: z.enum(dealStages),
  responsible: z.string().optional().default(""),
  value: valueField,
  contact: z.string().min(1, "Contato é obrigatório"),
  expectedCloseDate: z.string().optional().default(""),
  probability: probabilityField,
  expectedValue: valueField,
  forecastCategory: z.string().optional().default(""),
});

export type DealFormSchemaType = z.infer<typeof dealFormSchema>;

export function dealFormDefaultValues(deal?: {
  name?: string;
  stage?: DealStage;
  responsible?: string | null;
  value?: number | null;
  contact?: string;
  expectedCloseDate?: string | null;
  probability?: number;
  expectedValue?: number | null;
  forecastCategory?: string | null;
}): DealFormValues {
  return {
    name: deal?.name ?? "",
    stage: deal?.stage ?? "Novo",
    responsible: deal?.responsible ?? "",
    value: deal?.value != null ? String(deal.value) : "",
    contact: deal?.contact ?? "",
    expectedCloseDate: deal?.expectedCloseDate ?? "",
    probability: deal?.probability != null ? String(deal.probability) : "100",
    expectedValue: deal?.expectedValue != null ? String(deal.expectedValue) : "",
    forecastCategory: deal?.forecastCategory ?? "",
  };
}
