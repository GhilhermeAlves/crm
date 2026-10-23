import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { AnamnesisAnswerForm } from "@/features/clinical/anamnesis/components/AnamnesisAnswerForm";
import type { PatientAnamnesisResponse } from "@/features/clinical/anamnesis/types/patient-anamnesis.types";
import type { AnamnesisQuestion } from "@/features/masterdata/anamnesis/types/anamnesis.types";

const { saveMock } = vi.hoisted(() => ({ saveMock: vi.fn() }));

vi.mock("@/features/clinical/anamnesis/hooks/usePatientAnamnesis", () => ({
  useSavePatientAnamnesis: () => ({ mutate: saveMock, isPending: false }),
}));

function question(overrides: Partial<AnamnesisQuestion>): AnamnesisQuestion {
  return {
    id: "q1",
    text: "Pergunta",
    type: "YES_NO",
    required: false,
    highlight: false,
    allowComplement: false,
    complementLabel: null,
    complementTrigger: "YES",
    complementOptionValue: null,
    sortOrder: 1,
    options: [],
    ...overrides,
  };
}

function makeAnamnesis(questions: AnamnesisQuestion[]): PatientAnamnesisResponse {
  return {
    id: "a1",
    contactId: "c1",
    modelId: "m1",
    modelName: "Padrão",
    modelVersion: 1,
    status: "DRAFT",
    answers: [],
    snapshot: {
      id: "m1",
      companyId: "co1",
      name: "Padrão",
      description: null,
      active: true,
      isDefault: true,
      version: 1,
      sections: [{ id: "s1", title: "Saúde geral", professional: false, sortOrder: 1, questions }],
      createdAt: "2026-10-01T00:00:00",
      updatedAt: "2026-10-01T00:00:00",
    },
    alerts: [],
    createdBy: "u1",
    createdByName: "Ana",
    completedBy: null,
    completedByName: null,
    completedAt: null,
    createdAt: "2026-10-20T10:00:00",
    updatedAt: "2026-10-20T10:00:00",
  };
}

describe("AnamnesisAnswerForm", () => {
  beforeEach(() => saveMock.mockClear());

  it("mostra o complemento só quando 'Sim' é marcado (gatilho YES)", () => {
    const anamnesis = makeAnamnesis([
      question({
        text: "Tem alergia?",
        allowComplement: true,
        complementLabel: "Qual alergia?",
        complementTrigger: "YES",
      }),
    ]);
    render(
      <AnamnesisAnswerForm companyId="co1" contactId="c1" anamnesis={anamnesis} complete={false} />,
    );

    expect(screen.queryByText("Qual alergia?")).toBeNull();
    fireEvent.click(screen.getByLabelText("Não"));
    expect(screen.queryByText("Qual alergia?")).toBeNull();
    fireEvent.click(screen.getByLabelText("Sim"));
    expect(screen.getByText("Qual alergia?")).toBeTruthy();
  });

  it("MULTI_SELECT envia as opções marcadas como array", () => {
    const anamnesis = makeAnamnesis([
      question({
        text: "Doenças",
        type: "MULTI_SELECT",
        options: [
          { id: "o1", label: "Diabetes", value: "DIABETES", sortOrder: 1 },
          { id: "o2", label: "Hipertensão", value: "HIPERTENSAO", sortOrder: 2 },
          { id: "o3", label: "Asma", value: "ASMA", sortOrder: 3 },
        ],
      }),
    ]);
    render(
      <AnamnesisAnswerForm companyId="co1" contactId="c1" anamnesis={anamnesis} complete={false} />,
    );

    fireEvent.click(screen.getByLabelText("Diabetes"));
    fireEvent.click(screen.getByLabelText("Asma"));
    fireEvent.click(screen.getByText("Salvar rascunho"));

    const { payload } = saveMock.mock.calls[0][0];
    expect(payload.complete).toBe(false);
    expect(payload.answers[0].questionId).toBe("q1");
    expect(payload.answers[0].valueJson).toEqual(["DIABETES", "ASMA"]);
  });
});
