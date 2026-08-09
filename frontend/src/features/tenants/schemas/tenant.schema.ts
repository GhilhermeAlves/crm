import { z } from "zod";

const cnpjRegex = /^\d{2}\.\d{3}\.\d{3}\/\d{4}-\d{2}$/;
const phoneRegex = /^\(\d{2}\)\s?\d{4,5}-\d{4}$/;
const cepRegex = /^\d{5}-\d{3}$/;

export const tenantAddressSchema = z.object({
  zipCode: z
    .string()
    .min(1, "CEP é obrigatório")
    .regex(cepRegex, "CEP inválido (ex: 01001-000)"),
  street: z.string().min(1, "Logradouro é obrigatório").max(200),
  number: z.string().min(1, "Número é obrigatório").max(20),
  complement: z.string().max(100).optional().default(""),
  neighborhood: z.string().min(1, "Bairro é obrigatório").max(100),
  city: z.string().min(1, "Cidade é obrigatória").max(100),
  state: z.string().min(2, "Estado é obrigatório").max(2),
  country: z.string().min(1, "País é obrigatório").max(60).default("Brasil"),
});

export const tenantSchema = z.object({
  legalName: z
    .string()
    .min(1, "Razão Social é obrigatória")
    .max(200, "Razão Social deve ter no máximo 200 caracteres"),
  tradingName: z
    .string()
    .min(1, "Nome Fantasia é obrigatório")
    .max(200, "Nome Fantasia deve ter no máximo 200 caracteres"),
  cnpj: z
    .string()
    .min(1, "CNPJ é obrigatório")
    .regex(cnpjRegex, "CNPJ inválido (ex: 12.345.678/0001-90)"),
  stateRegistration: z.string().max(30).optional().default(""),
  municipalRegistration: z.string().max(30).optional().default(""),
  email: z.string().min(1, "E-mail é obrigatório").email("E-mail inválido"),
  phone: z
    .string()
    .min(1, "Telefone é obrigatório")
    .regex(phoneRegex, "Telefone inválido (ex: (11) 99999-9999)"),
  website: z
    .string()
    .optional()
    .default("")
    .refine(
      (val) => !val || /^https?:\/\/.+/.test(val),
      { message: "Website deve começar com http:// ou https://" },
    ),
  status: z.enum(["active", "suspended", "onboarding", "inactive"]),
  plan: z.enum(["starter", "professional", "business", "enterprise"]),
  maxUsers: z
    .number({ invalid_type_error: "Limite de usuários é obrigatório" })
    .min(1, "Mínimo 1 usuário")
    .max(10000, "Máximo 10.000 usuários"),
  maxStorageMb: z
    .number({ invalid_type_error: "Limite de armazenamento é obrigatório" })
    .min(100, "Mínimo 100 MB")
    .max(1000000, "Máximo 1 TB"),
  maxContacts: z
    .number({ invalid_type_error: "Limite de contatos é obrigatório" })
    .min(1, "Mínimo 1 contato")
    .max(1000000, "Máximo 1.000.000 contatos"),
  logoUrl: z.string().nullable().optional().default(null),
  notes: z.string().max(2000).optional().default(""),
  address: tenantAddressSchema,
});

export type TenantFormData = z.infer<typeof tenantSchema>;
export type TenantAddressFormData = z.infer<typeof tenantAddressSchema>;

export const tenantStatusLabels: Record<string, string> = {
  active: "Ativa",
  suspended: "Suspensa",
  onboarding: "Em implantação",
  inactive: "Inativa",
};

export const tenantPlanLabels: Record<string, string> = {
  starter: "Starter",
  professional: "Professional",
  business: "Business",
  enterprise: "Enterprise",
};
