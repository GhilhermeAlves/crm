import { z } from "zod";
import { tenantAddressSchema } from "@/features/tenants/schemas/tenant.schema";

const cnpjRegex = /^\d{2}\.\d{3}\.\d{3}\/\d{4}-\d{2}$/;
const phoneRegex = /^\(\d{2}\)\s?\d{4,5}-\d{4}$/;

/**
 * Schema do onboarding (Sprint 8.3). Só os dados necessários para criar a
 * primeira empresa — plano/limites/status usam default no backend.
 */
export const onboardingCompanySchema = z.object({
  legalName: z.string().min(1, "Razão Social é obrigatória").max(200),
  tradingName: z.string().min(1, "Nome Fantasia é obrigatório").max(200),
  cnpj: z
    .string()
    .min(1, "CNPJ é obrigatório")
    .regex(cnpjRegex, "CNPJ inválido (ex: 12.345.678/0001-90)"),
  email: z.string().min(1, "E-mail é obrigatório").email("E-mail inválido"),
  phone: z
    .string()
    .min(1, "Telefone é obrigatório")
    .regex(phoneRegex, "Telefone inválido (ex: (11) 99999-9999)"),
  address: tenantAddressSchema,
});

export type OnboardingCompanyFormData = z.infer<typeof onboardingCompanySchema>;
