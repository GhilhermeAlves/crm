import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { User } from "lucide-react";

import { ReportCard } from "@/features/finance/components/reports/ReportCard";
import { ReportListChip } from "@/features/finance/components/reports/ReportListChip";
import { ReportShell } from "@/features/finance/components/reports/ReportShell";

const renderShell = (onExport: () => void = () => {}) =>
  render(
    <ReportShell
      title="Relatório de teste"
      subtitle="Subtítulo do relatório"
      panelTitle="Painel de teste"
      onExport={onExport}
    >
      <ReportListChip
        icon={User}
        label="Profissional"
        options={[
          { value: "u1", label: "Ana" },
          { value: "u2", label: "Bruno" },
        ]}
        selected={[]}
        onApply={() => {}}
        onClear={() => {}}
      />
      <ReportCard title="Gráfico" isEmpty />
    </ReportShell>,
  );

describe("ReportShell", () => {
  it("mostra o contexto do relatório e volta para /reports", () => {
    renderShell();

    expect(screen.getByText("Relatório de teste")).toBeTruthy();
    expect(screen.getByText("Subtítulo do relatório")).toBeTruthy();
    expect(screen.getByText("Painel de teste")).toBeTruthy();

    const back = screen.getByRole("link", { name: /Voltar/ });
    expect(back.getAttribute("href")).toBe("/reports");
  });

  it("exporta em CSV ao clicar no ícone de download", () => {
    const onExport = vi.fn();
    renderShell(onExport);

    fireEvent.click(screen.getByRole("button", { name: "Baixar relatório em CSV" }));

    expect(onExport).toHaveBeenCalledTimes(1);
  });

  it("abre o popover de lista com busca e 'Selecionar tudo'", () => {
    renderShell();

    expect(screen.queryByText("Selecionar tudo")).toBeNull();

    fireEvent.click(screen.getByRole("button", { name: /Profissional/ }));

    expect(screen.getByText("Selecionar tudo")).toBeTruthy();
    expect(screen.getByPlaceholderText("Pesquisar na lista")).toBeTruthy();
  });

  it("mostra 'Sem resultados' em um card de gráfico vazio", () => {
    renderShell();

    expect(screen.getByText("Sem resultados")).toBeTruthy();
  });
});
