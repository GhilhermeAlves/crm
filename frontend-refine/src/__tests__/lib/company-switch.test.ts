import { describe, expect, it } from "vitest";
import { pathAfterCompanySwitch } from "@/lib/company-switch";

const ID = "c6ac8e73-6b42-4bea-806b-d00324d1cf6e";

describe("pathAfterCompanySwitch", () => {
  it("volta da ficha do paciente (e das abas) para a lista", () => {
    expect(pathAfterCompanySwitch(`/patients/${ID}`)).toBe("/patients");
    expect(pathAfterCompanySwitch(`/patients/${ID}/orcamentos`)).toBe("/patients");
  });

  it("vale para outros detalhes, inclusive aninhados", () => {
    expect(pathAfterCompanySwitch(`/leads/${ID}`)).toBe("/leads");
    expect(pathAfterCompanySwitch(`/finance/control/${ID}`)).toBe("/finance/control");
  });

  it("fica onde está em páginas sem registro", () => {
    expect(pathAfterCompanySwitch("/patients")).toBeNull();
    expect(pathAfterCompanySwitch("/patients/new")).toBeNull();
    expect(pathAfterCompanySwitch("/dashboard")).toBeNull();
    expect(pathAfterCompanySwitch(null)).toBeNull();
  });
});
