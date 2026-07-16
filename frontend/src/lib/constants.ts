export const API_VERSION = "v1";

export const ROUTES = {
  HOME: "/",
  LOGIN: "/login",
  REGISTER: "/register",
  FORGOT_PASSWORD: "/forgot-password",
  DASHBOARD: "/dashboard",
  LEADS: "/leads",
  CONTACTS: "/contacts",
  PIPELINE: "/pipeline",
  CHAT: "/chat",
  CAMPAIGNS: "/campaigns",
  REPORTS: "/reports",
  SETTINGS: "/settings",
} as const;

export const STORAGE_KEYS = {
  ACCESS_TOKEN: "accessToken",
  REFRESH_TOKEN: "refreshToken",
  THEME: "theme",
} as const;
