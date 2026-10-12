import { describe, expect, it } from "vitest";
import { CATALOG_CATEGORIES } from "@/features/masterdata/catalog/types/catalog.types";

describe("catalog.types", () => {
  it("lista as categorias fixas de produtos", () => {
    expect(CATALOG_CATEGORIES).toEqual([
      "EPI",
      "INSTRUMENTO",
      "MATERIAL_DE_CONSUMO",
      "MEDICAMENTO",
      "OUTROS",
    ]);
  });
});
