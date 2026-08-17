import api from "@/lib/api";
import type { Notification } from "../types/notification.types";

const BASE = "/companies";

export const NotificationService = {
  async list(companyId: string): Promise<Notification[]> {
    const response = await api.get<Notification[]>(`${BASE}/${companyId}/notifications`);
    return response.data;
  },

  async unreadCount(companyId: string): Promise<number> {
    const response = await api.get<number>(`${BASE}/${companyId}/notifications/unread-count`);
    return response.data;
  },

  async markAsRead(companyId: string, id: string): Promise<Notification> {
    const response = await api.post<Notification>(`${BASE}/${companyId}/notifications/${id}/read`);
    return response.data;
  },

  async markAllRead(companyId: string): Promise<void> {
    await api.post(`${BASE}/${companyId}/notifications/read-all`);
  },
};