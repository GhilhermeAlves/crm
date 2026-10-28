import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMutationDefaults } from "@/lib/query/mutation-utils";
import { PatientService } from "../services/patient.service";
import { toast } from "sonner";
import type {
  CreateContactRequest,
  UpdateContactRequest,
} from "@/features/masterdata/contacts/types/contact.types";
import type { CreateContactNoteRequest, UpdateContactNoteRequest } from "../types/patient.types";

function patientKeys(companyId: string | null): unknown[][] {
  return [
    ["patients", companyId],
    ["patient", companyId],
    // Paciente também é contato: mantém Contatos/Customer 360 em dia.
    ["contacts", companyId],
    ["contact", companyId],
    ["customer360", companyId],
  ];
}

export function usePatients(companyId: string | null, search: string) {
  return useQuery({
    queryKey: ["patients", companyId, search],
    queryFn: () => PatientService.list(companyId as string, search || undefined),
    enabled: !!companyId,
  });
}

export function usePatient(companyId: string | null, patientId: string | null | undefined) {
  return useQuery({
    queryKey: ["patient", companyId, patientId],
    queryFn: () => PatientService.findById(companyId as string, patientId as string),
    enabled: !!companyId && !!patientId,
  });
}

export function useCreatePatient(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (data: CreateContactRequest) => PatientService.create(companyId as string, data),
    onSuccess: onSuccess({
      successMessage: "Paciente cadastrado",
      invalidateKeys: patientKeys(companyId),
    }),
    onError: onError({ errorMessage: "Erro ao cadastrar paciente" }),
  });
}

export function useUpdatePatient(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateContactRequest }) =>
      PatientService.update(companyId as string, id, data),
    onSuccess: onSuccess({
      successMessage: "Paciente atualizado",
      invalidateKeys: patientKeys(companyId),
    }),
    onError: onError({ errorMessage: "Erro ao atualizar paciente" }),
  });
}

export function useMakePatient(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (contactId: string) => PatientService.makePatient(companyId as string, contactId),
    onSuccess: onSuccess({
      successMessage: "Contato agora é paciente",
      invalidateKeys: patientKeys(companyId),
    }),
    onError: onError({ errorMessage: "Erro ao tornar paciente" }),
  });
}

export function useContactAppointments(companyId: string | null, contactId: string | null) {
  return useQuery({
    queryKey: ["contact-appointments", companyId, contactId],
    queryFn: () => PatientService.listAppointments(companyId as string, contactId as string),
    enabled: !!companyId && !!contactId,
  });
}

export function useContactNotes(companyId: string | null, contactId: string | null) {
  return useQuery({
    queryKey: ["contact-notes", companyId, contactId],
    queryFn: () => PatientService.listNotes(companyId as string, contactId as string),
    enabled: !!companyId && !!contactId,
    // As outras abas gravam anotações automáticas no backend: sempre busca ao abrir.
    staleTime: 0,
    refetchOnMount: "always",
  });
}

export function useContactFiles(companyId: string | null, contactId: string | null) {
  return useQuery({
    queryKey: ["contact-files", companyId, contactId],
    queryFn: () => PatientService.listFiles(companyId as string, contactId as string),
    enabled: !!companyId && !!contactId,
  });
}

function notesKey(companyId: string | null, contactId: string | null): unknown[] {
  return ["contact-notes", companyId, contactId];
}

export function useCreateContactNote(companyId: string | null, contactId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (data: CreateContactNoteRequest) =>
      PatientService.createNote(companyId as string, contactId as string, data),
    onSuccess: onSuccess({
      successMessage: "Anotação criada",
      invalidateKeys: [notesKey(companyId, contactId)],
    }),
    onError: onError({ errorMessage: "Erro ao criar anotação" }),
  });
}

