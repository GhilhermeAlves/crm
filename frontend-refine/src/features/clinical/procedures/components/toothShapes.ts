import type { Dentition, ToothFace } from "../types/procedure.types";

export type ToothKind = "incisor" | "canine" | "premolar" | "molar";

/** Tipo anatômico pelo número FDI (decíduos não têm pré-molares). */
export function toothKind(tooth: number, dentition: Dentition): ToothKind {
  const position = tooth % 10;
  if (position <= 2) return "incisor";
  if (position === 3) return "canine";
  if (dentition === "DECIDUOUS") return "molar";
  return position <= 5 ? "premolar" : "molar";
}

/** Arcada superior: quadrantes 1, 2 (permanentes) e 5, 6 (decíduos). */
export function isUpper(tooth: number): boolean {
  const quadrant = Math.floor(tooth / 10);
  return quadrant === 1 || quadrant === 2 || quadrant === 5 || quadrant === 6;
}

/** Lado direito do paciente (esquerda da tela): a face mesial aponta para a direita da tela. */
export function mesialOnRight(tooth: number): boolean {
  const quadrant = Math.floor(tooth / 10);
  return quadrant === 1 || quadrant === 4 || quadrant === 5 || quadrant === 8;
}

/**
 * Silhueta vista por vestibular em uma caixa 40x80, com a raiz para cima e a
 * coroa para baixo (arcada superior). A arcada inferior usa o mesmo desenho
 * espelhado na vertical.
 */
export const TOOTH_PATHS: Record<ToothKind, { root: string; crown: string }> = {
  incisor: {
    root: "M14 44 C14 30 16 15 19 5 C19.5 3.5 20.5 3.5 21 5 C24 15 26 30 26 44 Z",
    crown: "M14 44 C11.5 53 11 63 12.5 71 C13.5 75.5 26.5 75.5 27.5 71 C29 63 28.5 53 26 44 Z",
  },
  canine: {
    root: "M13.5 44 C13.5 28 16 11 19.2 2.5 C19.7 1.2 20.3 1.2 20.8 2.5 C24 11 26.5 28 26.5 44 Z",
    crown:
      "M13.5 44 C11 53 11 62 14 69 C16 73 18.5 76.5 20 77.5 C21.5 76.5 24 73 26 69 C29 62 29 53 26.5 44 Z",
  },
  premolar: {
    root: "M13 44 C13 31 15 18 18.5 8.5 C19.3 6.8 20.7 6.8 21.5 8.5 C25 18 27 31 27 44 Z",
    crown:
      "M13 44 C9.5 53 9 64 11.5 71.5 C13 76 17.5 76.5 20 73.5 C22.5 76.5 27 76 28.5 71.5 C31 64 30.5 53 27 44 Z",
  },
  molar: {
    root: "M8.5 44 C8 32 9.5 20 11.5 12 C12.4 9 14.6 9 15.3 12 C16.5 20 17.5 29 19 35 L21 35 C22.5 29 23.5 20 24.7 12 C25.4 9 27.6 9 28.5 12 C30.5 20 32 32 31.5 44 Z",
    crown:
      "M8.5 44 C5 53 4.5 65 7 72 C8.5 76.5 13 76.5 15 73.5 C17 76.5 23 76.5 25 73.5 C27 76.5 31.5 76.5 33 72 C35.5 65 35 53 31.5 44 Z",
  },
};

type FacePosition = "top" | "right" | "bottom" | "left" | "center";

/** Setores de um círculo 40x40: anel externo em 4 quadrantes + centro (oclusal/incisal). */
export const FACE_SECTOR_PATHS: Record<Exclude<FacePosition, "center">, string> = {
  top: "M7.27 7.27 A18 18 0 0 1 32.73 7.27 L25.66 14.34 A8 8 0 0 0 14.34 14.34 Z",
  right: "M32.73 7.27 A18 18 0 0 1 32.73 32.73 L25.66 25.66 A8 8 0 0 0 25.66 14.34 Z",
  bottom: "M32.73 32.73 A18 18 0 0 1 7.27 32.73 L14.34 25.66 A8 8 0 0 0 25.66 25.66 Z",
  left: "M7.27 32.73 A18 18 0 0 1 7.27 7.27 L14.34 14.34 A8 8 0 0 0 14.34 25.66 Z",
};

/**
 * Qual face ocupa cada setor: vestibular voltada para fora da boca (cima na
 * arcada superior, baixo na inferior) e mesial voltada para a linha média.
 */
export function faceLayout(tooth: number): Record<FacePosition, ToothFace> {
  const upper = isUpper(tooth);
  const mesialRight = mesialOnRight(tooth);
  return {
    top: upper ? "V" : "L",
    bottom: upper ? "L" : "V",
    right: mesialRight ? "M" : "D",
    left: mesialRight ? "D" : "M",
    center: "O",
  };
}
