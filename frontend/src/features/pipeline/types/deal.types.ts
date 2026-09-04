export type DealGroup = "active" | "won";

export type DealStage =
  "Novo" | "Descoberta" | "Proposta" | "Negociação" | "Fechado/Ganho" | "Perdido";

export type Deal = {
  id: string;
  name: string;
  stage: DealStage;
  value: number | null;
  contact: string;
  expectedCloseDate: string | null;
  probability: number;
  expectedValue: number | null;
  forecastCategory: string | null;
  group: DealGroup;
  responsible: string | null;
  tasks: string | null;
  schedule: string | null;
  lastInteraction: string | null;
  quotesInvoices: string | null;
};

export type DealFormValues = {
  name: string;
  stage: DealStage;
  responsible: string;
  value: string;
  contact: string;
  expectedCloseDate: string;
  probability: string;
  expectedValue: string;
  forecastCategory: string;
};
