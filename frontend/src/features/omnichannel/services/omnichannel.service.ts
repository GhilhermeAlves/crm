import api from "@/lib/api";
import type {
  Channel,
  ChannelRequest,
  Conversation,
  ConversationDetail,
  Message,
  Page,
} from "../types/omnichannel.types";

const BASE = "/omnichannel";

export const OmnichannelService = {
  // Canais
  async listChannels(): Promise<Channel[]> {
    const response = await api.get<Channel[]>(`${BASE}/channels`);
    return response.data;
  },

  async createChannel(data: ChannelRequest): Promise<Channel> {
    const response = await api.post<Channel>(`${BASE}/channels`, data);
    return response.data;
  },

  async updateChannel(id: string, data: ChannelRequest): Promise<Channel> {
    const response = await api.put<Channel>(`${BASE}/channels/${id}`, data);
    return response.data;
  },

  async setChannelStatus(
    id: string,
    status: Channel["status"],
  ): Promise<Channel> {
    const response = await api.patch<Channel>(`${BASE}/channels/${id}/status`, {
      status,
    });
    return response.data;
  },

  async deleteChannel(id: string): Promise<void> {
    await api.delete(`${BASE}/channels/${id}`);
  },

  // Inbox
  async listConversations(
    page = 0,
    pageSize = 20,
  ): Promise<Page<Conversation>> {
    const response = await api.get<Page<Conversation>>(`${BASE}/inbox`, {
      params: { page, pageSize },
    });
    return response.data;
  },

  async getConversation(
    conversationId: string,
    page = 0,
    pageSize = 30,
  ): Promise<ConversationDetail> {
    const response = await api.get<ConversationDetail>(
      `${BASE}/inbox/${conversationId}`,
      {
        params: { page, pageSize },
      },
    );
    return response.data;
  },

  async sendMessage(conversationId: string, body: string): Promise<Message> {
    const response = await api.post<Message>(
      `${BASE}/inbox/${conversationId}/messages`,
      { body },
    );
    return response.data;
  },

  async markRead(conversationId: string): Promise<void> {
    await api.post(`${BASE}/inbox/${conversationId}/read`);
  },
};
