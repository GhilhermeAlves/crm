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
  hostIds: z.array(z.string().uuid()).min(1, "Selecione ao menos um responsável"),
});

export type CreateAppointmentTypeFormValues = z.infer<typeof createAppointmentTypeSchema>;

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
  locationKind: z.enum(LOCATION_KINDS).optional(),
  locationDetail: z.string().max(200).optional(),
  notes: z.string().max(2000, "Observações muito longas").optional(),
  force: z.boolean().optional(),
});

export type CreateAppointmentFormValues = z.infer<typeof createAppointmentSchema>;

export const createBlockSchema = z.object({
  hostId: z.string().uuid("Responsável é obrigatório"),
  startAt: z.string().min(1, "Data/hora de início é obrigatória"),
  endAt: z.string().min(1, "Data/hora de fim é obrigatória"),
  reason: z.string().max(200, "Motivo muito longo").optional(),
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
