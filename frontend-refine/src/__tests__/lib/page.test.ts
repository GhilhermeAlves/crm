import { describe, expect, it } from "vitest";
import { normalizePage, toSpringParams } from "@/types/page";

describe("normalizePage", () => {
  it("mantém o DTO do CRM", () => {
    const raw = { content: [1, 2], page: 1, pageSize: 2, totalElements: 5, totalPages: 3 };
    expect(normalizePage(raw)).toEqual(raw);
  });

  it("converte PageImpl do Spring", () => {
    const raw = { content: ["a"], number: 2, size: 10, totalElements: 21, totalPages: 3 };
    expect(normalizePage(raw)).toEqual({
      content: ["a"],
      page: 2,
      pageSize: 10,
      totalElements: 21,
      totalPages: 3,
    });
  });

  it("converte VIA_DTO com page aninhado", () => {
    const raw = { content: ["a"], page: { number: 0, size: 15, totalElements: 30, totalPages: 2 } };
    expect(normalizePage(raw)).toMatchObject({ page: 0, pageSize: 15, totalElements: 30 });
  });

  it("aceita array simples e vazio", () => {
    expect(normalizePage([1, 2, 3])).toMatchObject({ totalElements: 3, totalPages: 1 });
    expect(normalizePage(undefined)).toMatchObject({ content: [], totalPages: 0 });
  });
});

describe("toSpringParams", () => {
  it("gera page, size e sort=campo,ordem", () => {
    const p = toSpringParams({
      page: 0,
      size: 20,
      sort: [
        { field: "name", order: "asc" },
        { field: "id", order: "desc" },
      ],
    });
    expect(p.toString()).toBe("page=0&size=20&sort=name%2Casc&sort=id%2Cdesc");
  });
});
