import api from "@/lib/api";
import type {
  CompanyInvitation,
  CompanyInvitationLink,
  CompanyInvitationPreview,
  CreateCompanyInvitationRequest,
  InvitedCompanyDetails,
} from "./types";

const BASE = "/company-invitations";

/** Convite de criação de empresa: administração (CRM_ADMIN) e fluxo público. */
export const CompanyInvitationService = {
  async list(): Promise<CompanyInvitation[]> {
    const response = await api.get<CompanyInvitation[]>(BASE);
    return response.data;
  },
  async create(data: CreateCompanyInvitationRequest): Promise<CompanyInvitationLink> {
    const response = await api.post<CompanyInvitationLink>(BASE, data);
    return response.data;
  },
  async revoke(id: string): Promise<void> {
    await api.delete(`${BASE}/${id}`);
  },
  /** Novo link; o anterior deixa de valer. */
  async regenerate(id: string): Promise<CompanyInvitationLink> {
    const response = await api.post<CompanyInvitationLink>(`${BASE}/${id}/regenerate`);
    return response.data;
  },
  async preview(token: string): Promise<CompanyInvitationPreview> {
    const response = await api.get<CompanyInvitationPreview>(`${BASE}/preview`, {
      params: { token },
    });
    return response.data;
  },
  async register(data: {
    token: string;
    name: string;
    password: string;
    company: InvitedCompanyDetails;
  }): Promise<CompanyInvitation> {
    const response = await api.post<CompanyInvitation>(`${BASE}/register`, data);
    return response.data;
  },
  async accept(token: string, company: InvitedCompanyDetails): Promise<CompanyInvitation> {
    const response = await api.post<CompanyInvitation>(`${BASE}/accept`, { token, company });
    return response.data;
  },
};
