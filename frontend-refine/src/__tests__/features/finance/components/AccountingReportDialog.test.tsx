import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";

const state = vi.hoisted(() => ({
  months: [] as string[],
  loading: false,
  pending: false,
  generate: vi.fn(),
}));

vi.mock("@/features/finance/hooks/useFinance", () => ({
  useAvailableMonths: () => ({ data: state.months, isLoading: state.loading }),
}));

vi.mock("@/features/reports/export/useReportExports", () => ({
  useRequestReportExport: () => ({ mutate: state.generate, isPending: state.pending }),
}));

import { AccountingReportDialog } from "@/features/finance/components/AccountingReportDialog";

const createObjectURL = vi.fn(() => "blob:mock");
const revokeObjectURL = vi.fn();

const renderDialog = (onOpenChange: (open: boolean) => void = () => {}) =>
  render(<AccountingReportDialog open onOpenChange={onOpenChange} companyId="company-1" />);

const generateButton = () =>
  screen.getByRole("button", { name: /Gerar relatório/ }) as HTMLButtonElement;

const selectMonth = (label: string) => {
  fireEvent.click(screen.getByRole("combobox"));
  fireEvent.click(screen.getByRole("option", { name: label }));
};

beforeEach(() => {
  state.months = [];
  state.loading = false;
  state.pending = false;
  state.generate.mockReset();
  URL.createObjectURL = createObjectURL;
  URL.revokeObjectURL = revokeObjectURL;
  createObjectURL.mockClear();
  revokeObjectURL.mockClear();
  vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => {});
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe("AccountingReportDialog", () => {
  it("mantém 'Gerar e baixar' desabilitado até escolher tipo, mês e formato", () => {
    state.months = ["2020-01"];
    renderDialog();

    expect(generateButton().disabled).toBe(true);

    fireEvent.click(screen.getByRole("radio", { name: /Fluxo de caixa/ }));
    expect(generateButton().disabled).toBe(true);

    fireEvent.click(screen.getByRole("radio", { name: "Excel" }));
    expect(generateButton().disabled).toBe(true);

    selectMonth("Janeiro 2020");
    expect(generateButton().disabled).toBe(false);
  });

  it("exibe os meses disponíveis como 'Setembro 2026'", () => {
    state.months = ["2026-09"];
    renderDialog();

    fireEvent.click(screen.getByRole("combobox"));

    expect(screen.getAllByText("Setembro 2026").length).toBeGreaterThan(0);
  });

  it("pede o relatório com o tipo, mês e formato escolhidos e fecha", () => {
    state.months = ["2026-09"];
    state.generate.mockImplementation((_body, options) => {
      options.onSuccess();
    });

    const onOpenChange = vi.fn();
    renderDialog(onOpenChange);

    fireEvent.click(screen.getByRole("radio", { name: /Fluxo de caixa/ }));
    fireEvent.click(screen.getByRole("radio", { name: "Excel" }));
    selectMonth("Setembro 2026");

    fireEvent.click(generateButton());

    expect(state.generate).toHaveBeenCalledWith(
      { type: "CASH_FLOW", format: "XLSX", from: "2026-09-01", to: "2026-09-30" },
      expect.anything(),
    );
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it("mostra o aviso quando não há meses com lançamentos", () => {
    state.months = [];
    renderDialog();

    expect(screen.getByText("Ainda não há lançamentos para gerar relatórios.")).toBeTruthy();
    expect(generateButton().disabled).toBe(true);
  });
});
