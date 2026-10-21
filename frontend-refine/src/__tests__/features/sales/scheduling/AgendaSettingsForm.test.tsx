import { describe, it, expect, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { AgendaSettingsForm } from "@/features/sales/scheduling/components/AgendaSettingsForm";
import {
  DEFAULT_AGENDA_SETTINGS,
  normalizeAgendaSettings,
} from "@/features/sales/scheduling/services/scheduling.service";

function renderForm(canConfigure = true) {
  const onSave = vi.fn();
  render(
    <AgendaSettingsForm
      settings={DEFAULT_AGENDA_SETTINGS}
      canConfigure={canConfigure}
      isSaving={false}
      onSave={onSave}
    />,
  );
  const saveButton = () => screen.queryByRole("button", { name: "Salvar" }) as HTMLButtonElement;
  return { onSave, saveButton };
}

describe("AgendaSettingsForm", () => {
  it("só habilita Salvar depois de alguma mudança e envia os ajustes completos", () => {
    const { onSave, saveButton } = renderForm();
    expect(saveButton().disabled).toBe(true);

    fireEvent.change(screen.getByLabelText("Início do expediente"), { target: { value: "07:00" } });
    expect(saveButton().disabled).toBe(false);
    fireEvent.click(saveButton());

    expect(onSave).toHaveBeenCalledWith({ ...DEFAULT_AGENDA_SETTINGS, workHoursStart: "07:00" });
  });

  it("bloqueia expediente com início depois do fim", () => {
    const { saveButton } = renderForm();
    fireEvent.change(screen.getByLabelText("Fim do expediente"), { target: { value: "07:00" } });

    expect(screen.getByText("O início do expediente deve ser antes do fim.")).toBeTruthy();
    expect(saveButton().disabled).toBe(true);
  });

  it("sem permissão não mostra Salvar", () => {
    const { saveButton } = renderForm(false);
    expect(saveButton()).toBeNull();
  });
});

describe("normalizeAgendaSettings", () => {
  it("converte o horário do backend para HH:mm", () => {
    const result = normalizeAgendaSettings({
      ...DEFAULT_AGENDA_SETTINGS,
      workHoursStart: "07:30:00",
      workHoursEnd: "19:00:00",
    });
    expect(result.workHoursStart).toBe("07:30");
    expect(result.workHoursEnd).toBe("19:00");
  });
});
