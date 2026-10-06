import { describe, expect, it } from "vitest";
import {
  isAdminLevelRole,
  memberStatusLabel,
  roleLabel,
} from "@/features/identity/members/lib/labels";

describe("members-labels (papéis e status de membro)", () => {
  it("traduz papéis conhecidos", () => {
    expect(roleLabel("ADMIN")).toBe("Administrador");
    expect(roleLabel("MANAGER")).toBe("Gerente");
  });

  it("quebra nomes de papéis desconhecidos por underline", () => {
    expect(roleLabel("AREA_SUPERVISOR")).toBe("AREA SUPERVISOR");
  });

  it("devolve travessão para papel ausente", () => {
    expect(roleLabel(null)).toBe("—");
    expect(roleLabel(undefined)).toBe("—");
  });

  it("trata ADMIN/OWNER/SUPER_ADMIN como nível administrador", () => {
    expect(isAdminLevelRole("ADMIN")).toBe(true);
    expect(isAdminLevelRole("OWNER")).toBe(true);
    expect(isAdminLevelRole("SUPER_ADMIN")).toBe(true);
    expect(isAdminLevelRole("MANAGER")).toBe(false);
    expect(isAdminLevelRole("AGENT")).toBe(false);
    expect(isAdminLevelRole(null)).toBe(false);
  });

  it("traduz status de membership", () => {
    expect(memberStatusLabel("ACTIVE")).toBe("Ativo");
    expect(memberStatusLabel("REMOVED")).toBe("Inativo");
  });
});
