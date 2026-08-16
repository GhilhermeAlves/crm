import { z } from "zod";

const phoneRegex = /^\(?\d{2}\)?\s?\d{4,5}-?\d{4}$/;

export const userStatusLabels: Record<string, string> = {
  active: "Ativo",
  inactive: "Inativo",
  locked: "Bloqueado",
  pending: "Pendente",
};

export const userSchema = z.object({
  firstName: z
    .string()
    .min(1, "Nome é obrigatório")
    .max(255, "Nome deve ter no máximo 255 caracteres"),
  lastName: z
    .string()
    .min(1, "Sobrenome é obrigatório")
    .max(255, "Sobrenome deve ter no máximo 255 caracteres"),
  email: z.string().min(1, "Email é obrigatório").email("Email inválido"),
  phone: z
    .string()
    .optional()
    .refine((val) => !val || phoneRegex.test(val.replace(/\s/g, "")), {
      message: "Telefone inválido. Use o formato (XX) XXXXX-XXXX",
    }),
  department: z.string().optional(),
  jobTitle: z.string().optional(),
  language: z.string().optional(),
  timezone: z.string().optional(),
  notes: z.string().optional(),
});

export const inviteUserSchema = z.object({
  firstName: z
    .string()
    .min(1, "Nome é obrigatório")
    .max(255, "Nome deve ter no máximo 255 caracteres"),
  lastName: z
    .string()
    .min(1, "Sobrenome é obrigatório")
    .max(255, "Sobrenome deve ter no máximo 255 caracteres"),
  email: z.string().min(1, "Email é obrigatório").email("Email inválido"),
  department: z.string().optional(),
  jobTitle: z.string().optional(),
});

export type UserFormData = z.infer<typeof userSchema>;
export type InviteUserFormData = z.infer<typeof inviteUserSchema>;
