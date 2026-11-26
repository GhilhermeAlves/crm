import { describe, expect, it } from "vitest";
import { missingRequired, visibleQuestions } from "../components/QuizForm";
import type { QuizQuestion } from "../types/trichology.types";
import { measureGray } from "../utils/imageQuality";

const questions: QuizQuestion[] = [
  { id: "sex", text: "Sexo", type: "SINGLE", required: true, options: [], showIf: null },
  {
    id: "postpartum",
    text: "Parto",
    type: "BOOLEAN",
    required: true,
    options: [],
    showIf: { dependsOn: "sex", values: ["F"] },
  },
  { id: "notes", text: "Notas", type: "TEXT", required: false, options: [], showIf: null },
];

describe("quiz", () => {
  it("mostra pergunta condicional só quando a dependência bate", () => {
    expect(visibleQuestions(questions, { sex: "M" }).map((q) => q.id)).toEqual(["sex", "notes"]);
    expect(visibleQuestions(questions, { sex: "F" }).map((q) => q.id)).toContain("postpartum");
  });

  it("aponta obrigatórias visíveis sem resposta", () => {
    expect(missingRequired(questions, {}).map((q) => q.id)).toEqual(["sex"]);
    expect(missingRequired(questions, { sex: "F" }).map((q) => q.id)).toEqual(["postpartum"]);
    expect(missingRequired(questions, { sex: "F", postpartum: false })).toHaveLength(0);
  });
});

function image(width: number, height: number, pixel: (x: number, y: number) => number) {
  const gray = new Uint8ClampedArray(width * height);
  for (let y = 0; y < height; y++)
    for (let x = 0; x < width; x++) gray[y * width + x] = pixel(x, y);
  return gray;
}

describe("checagem de qualidade", () => {
  it("imagem lisa é desfocada", () => {
    expect(
      measureGray(
        image(40, 40, () => 128),
        40,
        40,
      ).status,
    ).toBe("BLURRED");
  });

  it("imagem com detalhe e boa exposição passa", () => {
    const report = measureGray(
      image(40, 40, (x, y) => ((x + y) % 2 ? 90 : 170)),
      40,
      40,
    );
    expect(report.status).toBe("OK");
  });

  it("imagem escura e imagem estourada", () => {
    expect(
      measureGray(
        image(40, 40, (x) => (x % 2 ? 10 : 30)),
        40,
        40,
      ).status,
    ).toBe("DARK");
    expect(
      measureGray(
        image(40, 40, (x) => (x % 2 ? 255 : 120)),
        40,
        40,
      ).status,
    ).toBe("GLARE");
  });
});
