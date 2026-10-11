import { beforeEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";

const { submitMock, toastSuccess, toastError } = vi.hoisted(() => ({
  submitMock: vi.fn(),
  toastSuccess: vi.fn(),
  toastError: vi.fn(),
}));

vi.mock("@/features/reports/hooks/useReports", () => ({
  useSubmitReportSuggestion: () => ({ mutate: submitMock, isPending: false }),
}));

vi.mock("sonner", () => ({
  toast: { success: toastSuccess, error: toastError },
}));

import { SuggestReportDialog } from "@/features/reports/components/SuggestReportDialog";

const renderDialog = (onOpenChange: (open: boolean) => void = () => {}) =>
  render(<SuggestReportDialog open onOpenChange={onOpenChange} companyId="company-1" />);

const continueButton = () => screen.getByRole("button", { name: "Continuar" }) as HTMLButtonElement;

beforeEach(() => {
  submitMock.mockReset();
  toastSuccess.mockReset();
  toastError.mockReset();
});

describe("SuggestReportDialog", () => {
  it("mantém 'Continuar' desabilitado até escolher um tema", () => {
    renderDialog();

    expect(continueButton().disabled).toBe(true);

    fireEvent.click(screen.getByRole("checkbox", { name: "Ficha clínica" }));

    expect(continueButton().disabled).toBe(false);
  });

  it("avança para o passo de observação e permite enviar sem observação", () => {
    submitMock.mockImplementation((_body, options) => options.onSuccess({ id: "s-1" }));
    const onOpenChange = vi.fn();
    renderDialog(onOpenChange);

    fireEvent.click(screen.getByRole("checkbox", { name: "Financeiro" }));
    fireEvent.click(continueButton());

    expect(screen.getByText("Mais contexto para sua escolha")).toBeTruthy();

    fireEvent.click(screen.getByRole("button", { name: "Enviar" }));

    expect(submitMock).toHaveBeenCalledWith(
      { categories: ["FINANCE"], notes: undefined },
      expect.anything(),
    );
    expect(toastSuccess).toHaveBeenCalledWith("Recebemos sua sugestão!");
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it("envia as categorias escolhidas e a observação", () => {
    submitMock.mockImplementation((_body, options) => options.onSuccess({ id: "s-1" }));
    renderDialog();

    fireEvent.click(screen.getByRole("checkbox", { name: "Estoque" }));
    fireEvent.click(screen.getByRole("checkbox", { name: "Outros" }));
    fireEvent.click(continueButton());
    fireEvent.change(screen.getByLabelText("Deseja incluir alguma observação?"), {
      target: { value: "  separar por unidade  " },
    });
    fireEvent.click(screen.getByRole("button", { name: "Enviar" }));

    expect(submitMock).toHaveBeenCalledWith(
      { categories: ["INVENTORY", "OTHER"], notes: "separar por unidade" },
      expect.anything(),
    );
  });

  it("mostra erro quando o envio falha", () => {
    submitMock.mockImplementation((_body, options) => options.onError(new Error("boom")));
    renderDialog();

    fireEvent.click(screen.getByRole("checkbox", { name: "Financeiro" }));
    fireEvent.click(continueButton());
    fireEvent.click(screen.getByRole("button", { name: "Enviar" }));

    expect(toastError).toHaveBeenCalled();
  });
});
