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
  ROLES: "/roles",
  ROLES_NEW: "/roles/new",
  PERMISSIONS: "/permissions",
  LEADS: "/leads",
  LEADS_NEW: "/leads/new",
  CONTACTS: "/contacts",
  PIPELINE: "/pipeline",
  CAMPAIGNS: "/campaigns",
  CAMPAIGNS_NEW: "/campaigns/new",
  REPORTS: "/reports",
  SETTINGS_USERS: "/settings/users",
  SETTINGS_ROLES: "/settings/roles",
  AUDIT: "/audit",
  TASKS: "/tasks",
  ACTIVITIES: "/activities",
  WORKFLOWS: "/workflows",
  INBOX: "/inbox",
  CHANNELS: "/channels",
  STORAGE: "/storage",
  NOTIFICATIONS: "/notifications",
  ASSISTANT: "/assistant",
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
