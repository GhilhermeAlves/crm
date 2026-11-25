import type {
  AddressType,
  ContactType,
  ContractType,
  DocumentType,
  EmployeeStatus,
  FieldContext,
  PersonRole,
  RelationshipType,
  SpecialtyCode,
  SupplierType,
} from "../types/people.types";

export const ROLE_LABELS: Record<PersonRole, string> = {
  CUSTOMER: "Cliente",
  PATIENT: "Paciente",
  SUPPLIER: "Fornecedor",
  EMPLOYEE: "Funcionário",
};

export const ADDRESS_TYPE_LABELS: Record<AddressType, string> = {
  RESIDENTIAL: "Residencial",
  COMMERCIAL: "Comercial",
  BILLING: "Cobrança",
  DELIVERY: "Entrega",
  OTHER: "Outro",
};

export const CONTACT_TYPE_LABELS: Record<ContactType, string> = {
  MOBILE: "Celular",
  PHONE: "Telefone",
  EMAIL: "E-mail",
  WEBSITE: "Site",
  OTHER: "Outro",
};

export const DOCUMENT_TYPE_LABELS: Record<DocumentType, string> = {
  RG: "RG",
  CNH: "CNH",
  PASSPORT: "Passaporte",
  CNS: "Cartão SUS (CNS)",
  RNE: "RNE",
  SOCIAL_CONTRACT: "Contrato social",
  OTHER: "Outro",
};

export const RELATIONSHIP_LABELS: Record<RelationshipType, { out: string; in: string }> = {
  GUARDIAN: { out: "Responsável", in: "Responsável por" },
  SPOUSE: { out: "Cônjuge", in: "Cônjuge de" },
  PARTNER: { out: "Sócio", in: "Sócio de" },
  LEGAL_REPRESENTATIVE: { out: "Representante legal", in: "Representante legal de" },
  OTHER: { out: "Outro vínculo", in: "Outro vínculo com" },
};

export const SUPPLIER_TYPE_LABELS: Record<SupplierType, string> = {
  LAB: "Laboratório",
  MATERIAL: "Dental / material",
  SERVICE: "Serviço",
  OTHER: "Outro",
};

export const SPECIALTY_LABELS: Record<SpecialtyCode, string> = {
  GENERIC: "Genérica",
  DENTAL: "Odontologia",
  TRICHOLOGY: "Tricologia",
  OCCUPATIONAL_SAFETY: "Segurança do trabalho",
};

export const CONTRACT_LABELS: Record<ContractType, string> = {
  CLT: "CLT",
  PJ: "PJ",
  INTERN: "Estágio",
  OTHER: "Outro",
};

export const EMPLOYEE_STATUS_LABELS: Record<EmployeeStatus, string> = {
  ACTIVE: "Ativo",
  ON_LEAVE: "Afastado",
  TERMINATED: "Desligado",
};

export const CONTEXT_LABELS: Record<FieldContext, string> = {
  PF: "PF",
  PJ: "PJ",
  CUSTOMER: "Cliente",
  SUPPLIER: "Fornec.",
  EMPLOYEE: "Funcion.",
};

export const FIELD_GROUP_LABELS: Record<string, string> = {
  IDENTIFICATION: "Identificação",
  ADDRESS: "Endereço",
  CONTACT: "Contato",
  COMPLEMENTARY: "Informações complementares",
  PATIENT: "Cliente — dados de paciente (só no segmento saúde)",
  PROFESSIONAL: "Funcionário — profissional de saúde",
};

export const GENDER_OPTIONS = ["Feminino", "Masculino", "Outro", "Prefiro não informar"];
export const MARITAL_OPTIONS = [
  "Solteiro(a)",
  "Casado(a)",
  "Divorciado(a)",
  "Viúvo(a)",
  "União estável",
];

export const UF_LIST = [
  "AC",
  "AL",
  "AP",
  "AM",
  "BA",
  "CE",
  "DF",
  "ES",
  "GO",
  "MA",
  "MT",
  "MS",
  "MG",
  "PA",
  "PB",
  "PR",
  "PE",
  "PI",
  "RJ",
  "RN",
  "RS",
  "RO",
  "RR",
  "SC",
  "SP",
  "SE",
  "TO",
];

export function onlyDigits(value: string | null | undefined): string {
  return (value ?? "").replace(/\D/g, "");
}

/** CPF/CNPJ (inclusive alfanumérico) com máscara; valores já mascarados passam direto. */
export function formatTaxId(value: string | null | undefined): string {
  if (!value) return "—";
  if (value.includes("*")) return value;
  const v = value.toUpperCase();
  if (v.length === 11) return `${v.slice(0, 3)}.${v.slice(3, 6)}.${v.slice(6, 9)}-${v.slice(9)}`;
  if (v.length === 14)
    return `${v.slice(0, 2)}.${v.slice(2, 5)}.${v.slice(5, 8)}/${v.slice(8, 12)}-${v.slice(12)}`;
  return v;
}

export function formatPhone(value: string | null | undefined): string {
  const d = onlyDigits(value);
  if (!d) return "—";
  const local = d.length > 11 && d.startsWith("55") ? d.slice(2) : d;
  if (local.length === 11) return `(${local.slice(0, 2)}) ${local.slice(2, 7)}-${local.slice(7)}`;
  if (local.length === 10) return `(${local.slice(0, 2)}) ${local.slice(2, 6)}-${local.slice(6)}`;
  return d;
}

export function formatCep(value: string | null | undefined): string {
  const d = onlyDigits(value);
  return d.length === 8 ? `${d.slice(0, 5)}-${d.slice(5)}` : d;
}

export function formatDate(value: string | null | undefined): string {
  if (!value) return "—";
  const [y, m, d] = value.slice(0, 10).split("-");
  return y && m && d ? `${d}/${m}/${y}` : value;
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) return "—";
  return new Date(value).toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
}

export function initials(name: string): string {
  const parts = name.trim().split(/\s+/);
  return (
    (parts[0]?.[0] ?? "") + (parts.length > 1 ? (parts[parts.length - 1]?.[0] ?? "") : "")
  ).toUpperCase();
}

/** Valida CPF ou CNPJ (alfanumérico) — a mesma regra do backend. */
export function isValidTaxId(personType: "F" | "J", raw: string): boolean {
  const v = raw.replace(/[^0-9A-Za-z]/g, "").toUpperCase();
  if (personType === "F") {
    if (!/^\d{11}$/.test(v) || /^(\d)\1{10}$/.test(v)) return false;
    const calc = (len: number) => {
      let sum = 0;
      for (let i = 0; i < len; i++) sum += Number(v[i]) * (len + 1 - i);
      const r = (sum * 10) % 11;
      return r === 10 ? 0 : r;
    };
    return calc(9) === Number(v[9]) && calc(10) === Number(v[10]);
  }
  if (!/^[0-9A-Z]{12}\d{2}$/.test(v) || /^(.)\1{13}$/.test(v)) return false;
  const val = (c: string) => c.charCodeAt(0) - 48;
  const dv = (len: number) => {
    const w =
      len === 12 ? [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2] : [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
    let sum = 0;
    for (let i = 0; i < len; i++) sum += val(v[i]) * w[i];
    const r = sum % 11;
    return r < 2 ? 0 : 11 - r;
  };
  return dv(12) === Number(v[12]) && dv(13) === Number(v[13]);
}
