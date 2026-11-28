import type { AnamnesisQuestionType } from "../types/anamnesis.types";

/** Vínculo de uma pergunta com o cadastro da pessoa ou com o exame tricológico. */
export type AnamnesisBinding =
  | "PERSON_NAME"
  | "PERSON_BIRTH_DATE"
  | "PERSON_SEX"
  | "PERSON_CPF"
  | "PERSON_MOBILE"
  | "PERSON_EMAIL"
  | "PERSON_ADDRESS"
  | "TRICHO_SEX"
  | "TRICHO_PATTERN"
  | "TRICHO_ONSET"
  | "TRICHO_DAILY_SHEDDING"
  | "TRICHO_FAMILY_HISTORY"
  | "TRICHO_RECENT_TRIGGERS"
  | "TRICHO_POSTPARTUM"
  | "TRICHO_SCALP_SYMPTOMS"
  | "TRICHO_HAIR_PROCEDURES"
  | "TRICHO_ACNE_HIRSUTISM"
  | "TRICHO_IRREGULAR_CYCLES"
  | "TRICHO_ANABOLIC_USE"
  | "TRICHO_CHIEF_COMPLAINT";

export type BindingCode = { value: string; label: string };

export type BindingInfo = {
  value: AnamnesisBinding;
  label: string;
  group: "PERSON" | "TRICHO";
  types: AnamnesisQuestionType[];
  codes?: BindingCode[];
};

const YES_NO: AnamnesisQuestionType[] = ["YES_NO", "YES_NO_WITH_TEXT"];
const SEX_CODES: BindingCode[] = [
  { value: "F", label: "Feminino" },
  { value: "M", label: "Masculino" },
];

