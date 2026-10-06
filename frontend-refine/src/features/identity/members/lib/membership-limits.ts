import { isAdminLevelRole } from "./labels";

export type MembershipLimits = {
  /** O membro é o único administrador ativo → não pode ser rebaixado/desativado. */
  isLastAdmin: boolean;
  /** É o único membro ativo → não pode ser desativado. */
  isLastMember: boolean;
};

/**
 * Espelha MembershipService.assertNotLastAdmin/assertNotLastMember para a UI:
 * a decisão final continua sendo do backend, mas a interface evita oferecer
 * uma ação que o backend rejeitaria com 400.
 */
export function computeMembershipLimits(
  memberRole: string | null,
  activeMembers: { role: string }[],
): MembershipLimits {
  const activeAdminCount = activeMembers.filter((m) => isAdminLevelRole(m.role)).length;
  return {
    isLastAdmin: isAdminLevelRole(memberRole) && activeAdminCount <= 1,
    isLastMember: activeMembers.length <= 1,
  };
}
