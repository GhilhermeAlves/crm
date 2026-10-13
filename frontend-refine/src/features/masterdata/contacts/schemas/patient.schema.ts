import { z } from "zod";
import { formatCep, formatCpf, formatPhone, validateCpf } from "@/lib/format";
import type { Contact, CreateContactRequest } from "../types/contact.types";

export const GENDER_OPTIONS = [
  { value: "MASCULINO", label: "Masculino" },
  { value: "FEMININO", label: "Feminino" },
  { value: "OUTRO", label: "Outro" },
] as const;

export const MARITAL_STATUS_OPTIONS = [
  { value: "SOLTEIRO", label: "Solteiro(a)" },
  { value: "CASADO", label: "Casado(a)" },
  { value: "DIVORCIADO", label: "Divorciado(a)" },
  { value: "VIUVO", label: "Viúvo(a)" },
  { value: "UNIAO_ESTAVEL", label: "União Estável" },
] as const;

export const PROFESSIONAL_STATUS_OPTIONS = [
  { value: "ATIVO", label: "Ativo" },
  { value: "INATIVO", label: "Inativo" },
  { value: "PROSPECTO", label: "Prospecto" },
  { value: "BLOQUEADO", label: "Bloqueado" },
] as const;

export const UF_OPTIONS = [
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
] as const;

const optionalCpf = z
  .string()
  .max(14)
  .optional()
  .refine((val) => !val || val.replace(/\D/g, "").length === 0 || validateCpf(val), "CPF inválido");

export function createPatientSchema(options: { requireCpf?: boolean } = {}) {
  const requireCpf = options.requireCpf ?? false;
  return z
    .object({
      firstName: z.string().min(1, "Nome é obrigatório").max(100),
      lastName: z.string().max(100).optional(),
      birthDate: z.string().optional(),
      cpf: optionalCpf,
      rg: z.string().max(20).optional(),
      rgIssuer: z.string().max(20).optional(),
      gender: z.string().max(20).optional(),
      maritalStatus: z.string().max(30).optional(),
      professionalStatus: z.string().max(20).optional(),
      healthPlan: z.string().max(80).optional(),
      healthPlanNumber: z.string().max(40).optional(),
      recordNumber: z.string().max(40).optional(),
      referralSource: z.string().max(80).optional(),
      mobile: z.string().min(1, "Celular é obrigatório").max(20),
      phone: z.string().max(20).optional(),
      email: z.string().min(1, "E-mail é obrigatório").max(255).email("E-mail inválido"),
      cep: z
        .string()
        .max(10)
        .optional()
        .refine(
          (val) =>
            !val || val.replace(/\D/g, "").length === 0 || val.replace(/\D/g, "").length === 8,
          "CEP inválido",
        ),
      street: z.string().min(1, "Logradouro é obrigatório").max(160),
      addressNumber: z.string().min(1, "Número é obrigatório").max(20),
      complement: z.string().max(80).optional(),
      neighborhood: z.string().min(1, "Bairro é obrigatório").max(80),
      city: z.string().min(1, "Cidade é obrigatória").max(80),
      state: z
        .string()
        .min(1, "Estado é obrigatório")
        .max(2)
        .refine((val) => (UF_OPTIONS as readonly string[]).includes(val), "UF inválida"),
      hasGuardian: z.boolean().optional(),
      guardianName: z.string().max(160).optional(),
      guardianCpf: optionalCpf,
      guardianPhone: z.string().max(20).optional(),
      notes: z.string().max(500).optional(),
    })
    .superRefine((data, ctx) => {
      if (requireCpf && (!data.cpf || data.cpf.replace(/\D/g, "").length === 0)) {
        ctx.addIssue({ code: z.ZodIssueCode.custom, message: "CPF é obrigatório", path: ["cpf"] });
      }
      if (data.hasGuardian) {
        if (!data.guardianName || data.guardianName.trim().length === 0) {
          ctx.addIssue({
            code: z.ZodIssueCode.custom,
            message: "Nome do responsável é obrigatório",
            path: ["guardianName"],
          });
        }
        if (!data.guardianPhone || data.guardianPhone.replace(/\D/g, "").length < 10) {
          ctx.addIssue({
            code: z.ZodIssueCode.custom,
            message: "Telefone do responsável é obrigatório",
            path: ["guardianPhone"],
          });
        }
      }
    });
}

export type PatientFormValues = z.infer<ReturnType<typeof createPatientSchema>>;

export function contactToFormValues(contact: Contact): PatientFormValues {
  return {
    firstName: contact.firstName ?? "",
    lastName: contact.lastName ?? "",
    birthDate: contact.birthDate ?? "",
    cpf: contact.cpf ? formatCpf(contact.cpf) : "",
    rg: contact.rg ?? "",
    rgIssuer: contact.rgIssuer ?? "",
    gender: contact.gender ?? "",
    maritalStatus: contact.maritalStatus ?? "",
    professionalStatus: contact.professionalStatus ?? "",
    healthPlan: contact.healthPlan ?? "",
    healthPlanNumber: contact.healthPlanNumber ?? "",
    recordNumber: contact.recordNumber ?? "",
    referralSource: contact.referralSource ?? "",
    mobile: contact.mobile ? formatPhone(contact.mobile) : "",
    phone: contact.phone ? formatPhone(contact.phone) : "",
    email: contact.email ?? "",
    cep: contact.zipCode ? formatCep(contact.zipCode) : "",
    street: contact.street ?? "",
    addressNumber: contact.addressNumber ?? "",
    complement: contact.addressComplement ?? "",
    neighborhood: contact.neighborhood ?? "",
    city: contact.city ?? "",
    state: contact.state ?? "",
    hasGuardian: contact.hasGuardian ?? false,
    guardianName: contact.guardianName ?? "",
    guardianCpf: contact.guardianCpf ? formatCpf(contact.guardianCpf) : "",
    guardianPhone: contact.guardianPhone ? formatPhone(contact.guardianPhone) : "",
    notes: contact.notes ?? "",
  };
}

/**
 * Converte os valores do formulário no payload aceito pela API. Campos de texto
 * vazios vão como `""` (o backend normaliza para `null` e, na edição, é assim
 * que um campo é limpo). Máscaras de CPF/telefone/CEP são removidas.
 */
export function formValuesToContactPayload(values: PatientFormValues): CreateContactRequest {
  const digits = (v?: string) => (v ?? "").replace(/\D/g, "");
  const text = (v?: string) => (v ?? "").trim();
  return {
    firstName: values.firstName.trim(),
    lastName: text(values.lastName),
    email: text(values.email),
    phone: digits(values.phone),
    mobile: digits(values.mobile),
    notes: text(values.notes),
    birthDate: values.birthDate?.trim() ? values.birthDate : undefined,
    cpf: digits(values.cpf),
    rg: text(values.rg),
    rgIssuer: text(values.rgIssuer),
    gender: text(values.gender),
    maritalStatus: text(values.maritalStatus),
    professionalStatus: text(values.professionalStatus),
    zipCode: digits(values.cep),
    street: text(values.street),
    addressNumber: text(values.addressNumber),
    addressComplement: text(values.complement),
    neighborhood: text(values.neighborhood),
    city: text(values.city),
    state: text(values.state),
    healthPlan: text(values.healthPlan),
    healthPlanNumber: text(values.healthPlanNumber),
    recordNumber: text(values.recordNumber),
    referralSource: text(values.referralSource),
    hasGuardian: values.hasGuardian === true,
    guardianName: text(values.guardianName),
    guardianCpf: digits(values.guardianCpf),
    guardianPhone: digits(values.guardianPhone),
  };
}
