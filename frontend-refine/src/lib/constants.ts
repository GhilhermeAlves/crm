export const API_VERSION = "v1";

export const ROUTES = {
  HOME: "/",
  LOGIN: "/login",
  REGISTER: "/register",
  FORGOT_PASSWORD: "/forgot-password",
  RESET_PASSWORD: "/reset-password",
  DASHBOARD: "/crm",
  CRM: "/crm",
  ONBOARDING: "/onboarding",
  TENANTS: "/tenants",
  TENANTS_NEW: "/tenants/new",
  USERS: "/users",
  USERS_NEW: "/users/new",
  MEMBERS: "/members",
  INVITATIONS: "/invitations",
  PROFILE: "/profile",
  ROLES: "/settings/roles",
  ROLES_NEW: "/settings/roles/new",
  PERMISSIONS: "/settings/permissions",
  LEADS: "/leads",
  LEADS_NEW: "/leads/new",
  CONTACTS: "/contacts",
  PIPELINE: "/pipeline",
  CAMPAIGNS: "/campaigns",
  CAMPAIGNS_NEW: "/campaigns/new",
  REPORTS: "/reports",
  SETTINGS_USERS: "/settings/users",
  SETTINGS_ROLES: "/settings/roles",
  SETTINGS_AGENDA: "/settings/agenda",
  SETTINGS_AGENT_CONFIG: "/settings/agent-config",
  SETTINGS_COMPANY: "/settings/company",
  SETTINGS_COMPANY_PREFERENCES: "/settings/company/preferences",
  SETTINGS_COMPANY_USERS_NEW: "/settings/company/users/new",
  SETTINGS_COMPANY_USERS: "/settings/company/users",
  AUDIT: "/audit",
  AGENDA: "/agenda",
  TASKS: "/tasks",
  CATALOG: "/catalog",
  ACTIVITIES: "/activities",
  WORKFLOWS: "/workflows",
  INBOX: "/inbox",
  CHANNELS: "/channels",
  FOLLOW_UP_SEQUENCES: "/follow-up-sequences",
  STORAGE: "/storage",
  NOTIFICATIONS: "/notifications",
  ASSISTANT: "/assistant",
  DESIGN_SYSTEM: "/design-system",
  SETTINGS_DOCUMENTS: "/settings/documents",
  SETTINGS_DOCUMENTS_ANAMNESIS: "/settings/documents/anamnesis",
  SETTINGS_DOCUMENTS_PRESCRIPTIONS: "/settings/documents/prescriptions",
} as const;

export const PUBLIC_ROUTES = [
  ROUTES.LOGIN,
  ROUTES.REGISTER,
  ROUTES.FORGOT_PASSWORD,
  ROUTES.RESET_PASSWORD,
] as const;

export const AUTH_ROUTES = [
  ROUTES.LOGIN,
  ROUTES.REGISTER,
  ROUTES.FORGOT_PASSWORD,
  ROUTES.RESET_PASSWORD,
] as const;

/** Página oficial do convite. O token vai só no caminho da URL (nunca em storage). */
export function invitationPath(token: string): string {
  return `/convite/${encodeURIComponent(token)}`;
}
