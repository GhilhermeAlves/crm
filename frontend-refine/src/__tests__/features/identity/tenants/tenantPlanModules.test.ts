import { describe, it, expect } from "vitest";
import { tenantPlanModules } from "@/features/identity/tenants/schemas/tenant.schema";

describe("tenantPlanModules (espelho do backend)", () => {
  it("Essencial é a parte clínica básica, sem comercial", () => {
    expect(tenantPlanModules.essential).toEqual([
      "PATIENTS",
      "AGENDA",
      "AGENDA_SETTINGS",
      "CATALOG",
      "DOCUMENTS",
      "TASKS",
      "STORAGE",
    ]);
    expect(tenantPlanModules.essential).not.toContain("CRM");
  });

  it("cada plano inclui o anterior e o Enterprise libera tudo", () => {
    const order = ["essential", "starter", "professional", "business", "enterprise"] as const;
    for (let i = 1; i < order.length; i++) {
      const prev = tenantPlanModules[order[i - 1]];
      expect(tenantPlanModules[order[i]]).toEqual(expect.arrayContaining(prev));
    }
    expect(new Set(tenantPlanModules.enterprise).size).toBe(21);
  });
});
