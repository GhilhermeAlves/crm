import api from "@/lib/api";

export type TelegramConfig = {
  configured: boolean;
  botUsername: string | null;
  enabled: boolean;
  updatedAt: string | null;
};
export type TelegramLink = { userId: string; telegramName: string | null; createdAt: string };
export type LinkCode = { code: string; botUsername: string; expiresInMinutes: number };

const base = (companyId: string) => `/companies/${companyId}/telegram`;

export function errorMessage(error: unknown, fallback: string) {
  return (
    (error as { response?: { data?: { message?: string } } })?.response?.data?.message ?? fallback
  );
}

export const telegramApi = {
  get: (c: string) => api.get<TelegramConfig>(base(c)).then((r) => r.data),
  save: (c: string, token: string) =>
    api.put<TelegramConfig>(base(c), { token }).then((r) => r.data),
  remove: (c: string) => api.delete(base(c)),
  links: (c: string) => api.get<TelegramLink[]>(`${base(c)}/links`).then((r) => r.data),
  revoke: (c: string, userId: string) => api.delete(`${base(c)}/links/${userId}`),
  me: (c: string) =>
    api
      .get<TelegramLink | "">(`${base(c)}/me`)
      .then((r) => (r.status === 204 || !r.data ? null : (r.data as TelegramLink))),
  unlinkMe: (c: string) => api.delete(`${base(c)}/me`),
  linkCode: (c: string) => api.post<LinkCode>(`${base(c)}/link-code`).then((r) => r.data),
};
