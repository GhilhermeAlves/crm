import { beforeEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";

const { importMock, downloadTemplateMock } = vi.hoisted(() => ({
  importMock: vi.fn(),
  downloadTemplateMock: vi.fn(),
}));

vi.mock("@/features/finance/hooks/useFinance", () => ({
  useImportTransactions: () => ({ mutate: importMock, isPending: false }),
}));

vi.mock("@/features/finance/services/finance.service", () => ({
  FinanceService: { downloadImportTemplate: downloadTemplateMock },
}));

import { ImportTransactionsDialog } from "@/features/finance/components/ImportTransactionsDialog";

const selectFile = (file: File) => {
  const input = screen.getByLabelText("Selecionar arquivo CSV");
  fireEvent.change(input, { target: { files: [file] } });
};

beforeEach(() => {
  importMock.mockReset();
  downloadTemplateMock.mockReset();
  downloadTemplateMock.mockResolvedValue(new Blob());
});

describe("ImportTransactionsDialog", () => {
  it("rejects a non-CSV file without importing", () => {
    render(<ImportTransactionsDialog open onOpenChange={() => {}} companyId="company-1" />);

    selectFile(new File(["nome"], "dados.txt", { type: "text/plain" }));

    expect(screen.getByText(/formato CSV/)).toBeTruthy();
    expect(importMock).not.toHaveBeenCalled();
  });

  it("shows the imported count for a successful send", () => {
    importMock.mockImplementation((_file, options) => {
      options.onSuccess({ imported: 5, errors: [] });
    });

    render(<ImportTransactionsDialog open onOpenChange={() => {}} companyId="company-1" />);
    selectFile(new File(["a,b\n"], "dados.csv", { type: "text/csv" }));
    fireEvent.click(screen.getByText("Enviar"));

    expect(screen.getByText("5 lançamentos importados")).toBeTruthy();
    expect(screen.getByText("Concluir")).toBeTruthy();
  });

  it("lists the lines with problems returned by the import", () => {
    importMock.mockImplementation((_file, options) => {
      options.onSuccess({ imported: 0, errors: [{ line: 3, message: "valor inválido" }] });
    });

    render(<ImportTransactionsDialog open onOpenChange={() => {}} companyId="company-1" />);
    selectFile(new File(["a,b\n"], "dados.csv", { type: "text/csv" }));
    fireEvent.click(screen.getByText("Enviar"));

    expect(screen.getByText("Linhas com problema")).toBeTruthy();
    expect(screen.getByText("Linha 3: valor inválido")).toBeTruthy();
  });
});