export function useUpdateContactNote(companyId: string | null, contactId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: ({ noteId, data }: { noteId: string; data: UpdateContactNoteRequest }) =>
      PatientService.updateNote(companyId as string, contactId as string, noteId, data),
    onSuccess: onSuccess({
      successMessage: "Anotação atualizada",
      invalidateKeys: [notesKey(companyId, contactId)],
    }),
    onError: onError({ errorMessage: "Erro ao atualizar anotação" }),
  });
}

export function useDeleteContactNote(companyId: string | null, contactId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (noteId: string) =>
      PatientService.deleteNote(companyId as string, contactId as string, noteId),
    onSuccess: onSuccess({
      successMessage: "Anotação excluída",
      invalidateKeys: [notesKey(companyId, contactId)],
    }),
    onError: onError({ errorMessage: "Erro ao excluir anotação" }),
  });
}

export function useUploadContactFile(companyId: string | null, contactId: string | null) {
  const { onSuccess, onError } = useMutationDefaults<unknown, File>();
  return useMutation({
    mutationFn: (file: File) =>
      PatientService.uploadFile(companyId as string, contactId as string, file),
    onSuccess: onSuccess({
      successMessage: "Arquivo enviado",
      invalidateKeys: [
        ["contact-files", companyId, contactId],
        ["storage", companyId],
        // O upload gera notificação com download e uma anotação automática.
        ["notifications", companyId],
        ["notifications-unread", companyId],
        ["contact-notes", companyId, contactId],
      ],
    }),
    onError: onError({ errorMessage: "Erro ao enviar arquivo" }),
  });
}

export function useRequestFileDownload(companyId: string | null, contactId: string | null) {
  const { onSuccess, onError } = useMutationDefaults<unknown, string>();
  return useMutation({
    mutationFn: (objectId: string) =>
      PatientService.requestFileDownload(companyId as string, contactId as string, objectId),
    onSuccess: onSuccess({
      successMessage: "Link de download enviado para o sino de notificações",
      invalidateKeys: [
        ["notifications", companyId],
        ["notifications-unread", companyId],
      ],
    }),
    onError: onError({ errorMessage: "Erro ao preparar o download" }),
  });
}

export function useUploadPatientPhoto(companyId: string | null, contactId: string | null) {
  const { onSuccess, onError } = useMutationDefaults<unknown, File>();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (file: File) =>
      PatientService.uploadPhoto(companyId as string, contactId as string, file),
    onSuccess: (data, _vars, _ctx) => {
      queryClient.invalidateQueries({ queryKey: ["patient", companyId, contactId] });
      queryClient.invalidateQueries({ queryKey: ["patients", companyId] });
      queryClient.invalidateQueries({ queryKey: patientKeys(companyId) });
      queryClient.invalidateQueries({ queryKey: ["patient-photo", companyId, contactId] });
      toast.success("Foto atualizada");
    },
    onError: (error: unknown) => {
      const message = (error as { response?: { data?: { message?: string } } })?.response?.data
        ?.message;
      toast.error(message || "Erro ao enviar foto");
    },
  });
}

export function useDeletePatientPhoto(companyId: string | null, contactId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => PatientService.deletePhoto(companyId as string, contactId as string),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["patient", companyId, contactId] });
      queryClient.invalidateQueries({ queryKey: ["patients", companyId] });
      queryClient.invalidateQueries({ queryKey: patientKeys(companyId) });
      queryClient.invalidateQueries({ queryKey: ["patient-photo", companyId, contactId] });
      toast.success("Foto removida");
    },
    onError: (error: unknown) => {
      const message = (error as { response?: { data?: { message?: string } } })?.response?.data
        ?.message;
      toast.error(message || "Erro ao remover foto");
    },
  });
}

export function usePatientPhoto(
  companyId: string | null,
  contactId: string | null,
  photoStorageId?: string | null,
) {
  return useQuery({
    queryKey: ["patient-photo", companyId, contactId, photoStorageId],
    queryFn: () => PatientService.fetchPhoto(companyId as string, contactId as string),
    enabled: !!companyId && !!contactId && !!photoStorageId,
    staleTime: 300000,
  });
}
