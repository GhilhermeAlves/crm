import api from "@/lib/api";
import type { Appointment } from "@/features/sales/scheduling/types/scheduling.types";
import type { StorageObject } from "@/features/masterdata/storage/types/storage.types";
import type {
  ContactNote,
  CreateContactNoteRequest,
  UpdateContactNoteRequest,
} from "../types/patient.types";

const BASE = "/companies";

export const PatientService = {
  async listAppointments(companyId: string, contactId: string): Promise<Appointment[]> {
    const response = await api.get<Appointment[]>(
      `${BASE}/${companyId}/contacts/${contactId}/appointments`,
    );
    return response.data;
  },

  async listNotes(companyId: string, contactId: string): Promise<ContactNote[]> {
    const response = await api.get<ContactNote[]>(
      `${BASE}/${companyId}/contacts/${contactId}/notes`,
    );
    return response.data;
  },

  async createNote(
    companyId: string,
    contactId: string,
    data: CreateContactNoteRequest,
  ): Promise<ContactNote> {
    const response = await api.post<ContactNote>(
      `${BASE}/${companyId}/contacts/${contactId}/notes`,
      data,
    );
    return response.data;
  },

  async updateNote(
    companyId: string,
    contactId: string,
    noteId: string,
    data: UpdateContactNoteRequest,
  ): Promise<ContactNote> {
    const response = await api.put<ContactNote>(
      `${BASE}/${companyId}/contacts/${contactId}/notes/${noteId}`,
      data,
    );
    return response.data;
  },

  async deleteNote(companyId: string, contactId: string, noteId: string): Promise<void> {
    await api.delete(`${BASE}/${companyId}/contacts/${contactId}/notes/${noteId}`);
  },

  async listFiles(companyId: string, contactId: string): Promise<StorageObject[]> {
    const response = await api.get<StorageObject[]>(
      `${BASE}/${companyId}/contacts/${contactId}/files`,
    );
    return response.data;
  },

  async uploadFile(companyId: string, contactId: string, file: File): Promise<StorageObject> {
    const formData = new FormData();
    formData.append("file", file);
    const response = await api.post<StorageObject>(
      `${BASE}/${companyId}/contacts/${contactId}/files`,
      formData,
      // `null` remove o default `application/json` e deixa o browser montar o
      // multipart/form-data com o boundary correto (ver StorageService.upload).
      { headers: { "Content-Type": null } },
    );
    return response.data;
  },
};
