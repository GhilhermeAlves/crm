import type {
  Dentition,
  PatientProcedureResponse,
  ProcedureState,
  ToothFace,
} from "../types/procedure.types";

const range = (quadrant: number, count: number, reverse: boolean) => {
  const teeth = Array.from({ length: count }, (_, i) => quadrant * 10 + i + 1);
  return reverse ? teeth.reverse() : teeth;
};

/** Quadrantes na ordem de exibição: sup. direito, sup. esquerdo, inf. esquerdo, inf. direito. */
export const QUADRANTS: Record<Dentition, number[][]> = {
  PERMANENT: [range(1, 8, true), range(2, 8, false), range(3, 8, false), range(4, 8, false)],
  DECIDUOUS: [range(5, 5, true), range(6, 5, false), range(7, 5, false), range(8, 5, false)],
};

export const FACES: ToothFace[] = ["M", "D", "O", "V", "L"];

export const STATE_LABELS: Record<ProcedureState, string> = {
  PLANNED: "A realizar",
  DONE: "Realizado",
  PRE_EXISTING: "Pré-existente",
};

export const DENTITION_LABELS: Record<Dentition, string> = {
  PERMANENT: "Permanente",
  DECIDUOUS: "Decídua",
};

/** Cores por estado (referência Capim): amarelo a realizar, verde realizado, azul pré-existente. */
export const STATE_FILL: Record<ProcedureState, string> = {
  PLANNED: "fill-amber-400",
  DONE: "fill-emerald-500",
  PRE_EXISTING: "fill-sky-500",
};

/** Texto do número do dente conforme o estado. */
export const STATE_TEXT: Record<ProcedureState, string> = {
  PLANNED: "text-amber-600 dark:text-amber-400",
  DONE: "text-emerald-600 dark:text-emerald-400",
  PRE_EXISTING: "text-sky-600 dark:text-sky-400",
};

/** Pílula de estado usada nas listas. */
export const STATE_PILL: Record<ProcedureState, string> = {
  PLANNED: "bg-amber-100 text-amber-800 dark:bg-amber-500/15 dark:text-amber-300",
  DONE: "bg-emerald-100 text-emerald-800 dark:bg-emerald-500/15 dark:text-emerald-300",
  PRE_EXISTING: "bg-sky-100 text-sky-800 dark:bg-sky-500/15 dark:text-sky-300",
};

const PRIORITY: ProcedureState[] = ["PLANNED", "DONE", "PRE_EXISTING"];

export function dominantState(states: ProcedureState[]): ProcedureState | null {
  return PRIORITY.find((state) => states.includes(state)) ?? null;
}

export type ToothSummary = {
  state: ProcedureState;
  faces: Partial<Record<ToothFace, ProcedureState>>;
};

export function toothStates(
  procedures: PatientProcedureResponse[],
  dentition: Dentition,
): Map<number, ToothSummary> {
  type Entry = { states: ProcedureState[]; faces: Map<ToothFace, ProcedureState[]> };
  const byTooth = new Map<number, Entry>();
  for (const procedure of procedures) {
    if (procedure.dentition !== dentition) continue;
    for (const { tooth, faces } of procedure.teeth) {
      const entry: Entry = byTooth.get(tooth) ?? { states: [], faces: new Map() };
      entry.states.push(procedure.state);
      for (const face of faces) {
        entry.faces.set(face, [...(entry.faces.get(face) ?? []), procedure.state]);
      }
      byTooth.set(tooth, entry);
    }
  }
  const result = new Map<number, ToothSummary>();
  byTooth.forEach((entry, tooth) => {
    const faces: Partial<Record<ToothFace, ProcedureState>> = {};
    entry.faces.forEach((states, face) => {
      faces[face] = dominantState(states) as ProcedureState;
    });
    result.set(tooth, { state: dominantState(entry.states) as ProcedureState, faces });
  });
  return result;
}

export function formatTeeth(teeth: { tooth: number; faces: ToothFace[] }[]): string {
  if (teeth.length === 0) return "Boca toda";
  return teeth
    .map(({ tooth, faces }) => (faces.length ? `${tooth} (${faces.join("")})` : String(tooth)))
    .join(", ");
}
