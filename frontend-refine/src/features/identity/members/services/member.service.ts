import api from "@/lib/api";
import type { Member, MemberStatus } from "../types/member.types";

const BASE = "/companies";

export const MemberService = {
  /**
   * GET /companies/{id}/members[?status=]
   * Sem `status` o backend devolve apenas ACTIVE (comportamento histórico).
   * `REMOVED` é a fonte de dados dos desligados (aba Inativos).
   */
  async listMembers(companyId: string, status?: MemberStatus): Promise<Member[]> {
    const response = await api.get<Member[]>(`${BASE}/${companyId}/members`, {
      params: status ? { status } : undefined,
    });
    return response.data;
  },
  /**
   * O backend responde só com {userId, role, status, joinedAt}: name/email são
   * omitidos (JSON non_null). Não usar a resposta para sobrescrever a lista —
   * o hook apenas invalida ["members"] e a lista é refeita.
   */
  async updateRole(companyId: string, userId: string, role: string): Promise<Member> {
    const response = await api.put<Member>(`${BASE}/${companyId}/members/${userId}`, { role });
    return response.data;
  },
  async removeMember(companyId: string, userId: string): Promise<void> {
    await api.delete(`${BASE}/${companyId}/members/${userId}`);
  },
};
