import api from "@/lib/api";
import type { CreateInvitationRequest, Invitation } from "../types/invitation.types";

const BASE = "/companies";

export const InvitationService = {
  async list(companyId: string, status?: string): Promise<Invitation[]> {
    const response = await api.get<Invitation[]>(`${BASE}/${companyId}/invitations`, {
      params: status ? { status } : undefined,
    });
    return response.data;
  },
  async create(companyId: string, data: CreateInvitationRequest): Promise<Invitation> {
    const response = await api.post<Invitation>(`${BASE}/${companyId}/invitations`, data);
    return response.data;
  },
  async revoke(companyId: string, invitationId: string): Promise<void> {
    await api.delete(`${BASE}/${companyId}/invitations/${invitationId}`);
  },
  async accept(token: string): Promise<Invitation> {
    const response = await api.post<Invitation>("/invitations/accept", null, {
      params: { token },
    });
    return response.data;
  },
  async decline(token: string): Promise<Invitation> {
    const response = await api.post<Invitation>("/invitations/decline", null, {
      params: { token },
    });
    return response.data;
  },
};