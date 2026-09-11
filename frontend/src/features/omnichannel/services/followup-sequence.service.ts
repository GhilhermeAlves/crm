import api from "@/lib/api";
import type { Page } from "../types/omnichannel.types";
import type {
  FollowUpSequence,
  FollowUpSequenceRequest,
} from "../types/followup-sequence.types";

const BASE = "/omnichannel/follow-up-sequences";

export const FollowUpSequenceService = {
  async list(page = 0, pageSize = 20): Promise<Page<FollowUpSequence>> {
    const response = await api.get<Page<FollowUpSequence>>(BASE, {
      params: { page, pageSize },
    });
    return response.data;
  },

  async get(sequenceId: string): Promise<FollowUpSequence> {
    const response = await api.get<FollowUpSequence>(`${BASE}/${sequenceId}`);
    return response.data;
  },

  async create(data: FollowUpSequenceRequest): Promise<FollowUpSequence> {
    const response = await api.post<FollowUpSequence>(BASE, data);
    return response.data;
  },

  async update(
    sequenceId: string,
    data: FollowUpSequenceRequest,
  ): Promise<FollowUpSequence> {
    const response = await api.put<FollowUpSequence>(
      `${BASE}/${sequenceId}`,
      data,
    );
    return response.data;
  },

  async delete(sequenceId: string): Promise<void> {
    await api.delete(`${BASE}/${sequenceId}`);
  },

  async activate(sequenceId: string): Promise<FollowUpSequence> {
    const response = await api.post<FollowUpSequence>(
      `${BASE}/${sequenceId}/activate`,
    );
    return response.data;
  },

  async deactivate(sequenceId: string): Promise<FollowUpSequence> {
    const response = await api.post<FollowUpSequence>(
      `${BASE}/${sequenceId}/deactivate`,
    );
    return response.data;
  },
};