export const BINDINGS: BindingInfo[] = [
  { value: "PERSON_NAME", label: "Nome", group: "PERSON", types: ["SHORT_TEXT"] },
  { value: "PERSON_BIRTH_DATE", label: "Data de nascimento", group: "PERSON", types: ["DATE"] },
  {
    value: "PERSON_SEX",
    label: "Sexo",
    group: "PERSON",
    types: ["SINGLE_SELECT"],
    codes: SEX_CODES,
  },
  { value: "PERSON_CPF", label: "CPF", group: "PERSON", types: ["SHORT_TEXT"] },
  { value: "PERSON_MOBILE", label: "Celular", group: "PERSON", types: ["SHORT_TEXT"] },
  { value: "PERSON_EMAIL", label: "E-mail", group: "PERSON", types: ["SHORT_TEXT"] },
  {
    value: "PERSON_ADDRESS",
    label: "Endereço",
    group: "PERSON",
    types: ["SHORT_TEXT", "LONG_TEXT"],
  },
  {
    value: "TRICHO_SEX",
    label: "Sexo (exame)",
    group: "TRICHO",
    types: ["SINGLE_SELECT"],
    codes: SEX_CODES,
  },
  {
    value: "TRICHO_PATTERN",
    label: "Padrão da queda",
    group: "TRICHO",
    types: ["SINGLE_SELECT", "MULTI_SELECT"],
    codes: [
      { value: "DIFFUSE_SHEDDING", label: "Queda espalhada pela cabeça" },
      { value: "THINNING_TOP", label: "Afinamento no topo, entradas ou coroa" },
      { value: "PATCHES", label: "Falhas arredondadas" },
      { value: "SCALP_SYMPTOMS", label: "Principalmente coceira, ardor ou descamação" },
      { value: "BREAKAGE", label: "Fios quebrando no comprimento" },
    ],
  },
  {
    value: "TRICHO_ONSET",
    label: "Início",
    group: "TRICHO",
    types: ["SINGLE_SELECT"],
    codes: [
      { value: "LT_3M", label: "Menos de 3 meses" },
      { value: "3_12M", label: "De 3 a 12 meses" },
      { value: "GT_1Y", label: "Mais de 1 ano" },
    ],
  },
  {
    value: "TRICHO_DAILY_SHEDDING",
    label: "Queda diária",
    group: "TRICHO",
    types: ["SINGLE_SELECT"],
    codes: [
      { value: "LT_50", label: "Menos de 50" },
      { value: "50_100", label: "Entre 50 e 100" },
      { value: "GT_100", label: "Mais de 100" },
    ],
  },
  { value: "TRICHO_FAMILY_HISTORY", label: "Histórico familiar", group: "TRICHO", types: YES_NO },
  {
    value: "TRICHO_RECENT_TRIGGERS",
    label: "Gatilhos recentes",
    group: "TRICHO",
    types: ["MULTI_SELECT"],
    codes: [
      { value: "FEVER_INFECTION", label: "Febre alta, COVID ou infecção" },
      { value: "SURGERY", label: "Cirurgia" },
      { value: "WEIGHT_LOSS", label: "Perda de peso rápida ou dieta restritiva" },
      { value: "SEVERE_STRESS", label: "Estresse intenso" },
      { value: "NEW_MEDICATION", label: "Remédio novo" },
      { value: "NONE", label: "Nenhum" },
    ],
  },
  { value: "TRICHO_POSTPARTUM", label: "Pós-parto", group: "TRICHO", types: YES_NO },
  {
    value: "TRICHO_SCALP_SYMPTOMS",
    label: "Sintomas do couro cabeludo",
    group: "TRICHO",
    types: ["MULTI_SELECT"],
    codes: [
      { value: "ITCH", label: "Coceira" },
      { value: "BURNING", label: "Ardor" },
      { value: "PAIN", label: "Dor" },
      { value: "OILINESS", label: "Oleosidade" },
      { value: "FLAKING", label: "Descamação" },
      { value: "NONE", label: "Nada" },
    ],
  },
  {
    value: "TRICHO_HAIR_PROCEDURES",
    label: "Procedimentos no cabelo",
    group: "TRICHO",
    types: ["MULTI_SELECT"],
    codes: [
      { value: "CHEMICAL", label: "Química/alisamento" },
      { value: "HEAT", label: "Chapinha ou secador quase todo dia" },
      { value: "TIGHT_STYLES", label: "Penteados presos/apertados" },
      { value: "NONE", label: "Nenhum" },
    ],
  },
  { value: "TRICHO_ACNE_HIRSUTISM", label: "Acne ou hirsutismo", group: "TRICHO", types: YES_NO },
  { value: "TRICHO_IRREGULAR_CYCLES", label: "Ciclo irregular", group: "TRICHO", types: YES_NO },
  { value: "TRICHO_ANABOLIC_USE", label: "Uso de anabolizantes", group: "TRICHO", types: YES_NO },
  {
    value: "TRICHO_CHIEF_COMPLAINT",
    label: "Queixa principal",
    group: "TRICHO",
    types: ["SHORT_TEXT", "LONG_TEXT"],
  },
];

export const BINDING_GROUP_LABELS: Record<BindingInfo["group"], string> = {
  PERSON: "Cadastro",
  TRICHO: "Exame tricológico",
};

export function bindingInfo(binding: AnamnesisBinding | null | undefined): BindingInfo | undefined {
  return binding ? BINDINGS.find((b) => b.value === binding) : undefined;
}

/** Vínculos compatíveis com o tipo da pergunta; os do exame só com o módulo Tricologia. */
export function bindingsFor(
  type: AnamnesisQuestionType,
  trichologyEnabled: boolean,
): BindingInfo[] {
  return BINDINGS.filter(
    (b) => b.types.includes(type) && (trichologyEnabled || b.group === "PERSON"),
  );
}

/** Vínculos já usados no modelo. */
export function usedBindings(
  sections: { questions: { binding?: AnamnesisBinding | null }[] }[],
): Set<AnamnesisBinding> {
  const used = new Set<AnamnesisBinding>();
  sections.forEach((s) => s.questions.forEach((q) => q.binding && used.add(q.binding)));
  return used;
}

/** Valores de opção fora dos códigos esperados pelo vínculo. */
export function invalidOptionValues(
  binding: AnamnesisBinding | null | undefined,
  values: string[],
): string[] {
  const info = bindingInfo(binding);
  if (!info?.codes) return [];
  const allowed = new Set(info.codes.map((c) => c.value));
  return values.filter((v) => v && !allowed.has(v));
}
