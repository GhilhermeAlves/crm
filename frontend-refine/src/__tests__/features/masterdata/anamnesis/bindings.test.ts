import { describe, expect, it } from "vitest";
import {
  bindingsFor,
  invalidOptionValues,
  usedBindings,
} from "@/features/masterdata/anamnesis/lib/bindings";

describe("bindingsFor", () => {
  it("só oferece vínculos compatíveis com o tipo", () => {
    expect(bindingsFor("DATE", true).map((b) => b.value)).toEqual(["PERSON_BIRTH_DATE"]);
  });

  it("esconde vínculos do exame sem o módulo Tricologia", () => {
    const options = bindingsFor("SINGLE_SELECT", false);
    expect(options.length).toBeGreaterThan(0);
    expect(options.every((b) => b.group === "PERSON")).toBe(true);
  });

  it("mostra vínculos do exame com o módulo Tricologia", () => {
    expect(bindingsFor("SINGLE_SELECT", true).map((b) => b.value)).toContain("TRICHO_ONSET");
  });
});

describe("usedBindings", () => {
  it("junta os vínculos de todas as seções", () => {
    const used = usedBindings([
      { questions: [{ binding: "PERSON_NAME" }] },
      { questions: [{ binding: null }, {}] },
    ]);
    expect(Array.from(used)).toEqual(["PERSON_NAME"]);
  });
});

describe("invalidOptionValues", () => {
  it("aponta opções fora dos códigos do vínculo", () => {
    expect(invalidOptionValues("TRICHO_ONSET", ["LT_3M", "Ontem"])).toEqual(["Ontem"]);
  });

  it("aceita qualquer valor em vínculo sem códigos ou sem vínculo", () => {
    expect(invalidOptionValues("PERSON_NAME", ["x"])).toEqual([]);
    expect(invalidOptionValues(null, ["x"])).toEqual([]);
  });
});
