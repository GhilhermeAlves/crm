import { z } from "zod";

/**
 * Política de senha espelhada do crm-backend
 * (com.becommerce.crm.domain.identity.valueobject.Password):
 * mínimo 8 caracteres, ao menos 1 maiúscula, 1 minúscula, 1 número
 * e 1 símbolo de {@code @#$%^&+=!}.
 */
export const PASSWORD_PATTERN = /^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$/;

export const PASSWORD_REQUIREMENTS = [
  { label: "Pelo menos 8 caracteres", check: (value: string) => value.length >= 8 },
  { label: "Uma letra maiúscula", check: (value: string) => /[A-Z]/.test(value) },
  { label: "Uma letra minúscula", check: (value: string) => /[a-z]/.test(value) },
  { label: "Um número", check: (value: string) => /[0-9]/.test(value) },
  { label: "Um caractere especial", check: (value: string) => /[@#$%^&+=!]/.test(value) },
] as const;

export type PasswordStrength = "Fraca" | "Média" | "Forte";

export function passwordStrength(value: string): PasswordStrength {
  const satisfied = PASSWORD_REQUIREMENTS.filter((requirement) => requirement.check(value)).length;
  if (satisfied <= 2) return "Fraca";
  if (satisfied < 5) return "Média";
  return "Forte";
}

export const loginSchema = z.object({
  email: z.string().email("Email inválido"),
  password: z.string().min(8, "Senha deve ter no mínimo 8 caracteres"),
});

export const registerSchema = z
  .object({
    name: z.string().min(3, "Nome deve ter no mínimo 3 caracteres"),
    email: z.string().email("Email inválido"),
    password: z
      .string()
      .regex(PASSWORD_PATTERN, "Sua senha ainda não atende aos requisitos."),
    confirmPassword: z.string(),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: "Senhas não conferem",
    path: ["confirmPassword"],
  });

export const forgotPasswordSchema = z.object({
  email: z.string().email("Email inválido"),
});

export const resetPasswordSchema = z
  .object({
    token: z.string().min(1, "Token é obrigatório"),
    password: z.string().min(8, "Senha deve ter no mínimo 8 caracteres"),
    confirmPassword: z.string(),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: "Senhas não conferem",
    path: ["confirmPassword"],
  });

export type LoginFormData = z.infer<typeof loginSchema>;
export type RegisterFormData = z.infer<typeof registerSchema>;
export type ForgotPasswordFormData = z.infer<typeof forgotPasswordSchema>;
export type ResetPasswordFormData = z.infer<typeof resetPasswordSchema>;
