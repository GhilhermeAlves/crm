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
};

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
};

/** Dados da empresa preenchidos pelo convidado. */
export type InvitedCompanyDetails = {
  legalName: string;
  tradingName: string;
  cnpj: string;
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
