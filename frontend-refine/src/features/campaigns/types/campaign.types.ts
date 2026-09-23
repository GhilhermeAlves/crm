export const CAMPAIGN_STATUSES = [
  "DRAFT",
  "SCHEDULED",
  "RUNNING",
  "PAUSED",
  "COMPLETED",
  "CANCELLED",
] as const;
export type CampaignStatus = (typeof CAMPAIGN_STATUSES)[number];

export const AUDIENCE_TYPES = ["CONTACTS", "LEADS"] as const;
export type AudienceType = (typeof AUDIENCE_TYPES)[number];

export const CHANNEL_TYPES = ["WHATSAPP"] as const;
export type ChannelType = (typeof CHANNEL_TYPES)[number];

export type CampaignChannelInfo = {
  id: string;
  channelType: string;
  providerChannelId: string;
  templateId: string;
  templateVersion: number;
};

export type Campaign = {
  id: string;
  companyId: string;
  name: string;
  description: string | null;
  status: CampaignStatus;
  audienceType: AudienceType;
  audienceCriteria: string | null;
  estimatedRecipients: number;
  scheduledAt: string | null;
  timezone: string;
  startedAt: string | null;
  completedAt: string | null;
  createdBy: string | null;
  channelId: string | null;
  channelType: string | null;
  providerChannelId: string | null;
  templateId: string | null;
  templateVersion: number | null;
  createdAt: string;
  updatedAt: string;
};

export type MessageTemplate = {
  id: string;
  companyId: string;
  name: string;
  channelType: string;
  subject: string | null;
  body: string;
  variables: string[];
  status: "ACTIVE" | "ARCHIVED";
  version: number;
  externalTemplateId: string | null;
  createdAt: string;
  updatedAt: string;
};

export type CampaignExecution = {
  id: string;
  campaignId: string;
  status: "RUNNING" | "PAUSED" | "COMPLETED" | "CANCELLED";
  totalRecipients: number;
  processedCount: number;
  failedCount: number;
  startedAt: string | null;
  finishedAt: string | null;
};

export type PageResponse<T> = {
  content: T[];
  page: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
};

export type ListCampaignsParams = {
  page?: number;
  pageSize?: number;
  status?: CampaignStatus;
  audienceType?: AudienceType;
};

export type CreateCampaignRequest = {
  name: string;
  description?: string;
  audienceType: AudienceType;
  audienceCriteria?: string;
  timezone?: string;
};

export type UpdateCampaignRequest = {
  name?: string;
  description?: string;
};

export type AttachChannelRequest = {
  channelType: string;
  providerChannelId: string;
  templateId: string;
};

export type ScheduleCampaignRequest = {
  scheduledAt: string;
};
