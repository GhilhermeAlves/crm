import api from "@/lib/api";
import type {
  Contact,
  CreateContactRequest,
  Customer360,
  UpdateContactRequest,
} from "../types/contact.types";

const BASE = "/companies";

export const ContactService = {
  async list(companyId: string): Promise<Contact[]> {
    const response = await api.get<Contact[]>(`${BASE}/${companyId}/contacts`);
    return response.data;
  },

  async findById(companyId: string, id: string): Promise<Contact> {
    const response = await api.get<Contact>(`${BASE}/${companyId}/contacts/${id}`);
    return response.data;
  },

  async create(companyId: string, data: CreateContactRequest): Promise<Contact> {
    const response = await api.post<Contact>(`${BASE}/${companyId}/contacts`, data);
    return response.data;
  },

  async update(
    companyId: string,
    id: string,
    data: UpdateContactRequest
  ): Promise<Contact> {
    const response = await api.put<Contact>(
      `${BASE}/${companyId}/contacts/${id}`,
      data
    );
    return response.data;
  },

  async delete(companyId: string, id: string): Promise<void> {
    await api.delete(`${BASE}/${companyId}/contacts/${id}`);
  },

  /** Customer 360 (Sprint 13): visão consolidada do contato. */
  async customer360(companyId: string, id: string): Promise<Customer360> {
    const response = await api.get<Customer360>(
      `${BASE}/${companyId}/contacts/${id}/360`
    );
    return response.data;
  },
};