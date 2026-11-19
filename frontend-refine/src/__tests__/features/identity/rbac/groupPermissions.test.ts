import { describe, expect, it } from "vitest";
import { groupPermissions, originOf } from "@/features/identity/rbac/utils/groupPermissions";

describe("groupPermissions", () => {
  it("agrupa por prefixo antes de ':'", () => {
    const g = groupPermissions([
      { id: "1", name: "finance:read" },
      { id: "2", name: "finance:manage" },
      { id: "3", name: "patient:read" },
    ]);
    expect(Object.keys(g)).toEqual(["finance", "patient"]);
  });

  it("origem: DENY > ALLOW > perfil > nenhuma", () => {
    expect(originOf("a", new Set(["a"]), new Map([["a", "DENY"]]))).toBe("deny");
    expect(originOf("a", new Set(), new Map([["a", "ALLOW"]]))).toBe("allow");
    expect(originOf("a", new Set(["a"]), new Map())).toBe("role");
    expect(originOf("a", new Set(), new Map())).toBe("none");
  });
});
