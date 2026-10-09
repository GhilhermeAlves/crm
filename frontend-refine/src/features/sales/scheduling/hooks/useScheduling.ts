import { useQuery, useMutation } from "@tanstack/react-query";
import { useMutationDefaults } from "@/lib/query/mutation-utils";
import {
  AppointmentTypeService,
  AppointmentService,
  BlockService,
  AvailabilityService,
  BirthdayMessageService,
  type BirthdayMessageSettings,
} from "../services/scheduling.service";
import type {
  AppointmentType,
  CreateAppointmentTypeRequest,
  UpdateAppointmentTypeRequest,
  CreateAppointmentRequest,
  UpdateAppointmentRequest,
  RescheduleRequest,
  CreateBlockRequest,
  AppointmentStatus,
  Availability,
} from "../types/scheduling.types";

function invalidateSchedulingKeys(companyId: string | null): unknown[][] {
  return [
    ["appointments", companyId],
    ["appointment-types", companyId],
    ["blocks", companyId],
  ];
}

// --- AppointmentType ---

export function useAppointmentTypes(companyId: string | null) {
  return useQuery({
    queryKey: ["appointment-types", companyId],
    queryFn: () => AppointmentTypeService.list(companyId as string),
    enabled: !!companyId,
  });
}

export function useCreateAppointmentType(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (data: CreateAppointmentTypeRequest) =>
      AppointmentTypeService.create(companyId as string, data),
    onSuccess: onSuccess({
      successMessage: "Tipo de agendamento criado",
      invalidateKeys: [["appointment-types", companyId]],
    }),
    onError: onError({ errorMessage: "Erro ao criar tipo de agendamento" }),
  });
}

export function useUpdateAppointmentType(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateAppointmentTypeRequest }) =>
      AppointmentTypeService.update(companyId as string, id, data),
    onSuccess: onSuccess({
      successMessage: "Tipo de agendamento atualizado",
      invalidateKeys: [["appointment-types", companyId]],
    }),
    onError: onError({ errorMessage: "Erro ao atualizar tipo de agendamento" }),
  });
}

/**
 * Liga/desliga um profissional para receber agendamentos: inclui ou remove o
 * usuário como responsável em todos os tipos de agendamento. É o que o cálculo
 * de horários livres (inclusive o do agente) usa para saber quem atende.
 */
export function useSetProfessionalScheduling(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults<
    unknown,
    { userId: string; enabled: boolean; types: AppointmentType[] }
  >();
  return useMutation({
    mutationFn: ({
      userId,
      enabled,
      types,
    }: {
      userId: string;
      enabled: boolean;
      types: AppointmentType[];
    }) =>
      Promise.all(
        types
          .filter((t) => t.hostIds.includes(userId) !== enabled)
          .map((t) =>
            AppointmentTypeService.update(companyId as string, t.id, {
              name: t.name,
              slug: t.slug,
              description: t.description ?? undefined,
              durationMinutes: t.durationMinutes,
              bufferBeforeMinutes: t.bufferBeforeMinutes,
              bufferAfterMinutes: t.bufferAfterMinutes,
              minNoticeHours: t.minNoticeHours,
              maxDaysAhead: t.maxDaysAhead,
              slotIntervalMinutes: t.slotIntervalMinutes,
              color: t.color ?? undefined,
              locationKind: t.locationKind ?? undefined,
              locationDetail: t.locationDetail ?? undefined,
              assignmentMode: t.assignmentMode ?? undefined,
              publicBookingEnabled: t.publicBookingEnabled,
              active: t.active,
              hostIds: enabled ? [...t.hostIds, userId] : t.hostIds.filter((h) => h !== userId),
            }),
          ),
      ),
    onSuccess: onSuccess({
      successMessage: (_, v) =>
        v.enabled ? "Profissional ativado na agenda" : "Profissional desativado na agenda",
      invalidateKeys: [["appointment-types", companyId]],
    }),
    onError: onError({ errorMessage: "Erro ao atualizar a agenda do profissional" }),
  });
}

export function useDeleteAppointmentType(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (id: string) => AppointmentTypeService.delete(companyId as string, id),
    onSuccess: onSuccess({
      successMessage: "Tipo de agendamento excluído",
      invalidateKeys: [["appointment-types", companyId]],
    }),
    onError: onError({ errorMessage: "Erro ao excluir tipo de agendamento" }),
  });
}

// --- Appointments ---

export function useAppointments(
  companyId: string | null,
  from: string | null,
  to: string | null,
  hostIds?: string[],
) {
  return useQuery({
    queryKey: ["appointments", companyId, from, to, hostIds],
    queryFn: () =>
      AppointmentService.list(companyId as string, from as string, to as string, hostIds),
    enabled: !!companyId && !!from && !!to,
  });
}

