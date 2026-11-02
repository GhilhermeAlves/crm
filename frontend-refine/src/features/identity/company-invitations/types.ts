import type { InvitationStatus } from "@/features/identity/invitations/types/invitation.types";

/** POST /company-invitations (exclusivo CRM_ADMIN). */
export type CreateCompanyInvitationRequest = {
  email: string;
  name?: string;
  segment: string;
  enabledModules: string[];
  plan: string;
  maxUsers: number;
  /** Default `true`. `false` = não envia e-mail; o link fica para cópia/WhatsApp. */
  sendEmail?: boolean;
  /** Dados da empresa adiantados pelo admin; o convidado confere ao aceitar. */
  company?: CompanyPrefill;
};

/** Dados adiantados no convite: todos opcionais (só os preenchidos são enviados). */
export type CompanyPrefill = Partial<InvitedCompanyDetails>;

export type CompanyInvitation = {
  id: string;
  email: string;
  inviteeName: string | null;
  segment: string;
  enabledModules: string[];
  plan: string;
  maxUsers: number;
  status: InvitationStatus;
  companyId: string | null;
  invitedBy: string | null;
  expiresAt: string;
  createdAt: string;
  company: CompanyPrefill | null;
};

export type CompanyInvitationLink = {
  invitation: CompanyInvitation;
  url: string;
};

/** GET /company-invitations/preview (público). */
export type CompanyInvitationPreview = {
  email: string;
  inviteeName: string | null;
  segment: string;
  enabledModules: string[];
  plan: string;
  maxUsers: number;
  status: InvitationStatus;
  expiresAt: string;
  hasAccount: boolean;
  company: CompanyPrefill | null;
};

/** Dados da empresa preenchidos pelo convidado. */
export type InvitedCompanyDetails = {
  legalName: string;
  tradingName: string;
  /** CNPJ ou CPF; opcional. */
  cnpj?: string;
  email: string;
  phone: string;
  addressZipCode: string;
  addressStreet: string;
  addressNumber: string;
  addressComplement?: string;
  addressNeighborhood: string;
  addressCity: string;
  addressState: string;
};
