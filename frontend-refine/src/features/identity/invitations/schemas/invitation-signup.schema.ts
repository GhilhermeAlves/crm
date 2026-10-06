import { z } from "zod";
import { PASSWORD_PATTERN } from "@/features/identity/auth/schemas/auth.schema";

/**
 * Cadastro por convite: só nome e senha. O e-mail é o do convite e não faz
 * parte do formulário. Mesmas regras de nome e senha do cadastro comum.
 */
export const invitationSignupSchema = z
  .object({
    name: z.string().trim().min(3, "Nome deve ter no mínimo 3 caracteres"),
    password: z.string().regex(PASSWORD_PATTERN, "Sua senha ainda não atende aos requisitos."),
    confirmPassword: z.string(),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: "Senhas não conferem",
    path: ["confirmPassword"],
  });

export type InvitationSignupFormData = z.infer<typeof invitationSignupSchema>;
