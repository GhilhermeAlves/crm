import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { PatientImportPanel } from "@/features/masterdata/importing/components/PatientImportPanel";
import { importService } from "@/features/masterdata/importing/import.service";

vi.mock("@/features/masterdata/importing/import.service", () => ({
  importService: { preview: vi.fn(), importPatients: vi.fn(), list: vi.fn(), undo: vi.fn() },
}));

const preview = {
  total: 3,
  ok: 1,
  duplicates: 1,
  errors: 1,
  ignored: 0,
  unsupportedFiles: ["budgets.csv"],
  importId: null,
  rows: [
    { line: 2, name: "Ana", status: "OK", message: null, warnings: [] },
    { line: 3, name: "Beto", status: "DUPLICADO", message: "CPF já cadastrado", warnings: [] },
    { line: 4, name: "", status: "ERRO", message: "Primeiro nome é obrigatório.", warnings: [] },
  ],
};

function renderPanel() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <PatientImportPanel companyId="c1" />
    </QueryClientProvider>,
  );
}

describe("PatientImportPanel", () => {
  beforeEach(() => {
    vi.mocked(importService.list).mockResolvedValue([]);
    vi.mocked(importService.preview).mockResolvedValue(preview as never);
    vi.mocked(importService.importPatients).mockResolvedValue({
      ...preview,
      importId: "i1",
      rows: [{ ...preview.rows[0], status: "IMPORTADO" }, preview.rows[1], preview.rows[2]],
    } as never);
  });

  it("mostra a prévia e importa só os válidos", async () => {
    renderPanel();
    const file = new File(["x"], "base.zip", { type: "application/zip" });
    fireEvent.change(screen.getByLabelText(/arquivo \.zip/i), { target: { files: [file] } });

    expect(await screen.findByText("CPF já cadastrado")).toBeTruthy();
    expect(screen.getByText(/budgets\.csv/)).toBeTruthy();

    fireEvent.click(screen.getByRole("button", { name: /importar 1 paciente/i }));
    await waitFor(() => expect(importService.importPatients).toHaveBeenCalledWith("c1", file));
    expect(await screen.findByText(/1 paciente\(s\) importado/i)).toBeTruthy();
  });
});
