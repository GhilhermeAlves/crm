import { z } from "zod";
import { useAuthorization } from "@/features/auth/hooks/useAuthorization";

export const formatCurrency = (value: number): string =>
  new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(value);

export const formatPercent = (value: number): string =>
  new Intl.NumberFormat("pt-BR", {
    style: "percent",
    maximumFractionDigits: 1,
  }).format(value);

export const opportunityFormSchema = z.object({
  title: z
    .string()
    .min(1, "Título é obrigatório")
    .max(200, "Título deve ter no máximo 200 caracteres"),
  value: z
    .string()
    .min(1, "Valor é obrigatório")
    .refine((val) => {
      const n = Number(val);
      return !isNaN(n) && n > 0;
    }, "Valor deve ser maior que zero"),
  contactId: z
    .string()
    .min(1, "Contato é obrigatório")
    .uuid("Contato inválido (UUID)"),
  expectedCloseDate: z.string().optional(),
  notes: z
    .string()
    .max(1000, "Notas devem ter no máximo 1000 caracteres")
    .optional(),
});

export type OpportunityFormValues = z.infer<typeof opportunityFormSchema>;

export function useOpportunityPermissions() {
  const { can } = useAuthorization();
  return {
    canCreate: can("opportunity:create"),
    canMove: can("opportunity:move"),
    canWin: can("opportunity:win"),
    canLose: can("opportunity:lose"),
    canDelete: can("opportunity:delete"),
  };
}
