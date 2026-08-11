import api from "@/lib/api";
import type { Member } from "../types/member.types";

const BASE = "/companies";

export const MemberService = {
  async listMembers(companyId: string): Promise<Member[]> {
    const response = await api.get<Member[]>(`${BASE}/${companyId}/members`);
    return response.data;
  },
  async updateRole(
    companyId: string,
    userId: string,
    role: string
  ): Promise<Member> {
    const response = await api.put<Member>(
      `${BASE}/${companyId}/members/${userId}`,
      { role }
    );
    return response.data;
  },
  async removeMember(companyId: string, userId: string): Promise<void> {
    await api.delete(`${BASE}/${companyId}/members/${userId}`);
  },
};
