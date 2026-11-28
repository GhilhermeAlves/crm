import type { ImageQuality } from "../types/trichology.types";

/** Limiares da checagem determinística (doc v1.2, seção 3.1): avisam, não bloqueiam. */
export const QUALITY_THRESHOLDS = {
  /** Variância do Laplaciano abaixo disso: foto desfocada. */
  minSharpness: 60,
  /** Brilho médio (0–255) abaixo disso: foto escura. */
  minBrightness: 45,
  /** Fração de pixels estourados (≥ 250) acima disso: reflexo. */
  maxGlare: 0.08,
} as const;

export type QualityReport = {
  status: ImageQuality;
  sharpness: number;
  brightness: number;
  glare: number;
};

/** Mede foco, exposição e reflexo de uma imagem em tons de cinza (Uint8 por pixel). */
export function measureGray(gray: Uint8ClampedArray, width: number, height: number): QualityReport {
  let sum = 0;
  let saturated = 0;
  for (let i = 0; i < gray.length; i++) {
    sum += gray[i];
    if (gray[i] >= 250) saturated++;
  }
  const brightness = gray.length ? sum / gray.length : 0;
  const glare = gray.length ? saturated / gray.length : 0;

  let lapSum = 0;
  let lapSq = 0;
  let count = 0;
  for (let y = 1; y < height - 1; y++) {
    for (let x = 1; x < width - 1; x++) {
      const i = y * width + x;
      const lap = gray[i - width] + gray[i + width] + gray[i - 1] + gray[i + 1] - 4 * gray[i];
      lapSum += lap;
      lapSq += lap * lap;
      count++;
    }
  }
  const mean = count ? lapSum / count : 0;
  const sharpness = count ? lapSq / count - mean * mean : 0;

  let status: ImageQuality = "OK";
  if (brightness < QUALITY_THRESHOLDS.minBrightness) status = "DARK";
  else if (glare > QUALITY_THRESHOLDS.maxGlare) status = "GLARE";
  else if (sharpness < QUALITY_THRESHOLDS.minSharpness) status = "BLURRED";
  return { status, sharpness, brightness, glare };
}

/** Reduz o canvas para ~480 px de largura e mede a qualidade. */
export function measureCanvas(source: HTMLCanvasElement): QualityReport | null {
  const scale = Math.min(1, 480 / (source.width || 1));
  const width = Math.max(3, Math.round(source.width * scale));
  const height = Math.max(3, Math.round(source.height * scale));
  const small = document.createElement("canvas");
  small.width = width;
  small.height = height;
  const ctx = small.getContext("2d", { willReadFrequently: true });
  if (!ctx) return null;
  ctx.drawImage(source, 0, 0, width, height);
  const { data } = ctx.getImageData(0, 0, width, height);
  const gray = new Uint8ClampedArray(width * height);
  for (let i = 0, p = 0; i < data.length; i += 4, p++) {
    gray[p] = 0.299 * data[i] + 0.587 * data[i + 1] + 0.114 * data[i + 2];
  }
  return measureGray(gray, width, height);
}
