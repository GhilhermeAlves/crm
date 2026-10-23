import { describe, it, expect } from "vitest";
import {
  faceLayout,
  isUpper,
  toothKind,
} from "@/features/clinical/procedures/components/toothShapes";

describe("toothShapes", () => {
  it("classifica o tipo anatômico pelo número FDI", () => {
    expect(toothKind(11, "PERMANENT")).toBe("incisor");
    expect(toothKind(13, "PERMANENT")).toBe("canine");
    expect(toothKind(24, "PERMANENT")).toBe("premolar");
    expect(toothKind(36, "PERMANENT")).toBe("molar");
    expect(toothKind(54, "DECIDUOUS")).toBe("molar");
  });

  it("separa arcada superior e inferior", () => {
    expect(isUpper(18)).toBe(true);
    expect(isUpper(61)).toBe(true);
    expect(isUpper(31)).toBe(false);
    expect(isUpper(85)).toBe(false);
  });

  it("vira a mesial para a linha média e a vestibular para fora", () => {
    expect(faceLayout(11)).toMatchObject({ right: "M", left: "D", top: "V", bottom: "L" });
    expect(faceLayout(21)).toMatchObject({ left: "M", right: "D", top: "V" });
    expect(faceLayout(46)).toMatchObject({ right: "M", top: "L", bottom: "V" });
    expect(faceLayout(36)).toMatchObject({ left: "M", bottom: "V" });
  });
});
