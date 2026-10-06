import api from "@/lib/api";
import type {
  CreateInvitationRequest,
  Invitation,
  InvitationLink,
  InvitationPreview,
  InvitationRegisterRequest,
} from "../types/invitation.types";

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
  /**
   * POST /companies/{id}/invitations/{invId}/regenerate?send=
   * `send=true` → Reenviar (novo token + novo e-mail);
   * `send=false` → Copiar link (só devolve a URL).
   * Em ambos os casos o link anterior deixa de funcionar.
   */
  async regenerate(
    companyId: string,
    invitationId: string,
    send: boolean,
  ): Promise<InvitationLink> {
    const response = await api.post<InvitationLink>(
      `${BASE}/${companyId}/invitations/${invitationId}/regenerate`,
      null,
      { params: { send } },
    );
    return response.data;
  },
  async preview(token: string): Promise<InvitationPreview> {
    const response = await api.get<InvitationPreview>("/invitations/preview", {
      params: { token },
    });
    return response.data;
  },
  async register(data: InvitationRegisterRequest): Promise<Invitation> {
    // Monta o corpo explicitamente: nunca repassar campos extras (e-mail etc.).
    const body: InvitationRegisterRequest = {
      token: data.token,
      name: data.name,
      password: data.password,
    };
    const response = await api.post<Invitation>("/invitations/register", body);
    return response.data;
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
