import { describe, expect, it } from "vitest";
import { measureGray } from "../utils/imageQuality";

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
