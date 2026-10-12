import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";

import { RecentReportExports } from "@/features/reports/export/RecentReportExports";
import { ReportExportForm } from "@/features/reports/export/ReportExportForm";
import {
  findReportExport,
  parseReportExportMetadata,
  type ReportExport,
} from "@/features/reports/export/types";

vi.mock("@/features/reports/export/report-export.service", () => ({
  downloadReportExport: vi.fn().mockResolvedValue(undefined),
}));

function exportItem(overrides: Partial<ReportExport>): ReportExport {
  return {
    id: "e1",
    type: "CASH_FLOW",
    typeLabel: "Fluxo de caixa",
    format: "PDF",
    periodFrom: "2026-06-01",
    periodTo: "2026-06-30",
    status: "READY",
    fileName: "fluxo.pdf",
    errorMessage: null,
    createdAt: new Date().toISOString(),
    finishedAt: null,
    ...overrides,
  };
}

describe("ReportExportForm", () => {
  it("envia o mês para a DRE em PDF", () => {
    const onSubmit = vi.fn();
    render(
      <ReportExportForm
        definition={findReportExport("monthly-dre")!}
        isSubmitting={false}
        onSubmit={onSubmit}
      />,
    );
    fireEvent.change(screen.getByLabelText("Mês de referência"), { target: { value: "2026-06" } });
    fireEvent.click(screen.getByRole("button", { name: /Imprimir \(PDF\)/ }));

    expect(onSubmit).toHaveBeenCalledWith({ type: "MONTHLY_DRE", format: "PDF", month: "2026-06" });
  });

  it("envia o intervalo para o fluxo de caixa em Excel", () => {
    const onSubmit = vi.fn();
    render(
      <ReportExportForm
        definition={findReportExport("cash-flow")!}
        isSubmitting={false}
        onSubmit={onSubmit}
      />,
    );
    fireEvent.change(screen.getByLabelText("De"), { target: { value: "2026-05-01" } });
    fireEvent.change(screen.getByLabelText("Até"), { target: { value: "2026-05-31" } });
    fireEvent.click(screen.getByRole("button", { name: /Exportar \(Excel\)/ }));

    expect(onSubmit).toHaveBeenCalledWith({
      type: "CASH_FLOW",
      format: "XLSX",
      from: "2026-05-01",
      to: "2026-05-31",
    });
  });

  it("bloqueia intervalo invertido", () => {
    render(
      <ReportExportForm
        definition={findReportExport("agenda")!}
        isSubmitting={false}
        onSubmit={vi.fn()}
      />,
    );
    fireEvent.change(screen.getByLabelText("De"), { target: { value: "2026-06-10" } });
    fireEvent.change(screen.getByLabelText("Até"), { target: { value: "2026-06-01" } });

    expect(screen.getByRole("button", { name: /Imprimir \(PDF\)/ })).toHaveProperty(
      "disabled",
      true,
    );
  });
});

describe("RecentReportExports", () => {
  it("mostra Baixar só para exportações prontas", () => {
    render(
      <RecentReportExports
        companyId="c1"
        isLoading={false}
        exports={[
          exportItem({ id: "a", status: "READY" }),
          exportItem({ id: "b", status: "PENDING", fileName: null }),
          exportItem({ id: "c", status: "FAILED", fileName: null }),
        ]}
      />,
    );

    expect(screen.getAllByRole("button", { name: /Baixar/ })).toHaveLength(1);
    expect(screen.getByText("Gerando")).toBeTruthy();
    expect(screen.getByText("Falhou")).toBeTruthy();
  });
});

describe("parseReportExportMetadata", () => {
  it("reconhece só metadados de exportação", () => {
    expect(
      parseReportExportMetadata(
        '{"kind":"REPORT_EXPORT","exportId":"x","fileName":"a.pdf","format":"PDF"}',
      ),
    ).toMatchObject({ exportId: "x", fileName: "a.pdf" });
    expect(parseReportExportMetadata('{"kind":"OTHER"}')).toBeNull();
    expect(parseReportExportMetadata("not json")).toBeNull();
    expect(parseReportExportMetadata(null)).toBeNull();
  });
});