export function useCreateAppointment(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (data: CreateAppointmentRequest) =>
      AppointmentService.create(companyId as string, data),
    onSuccess: onSuccess({
      successMessage: "Agendamento criado",
      invalidateKeys: invalidateSchedulingKeys(companyId),
    }),
    onError: onError({ errorMessage: "Erro ao criar agendamento" }),
  });
}

export function useUpdateAppointment(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateAppointmentRequest }) =>
      AppointmentService.update(companyId as string, id, data),
    onSuccess: onSuccess({
      successMessage: "Agendamento atualizado",
      invalidateKeys: invalidateSchedulingKeys(companyId),
    }),
    onError: onError({ errorMessage: "Erro ao atualizar agendamento" }),
  });
}

export function useRescheduleAppointment(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: RescheduleRequest }) =>
      AppointmentService.reschedule(companyId as string, id, data),
    onSuccess: onSuccess({
      successMessage: "Agendamento remarcado",
      invalidateKeys: invalidateSchedulingKeys(companyId),
    }),
    onError: onError({ errorMessage: "Erro ao remarcar agendamento" }),
  });
}

export function useChangeAppointmentStatus(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: ({ id, status }: { id: string; status: AppointmentStatus }) =>
      AppointmentService.changeStatus(companyId as string, id, status),
    onSuccess: onSuccess({
      invalidateKeys: invalidateSchedulingKeys(companyId),
    }),
    onError: onError({ errorMessage: "Erro ao alterar status" }),
  });
}

export function useDeleteAppointment(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (id: string) => AppointmentService.delete(companyId as string, id),
    onSuccess: onSuccess({
      successMessage: "Agendamento excluído",
      invalidateKeys: invalidateSchedulingKeys(companyId),
    }),
    onError: onError({ errorMessage: "Erro ao excluir agendamento" }),
  });
}

// --- Blocks ---

export function useBlocks(
  companyId: string | null,
  from: string | null,
  to: string | null,
  hostIds?: string[],
) {
  return useQuery({
    queryKey: ["blocks", companyId, from, to, hostIds],
    queryFn: () => BlockService.list(companyId as string, from as string, to as string, hostIds),
    enabled: !!companyId && !!from && !!to,
  });
}

export function useCreateBlock(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (data: CreateBlockRequest) => BlockService.create(companyId as string, data),
    onSuccess: onSuccess({
      successMessage: "Bloqueio criado",
      invalidateKeys: invalidateSchedulingKeys(companyId),
    }),
    onError: onError({ errorMessage: "Erro ao criar bloqueio" }),
  });
}

export function useDeleteBlock(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (id: string) => BlockService.delete(companyId as string, id),
    onSuccess: onSuccess({
      successMessage: "Bloqueio removido",
      invalidateKeys: invalidateSchedulingKeys(companyId),
    }),
    onError: onError({ errorMessage: "Erro ao remover bloqueio" }),
  });
}

// --- Availability ---

export function useAvailability(companyId: string | null, userId: string | null) {
  return useQuery({
    queryKey: ["availability", companyId, userId],
    queryFn: () => AvailabilityService.get(companyId as string, userId as string),
    enabled: !!companyId && !!userId,
  });
}

export function useSetAvailability(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: ({ userId, data }: { userId: string; data: Availability }) =>
      AvailabilityService.set(companyId as string, userId, data),
    onSuccess: onSuccess({
      successMessage: "Disponibilidade salva",
      invalidateKeys: [["availability", companyId]],
    }),
    onError: onError({ errorMessage: "Erro ao salvar disponibilidade" }),
  });
}

// --- Slots ---

export function useSlots(
  companyId: string | null,
  typeId: string | null,
  from: string | null,
  to: string | null,
) {
  return useQuery({
    queryKey: ["slots", companyId, typeId, from, to],
    queryFn: () =>
      AppointmentTypeService.getSlots(
        companyId as string,
        typeId as string,
        from as string,
        to as string,
      ),
    enabled: !!companyId && !!typeId && !!from && !!to,
  });
}

// --- Birthday message ---

export function useBirthdayMessage(companyId: string | null) {
  return useQuery({
    queryKey: ["birthday-message", companyId],
    queryFn: () => BirthdayMessageService.get(companyId as string),
    enabled: !!companyId,
  });
}

export function useSaveBirthdayMessage(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (data: BirthdayMessageSettings) =>
      BirthdayMessageService.save(companyId as string, data),
    onSuccess: onSuccess({
      successMessage: "Mensagem de aniversário salva",
      invalidateKeys: [["birthday-message", companyId]],
    }),
    onError: onError({ errorMessage: "Erro ao salvar mensagem de aniversário" }),
  });
}
