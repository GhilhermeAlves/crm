import { useMutation, useQuery } from "@tanstack/react-query";
import { useMutationDefaults } from "@/lib/query/mutation-utils";
import { PatientService } from "../services/patient.service";
import type { CreateContactNoteRequest, UpdateContactNoteRequest } from "../types/patient.types";

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
      ],
    }),
    onError: onError({ errorMessage: "Erro ao enviar arquivo" }),
  });
}
