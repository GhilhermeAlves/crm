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
    root: "M15 6 Q20 1 25 6 L26 44 L14 44 Z",
    crown: "M14 44 L26 44 L29 70 Q20 77 11 70 Z",
  },
  canine: {
    root: "M16 3 Q20 -1 24 3 L27 44 L13 44 Z",
    crown: "M13 44 L27 44 L29 64 L20 78 L11 64 Z",
  },
  premolar: {
    root: "M15 9 Q20 4 25 9 L27 44 L13 44 Z",
    crown: "M11 44 L29 44 L31 64 Q30 76 24 76 L20 72 L16 76 Q10 76 9 64 Z",
  },
  molar: {
    root: "M7 44 L9 14 Q12 8 15 14 L18 38 L22 38 L25 14 Q28 8 31 14 L33 44 Z",
    crown: "M6 44 L34 44 L36 62 Q35 76 28 76 L24 71 L20 76 L16 71 L12 76 Q5 76 4 62 Z",
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
