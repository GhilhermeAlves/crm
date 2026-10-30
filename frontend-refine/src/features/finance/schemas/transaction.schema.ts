import { z } from "zod";
import { CATEGORIES_BY_TYPE } from "../types/finance.types";
import { parseBRLToCents } from "../utils/money";

export const TRANSACTION_TYPES = ["INCOME", "EXPENSE"] as const;
export const PAYMENT_METHODS = [
  "BOLETO",
  "CREDIT_CARD",
  "DEBIT_CARD",
  "PIX",
  "CASH",
  "TRANSFER",
  "CHECK",
  "OTHER",
] as const;

const todayIso = (): string => {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return `${now.getFullYear()}-${month}-${day}`;
};

export const transactionFormSchema = z
  .object({
    type: z.enum(TRANSACTION_TYPES),
    description: z
      .string()
      .min(1, "Descrição é obrigatória")
      .max(255, "Descrição deve ter no máximo 255 caracteres"),
    category: z.string().min(1, "Categoria é obrigatória"),
    amount: z.string().min(1, "Valor é obrigatório"),
    paymentMethod: z.string().min(1, "Forma de pagamento é obrigatória"),
    dueDate: z.string().min(1, "Vencimento é obrigatório"),
    installments: z.coerce.number().int().min(1).max(24).optional(),
    recurring: z.boolean().optional(),
    contactId: z.string().optional(),
    professionalUserId: z.string().optional(),
    catalogItemId: z.string().optional(),
    fee: z.string().optional(),
    paid: z.boolean().optional(),
    paidAt: z.string().optional(),
    notes: z.string().max(2000, "Observações muito longas").optional(),
  })
  .superRefine((values, ctx) => {
    const amountCents = parseBRLToCents(values.amount);
    if (amountCents === null || amountCents <= 0) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["amount"],
        message: "Informe um valor maior que zero",
      });
    }

    const allowedCategories = CATEGORIES_BY_TYPE[values.type] as string[];
    if (!allowedCategories.includes(values.category)) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["category"],
        message: "Selecione uma categoria válida",
      });
    }

    const feeCents =
      values.fee && values.fee.trim() !== "" ? (parseBRLToCents(values.fee) ?? 0) : 0;
    if (feeCents > 0 && amountCents !== null && feeCents > amountCents) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["fee"],
        message: "A taxa não pode ser maior que o valor",
      });
    }

    if (values.paid) {
      if (!values.paidAt) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ["paidAt"],
          message: "Informe a data do pagamento",
        });
      } else if (values.paidAt > todayIso()) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ["paidAt"],
          message: "A data do pagamento não pode ser futura",
        });
      }
    }
  });

export type TransactionFormValues = z.infer<typeof transactionFormSchema>;
