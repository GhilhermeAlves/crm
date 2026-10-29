import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { PublicPatientForm } from "@/features/clinical/anamnesis/components/PublicPatientForm";
import { PublicFormService } from "@/features/clinical/anamnesis/services/public-form.service";

vi.mock("@/features/clinical/anamnesis/services/public-form.service", () => ({
  PublicFormService: { get: vi.fn(), submit: vi.fn() },
}));

// O canvas do jsdom não desenha: o quadro de assinatura vira um botão que "assina".
vi.mock("@/features/clinical/anamnesis/components/SignaturePad", () => ({
  SignaturePad: ({ onChange }: { onChange: (v: string | null) => void }) => (
    <button type="button" onClick={() => onChange("data:image/png;base64,AAAA")}>
      assinar
    </button>
  ),
}));

const form = {
  clinicName: "Clínica Capilar",
  clinicLogoUrl: null,
  patientFirstName: "Ana",
  formName: "Anamnese capilar",
  mode: "LINK" as const,
  form: {
    id: "m1",
    companyId: "co1",
    name: "Anamnese capilar",
    description: null,
    active: true,
    isDefault: false,
    version: 1,
    createdAt: "",
    updatedAt: "",
    sections: [
      {
        id: "s1",
        title: "Saúde",
        professional: false,
        sortOrder: 0,
        questions: [
          {
            id: "q1",
            text: "Usa medicamento?",
            type: "YES_NO" as const,
            required: true,
            highlight: false,
            allowComplement: false,
            complementLabel: null,
            complementTrigger: "YES" as const,
            complementOptionValue: null,
            sortOrder: 0,
            options: [],
          },
        ],
      },
    ],
  },
};

describe("PublicPatientForm", () => {
  beforeEach(() => {
    vi.mocked(PublicFormService.get).mockResolvedValue({ ok: true, data: form });
    vi.mocked(PublicFormService.submit).mockResolvedValue({ ok: true, data: undefined });
  });

  it("exige resposta obrigatória e assinatura antes de enviar", async () => {
    render(<PublicPatientForm token="tok" reception={false} />);
    await screen.findByText("Anamnese capilar");
    expect(screen.getByText("Clínica Capilar")).toBeTruthy();

    fireEvent.click(screen.getByText("Enviar respostas"));
    expect(screen.getByText(/perguntas obrigatórias: Usa medicamento\?/)).toBeTruthy();

    fireEvent.click(screen.getByLabelText("Sim"));
    fireEvent.click(screen.getByText("Enviar respostas"));
    expect(screen.getByText("Assine no quadro antes de enviar.")).toBeTruthy();
    expect(PublicFormService.submit).not.toHaveBeenCalled();
  });

  it("envia respostas e assinatura e, na recepção, pede para devolver o tablet", async () => {
    render(<PublicPatientForm token="tok" reception />);
    await screen.findByText("Anamnese capilar");

    fireEvent.click(screen.getByLabelText("Sim"));
    fireEvent.click(screen.getByText("assinar"));
    fireEvent.click(screen.getByText("Enviar respostas"));

    await waitFor(() =>
      expect(PublicFormService.submit).toHaveBeenCalledWith("tok", {
        answers: { q1: { value: "YES", complement: null } },
        signature: "data:image/png;base64,AAAA",
        website: "",
      }),
    );
    expect(await screen.findByText(/devolva o tablet à recepção/)).toBeTruthy();
  });

  it("mostra aviso quando o link já foi usado", async () => {
    vi.mocked(PublicFormService.get).mockResolvedValue({
      ok: false,
      status: 410,
      message: "Este formulário já foi respondido.",
    });
    render(<PublicPatientForm token="tok" reception={false} />);
    expect(await screen.findByText("Este formulário já foi respondido.")).toBeTruthy();
  });
});
