import { describe, it, expect } from "vitest";
import {
  countQuestions,
  isSelectable,
  type AnamnesisSection,
} from "@/features/masterdata/anamnesis/types/anamnesis.types";

describe("anamnesis types helpers", () => {
  it("isSelectable reconhece seleção única e múltipla", () => {
    expect(isSelectable("SINGLE_SELECT")).toBe(true);
    expect(isSelectable("MULTI_SELECT")).toBe(true);
    expect(isSelectable("YES_NO")).toBe(false);
    expect(isSelectable("SHORT_TEXT")).toBe(false);
  });

  it("countQuestions soma as perguntas de todas as seções", () => {
    const sections = [
      { questions: [{}, {}] },
      { questions: [{}] },
      { questions: [] },
    ] as unknown as AnamnesisSection[];

    expect(countQuestions(sections)).toBe(3);
  });
});
