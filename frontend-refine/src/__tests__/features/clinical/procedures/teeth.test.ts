import { describe, it, expect } from "vitest";
import { QUADRANTS, toothStates, dominantState } from "@/features/clinical/procedures/utils/teeth";
import type { PatientProcedureResponse } from "@/features/clinical/procedures/types/procedure.types";

const base = { teeth: [], dentition: "PERMANENT" } as unknown as PatientProcedureResponse;

describe("teeth utils", () => {
  it("lista os quadrantes de cada dentição", () => {
    expect(QUADRANTS.PERMANENT[0]).toEqual([18, 17, 16, 15, 14, 13, 12, 11]);
    expect(QUADRANTS.PERMANENT[1]).toEqual([21, 22, 23, 24, 25, 26, 27, 28]);
    expect(QUADRANTS.DECIDUOUS[3]).toEqual([81, 82, 83, 84, 85]);
  });

  it("prioriza A realizar > Realizado > Pré-existente", () => {
    expect(dominantState(["PRE_EXISTING", "DONE"])).toBe("DONE");
    expect(dominantState(["DONE", "PLANNED", "PRE_EXISTING"])).toBe("PLANNED");
    expect(dominantState([])).toBeNull();
  });

  it("agrega estado por dente e por face", () => {
    const list = [
      { ...base, state: "DONE", teeth: [{ tooth: 11, faces: ["M"] }] },
      { ...base, state: "PLANNED", teeth: [{ tooth: 11, faces: ["O"] }] },
      { ...base, state: "PRE_EXISTING", teeth: [] },
    ] as PatientProcedureResponse[];
    const map = toothStates(list, "PERMANENT");
    expect(map.get(11)?.state).toBe("PLANNED");
    expect(map.get(11)?.faces.M).toBe("DONE");
    expect(map.get(11)?.faces.O).toBe("PLANNED");
    expect(map.size).toBe(1);
  });
});
