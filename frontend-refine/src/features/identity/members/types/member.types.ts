/**
 * Status da membership (espelha MembershipStatus do backend).
 * ACTIVE  → aba Ativos
 * REMOVED → aba Inativos (soft delete; o usuário continua existindo)
 * PENDING → legado do fluxo antigo de convite; o fluxo atual não produz
 */
export type MemberStatus = "ACTIVE" | "REMOVED" | "PENDING";

export type Member = {
  userId: string;
  name: string;
  email: string;
  role: string;
  status: MemberStatus;
  joinedAt: string | null;
  specialty?: string | null;
  /** Dono da plataforma (SUPER_ADMIN): todas as permissões em qualquer empresa. */
  platformAdmin?: boolean | null;
  /** Tem ao menos um perfil nesta empresa. */
  hasProfile?: boolean | null;
};

export type InviteMemberRequest = {
  firstName: string;
  lastName: string;
  email: string;
  department?: string;
  jobTitle?: string;
};
