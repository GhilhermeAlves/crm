export const API_VERSION = "v1";

export const ROUTES = {
  HOME: "/",
  LOGIN: "/login",
  REGISTER: "/register",
  FORGOT_PASSWORD: "/forgot-password",
  RESET_PASSWORD: "/reset-password",
  DASHBOARD: "/dashboard",
  ONBOARDING: "/onboarding",
  TENANTS: "/tenants",
  TENANTS_NEW: "/tenants/new",
  USERS: "/users",
  USERS_NEW: "/users/new",
  PROFILE: "/profile",
  ROLES: "/roles",
  ROLES_NEW: "/roles/new",
  PERMISSIONS: "/permissions",
  LEADS: "/leads",
  CONTACTS: "/contacts",
  PIPELINE: "/pipeline",
  CHAT: "/chat",
  CAMPAIGNS: "/campaigns",
  REPORTS: "/reports",
  SETTINGS: "/settings",
  AUDIT: "/audit",
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
