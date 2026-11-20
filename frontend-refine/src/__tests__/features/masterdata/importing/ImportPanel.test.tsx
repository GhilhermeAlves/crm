import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ImportPanel } from "@/features/masterdata/importing/components/ImportPanel";
import { importService } from "@/features/masterdata/importing/import.service";
import type { ImportKind } from "@/features/masterdata/importing/types";

vi.mock("@/features/masterdata/importing/import.service", () => ({
  importService: { preview: vi.fn(), importData: vi.fn(), list: vi.fn(), undo: vi.fn() },
}));

const preview = {
  total: 3,
  ok: 1,
  duplicates: 1,
  errors: 1,
  ignored: 0,
  unsupportedFiles: ["budgets.csv"],
  importId: null,
  notices: [],
  rows: [
    { line: 2, name: "Ana", status: "OK", message: null, warnings: [] },
    { line: 3, name: "Beto", status: "DUPLICADO", message: "CPF já cadastrado", warnings: [] },
    { line: 4, name: "", status: "ERRO", message: "Primeiro nome é obrigatório.", warnings: [] },
  ],
};

function renderPanel(kind: ImportKind) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <ImportPanel companyId="c1" kind={kind} />
    </QueryClientProvider>,
  );
}

describe("ImportPanel", () => {
  beforeEach(() => {
    vi.mocked(importService.preview).mockResolvedValue(preview as never);
    vi.mocked(importService.importData).mockResolvedValue({
      ...preview,
      importId: "i1",
      rows: [{ ...preview.rows[0], status: "IMPORTADO" }, preview.rows[1], preview.rows[2]],
    } as never);
  });

  it("mostra a prévia de pacientes e importa só os válidos", async () => {
    renderPanel("patients");
    const file = new File(["x"], "base.zip", { type: "application/zip" });
    fireEvent.change(screen.getByLabelText(/arquivo \.zip/i), { target: { files: [file] } });

    expect(await screen.findByText("CPF já cadastrado")).toBeTruthy();
    expect(screen.getByText(/budgets\.csv/)).toBeTruthy();

    fireEvent.click(screen.getByRole("button", { name: /importar 1 paciente/i }));
    await waitFor(() =>
      expect(importService.importData).toHaveBeenCalledWith("c1", "patients", file),
    );
    expect(await screen.findByText(/1 paciente\(s\) importado/i)).toBeTruthy();
  });

  it("procedimentos usa o endpoint próprio e mostra os avisos do catálogo", async () => {
    vi.mocked(importService.preview).mockResolvedValue({
      ...preview,
      unsupportedFiles: [],
      notices: ["1 serviço(s) novo(s) no catálogo: Limpeza"],
    } as never);
    renderPanel("procedures");
    const file = new File(["x"], "base.zip", { type: "application/zip" });
    fireEvent.change(screen.getByLabelText(/arquivo \.zip/i), { target: { files: [file] } });

    expect(await screen.findByText(/serviço\(s\) novo\(s\) no catálogo/)).toBeTruthy();
    expect(importService.preview).toHaveBeenCalledWith("c1", "procedures", file);
    expect(screen.getByRole("button", { name: /importar 1 procedimento/i })).toBeTruthy();
  });
});
