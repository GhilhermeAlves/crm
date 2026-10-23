import { z } from "zod";
import { useAuthorization } from "@/features/identity/auth/hooks/useAuthorization";

export const LOCATION_KINDS = ["GOOGLE_MEET", "PHONE", "IN_PERSON"] as const;
export const ASSIGNMENT_MODES = ["ROUND_ROBIN", "CHOOSE_HOST"] as const;

export const createAppointmentTypeSchema = z.object({
  name: z.string().min(1, "Nome é obrigatório").max(100, "Nome deve ter no máximo 100 caracteres"),
  slug: z.string().max(100).optional(),
  description: z.string().max(500, "Descrição muito longa").optional(),
  durationMinutes: z.coerce
    .number()
    .min(5, "Duração mínima é 5 minutos")
    .max(480, "Duração máxima é 8 horas"),
  bufferBeforeMinutes: z.coerce.number().min(0).max(120).optional(),
  bufferAfterMinutes: z.coerce.number().min(0).max(120).optional(),
  minNoticeHours: z.coerce.number().min(0).max(168).optional(),
  maxDaysAhead: z.coerce.number().min(1).max(365).optional(),
  slotIntervalMinutes: z.coerce.number().min(5).max(60).optional(),
  color: z.string().max(7).optional(),
  locationKind: z.enum(LOCATION_KINDS).optional(),
  locationDetail: z.string().max(200).optional(),
  assignmentMode: z.enum(ASSIGNMENT_MODES).optional(),
  publicBookingEnabled: z.boolean().optional(),
  active: z.boolean().optional(),
  hostIds: z.array(z.string().uuid()).min(1, "Selecione ao menos um responsável"),
});

export type CreateAppointmentTypeFormValues = z.infer<typeof createAppointmentTypeSchema>;

export const INITIAL_STATUSES = ["CONFIRMED", "SCHEDULED", "CANCELED"] as const;
export const RECURRENCE_FREQUENCIES = ["DAILY", "WEEKLY", "MONTHLY", "YEARLY"] as const;
export const RECURRENCE_END_TYPES = ["NEVER", "ON_DATE", "AFTER_COUNT"] as const;

/** Presets da tela; "custom" abre o diálogo "Configurar recorrência". */
export const RECURRENCE_PRESETS = [
  "none",
  "daily",
  "weekly",
  "monthly",
  "yearly",
  "custom",
] as const;
export type RecurrencePreset = (typeof RECURRENCE_PRESETS)[number];

export const recurrenceSchema = z
  .object({
    frequency: z.enum(RECURRENCE_FREQUENCIES),
    interval: z.number().int().min(1).max(99),
    endType: z.enum(RECURRENCE_END_TYPES),
    until: z.string().optional(),
    count: z.number().int().min(2).max(366).optional(),
  })
  .refine((r) => r.endType !== "ON_DATE" || !!r.until, {
    message: "Informe a data final",
    path: ["until"],
  })
  .refine((r) => r.endType !== "AFTER_COUNT" || r.count != null, {
    message: "Informe o número de ocorrências",
    path: ["count"],
  });

export type RecurrenceValue = z.infer<typeof recurrenceSchema>;

const PRESET_FREQUENCY = {
  daily: "DAILY",
  weekly: "WEEKLY",
  monthly: "MONTHLY",
  yearly: "YEARLY",
} as const satisfies Record<string, RecurrenceValue["frequency"]>;

type FixedPreset = keyof typeof PRESET_FREQUENCY;

export function recurrenceFromPreset(preset: FixedPreset | "none"): RecurrenceValue | null {
  if (preset === "none") return null;
  return { frequency: PRESET_FREQUENCY[preset], interval: 1, endType: "NEVER" };
}

export function presetFromRecurrence(r: RecurrenceValue | null | undefined): RecurrencePreset {
  if (!r) return "none";
  if (r.interval === 1 && r.endType === "NEVER") {
    const found = (Object.keys(PRESET_FREQUENCY) as FixedPreset[]).find(
      (k) => PRESET_FREQUENCY[k] === r.frequency,
    );
    if (found) return found;
  }
  return "custom";
}

/** Celular BR: 10 ou 11 dígitos (DDD + número), opcionalmente com o 55 na frente. */
export function isValidBrazilianPhone(value: string): boolean {
  let digits = value.replace(/\D/g, "");
  if ((digits.length === 12 || digits.length === 13) && digits.startsWith("55")) {
    digits = digits.slice(2);
  }
  return digits.length === 10 || digits.length === 11;
}

export const createAppointmentSchema = z.object({
  appointmentTypeId: z.string().uuid().optional(),
  hostId: z.string().uuid("Responsável é obrigatório"),
  contactId: z.string().uuid("Contato inválido").optional(),
  opportunityId: z.string().uuid("Oportunidade inválida").optional(),
  title: z
    .string()
    .min(1, "Título é obrigatório")
    .max(200, "Título deve ter no máximo 200 caracteres"),
  startAt: z.string().min(1, "Data/hora de início é obrigatória"),
  endAt: z.string().min(1, "Data/hora de fim é obrigatória"),
  status: z.enum(INITIAL_STATUSES).optional(),
  locationKind: z.enum(LOCATION_KINDS).optional(),
  locationDetail: z.string().max(200).optional(),
  notes: z.string().max(2000, "Observações muito longas").optional(),
  recurrence: recurrenceSchema.nullable().optional(),
  catalogItemIds: z.array(z.string().uuid()).optional(),
  /** Só vai preenchido quando o celular do paciente foi alterado no formulário. */
  contactPhone: z
    .string()
    .optional()
    .refine((v) => !v || isValidBrazilianPhone(v), "Celular inválido (ex: (11) 99999-9999)"),
  force: z.boolean().optional(),
  chairId: z.string().uuid().optional(),
  tagIds: z.array(z.string().uuid()).optional(),
});

export type CreateAppointmentFormValues = z.infer<typeof createAppointmentSchema>;

export const createBlockSchema = z.object({
  hostId: z.string().uuid("Responsável é obrigatório"),
  startAt: z.string().min(1, "Data/hora de início é obrigatória"),
  endAt: z.string().min(1, "Data/hora de fim é obrigatória"),
  reason: z.string().max(200, "Motivo muito longo").optional(),
  recurrence: recurrenceSchema.nullable().optional(),
});

export type CreateBlockFormValues = z.infer<typeof createBlockSchema>;

export function useSchedulingPermissions() {
  const { can } = useAuthorization();
  return {
    canRead: can("appointment:read"),
    canCreate: can("appointment:create"),
    canUpdate: can("appointment:update"),
    canDelete: can("appointment:delete"),
    canConfigure: can("scheduling:configure"),
  };
}
