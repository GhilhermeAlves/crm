import { describe, expect, it } from "vitest";
import {
  computeMembershipLimits,
  type MembershipLimits,
} from "@/features/identity/members/lib/membership-limits";

const member = (role: string) => ({ role });

function limits(role: string | null, active: { role: string }[]): MembershipLimits {
  return computeMembershipLimits(role, active);
}

describe("membership-limits (condições espelhadas do backend)", () => {
  it("ADMIN único ativo é o último admin", () => {
    expect(limits("ADMIN", [member("ADMIN"), member("AGENT")])).toEqual({
      isLastAdmin: true,
      isLastMember: false,
    });
  });

  it("ADMIN com outro admin ativo NÃO é o último admin", () => {
    expect(limits("ADMIN", [member("ADMIN"), member("OWNER")])).toEqual({
      isLastAdmin: false,
      isLastMember: false,
    });
  });

  it("OWNER e SUPER_ADMIN contam como nível admin", () => {
    expect(limits("OWNER", [member("OWNER")])).toEqual({
      isLastAdmin: true,
      isLastMember: true,
    });
    expect(limits("SUPER_ADMIN", [member("SUPER_ADMIN"), member("VIEWER")])).toEqual({
      isLastAdmin: true,
      isLastMember: false,
    });
  });

  it("AGENT nunca é tratado como último admin, mesmo sozinho", () => {
    expect(limits("AGENT", [member("AGENT")])).toEqual({
      isLastAdmin: false,
      isLastMember: true,
    });
  });

  it("único membro ativo → último membro (impede desativação)", () => {
    expect(limits("ADMIN", [member("ADMIN")])).toEqual({
      isLastAdmin: true,
      isLastMember: true,
    });
  });

  it("membro sem papel informado é conservador (não admin)", () => {
    expect(limits(null, [member("AGENT")])).toEqual({
      isLastAdmin: false,
      isLastMember: true,
    });
  });
});
