export const LEAD_STATUSES = [
  "NEW",
  "CONTACTED",
  "QUALIFIED",
  "UNQUALIFIED",
  "CONVERTED",
  "LOST",
] as const;
export type LeadStatus = (typeof LEAD_STATUSES)[number];

export const LEAD_SOURCES = [
  "WHATSAPP",
  "FORM",
  "API",
  "IMPORT",
  "MANUAL",
] as const;
export type LeadSource = (typeof LEAD_SOURCES)[number];

export const LEAD_CLASSIFICATIONS = [
  "HOT",
  "WARM",
  "COLD",
  "DISQUALIFIED",
] as const;
export type LeadClassification = (typeof LEAD_CLASSIFICATIONS)[number];

export type Lead = {
  id: string;
  companyId: string;
  contactId: string;
  status: LeadStatus;
  score: number;
  classification: LeadClassification | null;
  source: LeadSource;
  campaignId: string | null;
  assignedTo: string | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
};

export type CreateLeadRequest = {
  contactId: string;
  status?: LeadStatus;
  score?: number;
  classification?: LeadClassification;
  source: LeadSource;
  campaignId?: string;
  assignedTo?: string;
  notes?: string;
};

export type UpdateLeadRequest = {
  status?: LeadStatus;
  score?: number;
  classification?: LeadClassification;
  campaignId?: string;
  assignedTo?: string;
  notes?: string;
};

export type PageResponse<T> = {
  content: T[];
  page: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
};

export type ListLeadsParams = {
  page?: number;
  pageSize?: number;
  status?: LeadStatus;
  source?: LeadSource;
  classification?: LeadClassification;
  sortBy?: string;
  sortDirection?: "asc" | "desc";
};