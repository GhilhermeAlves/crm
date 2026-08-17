import api from "@/lib/api";

export interface AiSuggestionResponse {
  conversationId: string;
  suggestion: string;
  provider: string;
}

export const AiService = {
  async suggest(conversationId: string): Promise<AiSuggestionResponse> {
    const response = await api.get<AiSuggestionResponse>(`/ai/suggestions/${conversationId}`);
    return response.data;
  },
};