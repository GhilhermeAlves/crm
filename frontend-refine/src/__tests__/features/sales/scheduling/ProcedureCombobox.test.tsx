import { describe, it, expect, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { ProcedureCombobox } from "@/features/sales/scheduling/components/ProcedureCombobox";

const procedures = [
  { id: "1", name: "Restauração em resina" },
  { id: "2", name: "Limpeza" },
  { id: "3", name: "Extração simples" },
];

function open() {
  fireEvent.click(screen.getByRole("combobox", { name: "Selecionar procedimentos" }));
}

describe("ProcedureCombobox", () => {
  it("filtra ignorando acentos e maiúsculas", async () => {
    render(<ProcedureCombobox procedures={procedures} value={[]} onChange={vi.fn()} />);
    open();
    fireEvent.change(await screen.findByPlaceholderText("Digite o nome do procedimento…"), {
      target: { value: "RESTAURACAO" },
    });
    expect(screen.getByRole("option", { name: "Restauração em resina" })).toBeTruthy();
    expect(screen.queryByRole("option", { name: "Limpeza" })).toBeNull();
  });

  it("avisa quando nada é encontrado", async () => {
    render(<ProcedureCombobox procedures={procedures} value={[]} onChange={vi.fn()} />);
    open();
    fireEvent.change(await screen.findByPlaceholderText("Digite o nome do procedimento…"), {
      target: { value: "implante" },
    });
    expect(screen.getByText("Nenhum procedimento encontrado.")).toBeTruthy();
  });

  it("adiciona e remove procedimentos", async () => {
    const onChange = vi.fn();
    const { rerender } = render(
      <ProcedureCombobox procedures={procedures} value={["2"]} onChange={onChange} />,
    );
    expect(screen.getByText("1 procedimento selecionado")).toBeTruthy();
    open();
    fireEvent.click(await screen.findByRole("option", { name: "Extração simples" }));
    expect(onChange).toHaveBeenLastCalledWith(["2", "3"]);

    rerender(<ProcedureCombobox procedures={procedures} value={["2", "3"]} onChange={onChange} />);
    fireEvent.click(screen.getByRole("button", { name: "Remover Limpeza" }));
    expect(onChange).toHaveBeenLastCalledWith(["3"]);
  });
});
