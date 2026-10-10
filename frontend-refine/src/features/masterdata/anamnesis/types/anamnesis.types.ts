export type AnamnesisQuestionType =
  | "SHORT_TEXT"
  | "LONG_TEXT"
  | "YES_NO"
  | "YES_NO_WITH_TEXT"
  | "NUMBER"
  | "SINGLE_SELECT"
  | "MULTI_SELECT";

export type AnamnesisComplementTrigger = "YES" | "NO" | "ALWAYS" | "OPTION";

export type AnamnesisOption = {
  id: string;
  label: string;
  value: string;
  sortOrder: number;
};

export type AnamnesisQuestion = {
  id: string;
  text: string;
  type: AnamnesisQuestionType;
  required: boolean;
  highlight: boolean;
  allowComplement: boolean;
  complementLabel: string | null;
  complementTrigger: AnamnesisComplementTrigger;
  complementOptionValue: string | null;
  sortOrder: number;
  options: AnamnesisOption[];
};

export type AnamnesisSection = {
  id: string;
  title: string;
  professional: boolean;
  sortOrder: number;
  questions: AnamnesisQuestion[];
};

export type AnamnesisModel = {
  id: string;
  companyId: string;
  name: string;
  description: string | null;
  active: boolean;
  isDefault: boolean;
  version: number;
  sections: AnamnesisSection[];
  createdAt: string;
  updatedAt: string;
};

export type AnamnesisModelSummary = {
  id: string;
  companyId: string;
  name: string;
  description: string | null;
  active: boolean;
  isDefault: boolean;
  version: number;
  sectionCount: number;
  questionCount: number;
  createdAt: string;
  updatedAt: string;
};

export type AnamnesisOptionRequest = {
  id?: string;
  label: string;
  value?: string;
};

export type AnamnesisQuestionRequest = {
  id?: string;
  text: string;
  type: AnamnesisQuestionType;
  required?: boolean;
  highlight?: boolean;
  allowComplement?: boolean;
  complementLabel?: string | null;
  complementTrigger?: AnamnesisComplementTrigger;
  complementOptionValue?: string | null;
  options?: AnamnesisOptionRequest[];
};

export type AnamnesisSectionRequest = {
  id?: string;
  title: string;
  professional?: boolean;
  questions?: AnamnesisQuestionRequest[];
};

export type CreateAnamnesisModelRequest = {
  name: string;
  description?: string;
};

export type UpdateAnamnesisModelRequest = {
  name: string;
  description?: string;
  sections: AnamnesisSectionRequest[];
};

export const ANAMNESIS_QUESTION_TYPE_LABELS: Record<AnamnesisQuestionType, string> = {
  SHORT_TEXT: "Texto curto",
  LONG_TEXT: "Texto longo",
  YES_NO: "Sim/Não",
  YES_NO_WITH_TEXT: "Sim/Não e texto",
  NUMBER: "Número",
  SINGLE_SELECT: "Seleção única",
  MULTI_SELECT: "Seleção múltipla",
};

export const ANAMNESIS_COMPLEMENT_TRIGGER_LABELS: Record<AnamnesisComplementTrigger, string> = {
  YES: "quando responder “Sim”",
  NO: "quando responder “Não”",
  ALWAYS: "sempre",
  OPTION: "quando uma opção for selecionada",
};

export function isSelectable(type: AnamnesisQuestionType): boolean {
  return type === "SINGLE_SELECT" || type === "MULTI_SELECT";
}

export function countQuestions(sections: AnamnesisSection[]): number {
  return sections.reduce((total, section) => total + section.questions.length, 0);
}
