import api from "@/lib/api";
import type { Appointment } from "@/features/sales/scheduling/types/scheduling.types";
import type { StorageObject } from "@/features/masterdata/storage/types/storage.types";
import type {
  Contact,
  CreateContactRequest,
  UpdateContactRequest,
} from "@/features/masterdata/contacts/types/contact.types";
import type {
  ContactNote,
  CreateContactNoteRequest,
  UpdateContactNoteRequest,
} from "../types/patient.types";

const BASE = "/companies";

// Pacientes só falam com /patients/** — a ficha precisa funcionar mesmo com o
// módulo comercial (Contatos) desligado.
export const PatientService = {
  async list(companyId: string, search?: string): Promise<Contact[]> {
    const response = await api.get<Contact[]>(`${BASE}/${companyId}/patients`, {
      params: search ? { search } : undefined,
    });
    return response.data;
  },

  async findById(companyId: string, patientId: string): Promise<Contact> {
    const response = await api.get<Contact>(`${BASE}/${companyId}/patients/${patientId}`);
    return response.data;
  },

  async create(companyId: string, data: CreateContactRequest): Promise<Contact> {
    const response = await api.post<Contact>(`${BASE}/${companyId}/patients`, data);
    return response.data;
  },

  async update(companyId: string, patientId: string, data: UpdateContactRequest): Promise<Contact> {
    const response = await api.put<Contact>(`${BASE}/${companyId}/patients/${patientId}`, data);
    return response.data;
  },

  async makePatient(companyId: string, contactId: string): Promise<Contact> {
    const response = await api.post<Contact>(
      `${BASE}/${companyId}/contacts/${contactId}/make-patient`,
    );
    return response.data;
  },

  async listAppointments(companyId: string, contactId: string): Promise<Appointment[]> {
    const response = await api.get<Appointment[]>(
      `${BASE}/${companyId}/patients/${contactId}/appointments`,
    );
    return response.data;
  },

  async listNotes(companyId: string, contactId: string): Promise<ContactNote[]> {
    const response = await api.get<ContactNote[]>(
      `${BASE}/${companyId}/patients/${contactId}/notes`,
    );
    return response.data;
  },

  async createNote(
    companyId: string,
    contactId: string,
    data: CreateContactNoteRequest,
  ): Promise<ContactNote> {
    const response = await api.post<ContactNote>(
      `${BASE}/${companyId}/patients/${contactId}/notes`,
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
      `${BASE}/${companyId}/patients/${contactId}/notes/${noteId}`,
      data,
    );
    return response.data;
  },

  async deleteNote(companyId: string, contactId: string, noteId: string): Promise<void> {
    await api.delete(`${BASE}/${companyId}/patients/${contactId}/notes/${noteId}`);
  },

  async listFiles(companyId: string, contactId: string): Promise<StorageObject[]> {
    const response = await api.get<StorageObject[]>(
      `${BASE}/${companyId}/patients/${contactId}/files`,
    );
    return response.data;
  },

  async uploadFile(companyId: string, contactId: string, file: File): Promise<StorageObject> {
    const formData = new FormData();
    formData.append("file", file);
    const response = await api.post<StorageObject>(
      `${BASE}/${companyId}/patients/${contactId}/files`,
      formData,
      { headers: { "Content-Type": null } },
    );
    return response.data;
  },

  async uploadPhoto(companyId: string, contactId: string, file: File): Promise<Contact> {
    const formData = new FormData();
    formData.append("file", file);
    const response = await api.put<Contact>(
      `${BASE}/${companyId}/patients/${contactId}/photo`,
      formData,
      { headers: { "Content-Type": null } },
    );
    return response.data;
  },

  async deletePhoto(companyId: string, contactId: string): Promise<Contact> {
    const response = await api.delete<Contact>(
      `${BASE}/${companyId}/patients/${contactId}/photo`,
    );
    return response.data;
  },

  async fetchPhoto(companyId: string, contactId: string): Promise<string | null> {
    try {
      const response = await api.get(
        `${BASE}/${companyId}/patients/${contactId}/photo`,
        { responseType: "blob" },
      );
      if (response.status === 204 || !response.data) return null;
      return window.URL.createObjectURL(response.data);
    } catch {
      return null;
    }
  },
};
