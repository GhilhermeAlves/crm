import { describe, it, expect, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { Odontogram } from "@/features/clinical/procedures/components/Odontogram";
import type { ToothSummary } from "@/features/clinical/procedures/utils/teeth";

describe("Odontogram", () => {
  it("pinta o dente pelo estado e informa o clique", () => {
    const onClick = vi.fn();
    const summary = new Map<number, ToothSummary>([
      [11, { state: "PLANNED", faces: { O: "PLANNED" } }],
    ]);
    render(
      <Odontogram dentition="PERMANENT" summary={summary} selected={[]} onToothClick={onClick} />,
    );
    const tooth = screen.getByRole("button", { name: "Dente 11, A realizar" });
    expect(tooth.querySelector(".fill-destructive")).toBeTruthy();
    fireEvent.click(tooth);
    expect(onClick).toHaveBeenCalledWith(11);
    expect(screen.getByRole("button", { name: "Dente 48" })).toBeTruthy();
  });

  it("troca para a dentição decídua", () => {
    const onChange = vi.fn();
    render(
      <Odontogram
        dentition="DECIDUOUS"
        onDentitionChange={onChange}
        summary={new Map()}
        selected={[]}
        onToothClick={vi.fn()}
      />,
    );
    expect(screen.getByRole("button", { name: "Dente 55" })).toBeTruthy();
    expect(screen.queryByRole("button", { name: "Dente 18" })).toBeNull();
    fireEvent.click(screen.getByText("Permanente"));
    expect(onChange).toHaveBeenCalledWith("PERMANENT");
  });

  it("marca dentes selecionados", () => {
    render(
      <Odontogram
        dentition="PERMANENT"
        summary={new Map()}
        selected={[21]}
        onToothClick={vi.fn()}
      />,
    );
    expect(screen.getByRole("button", { name: "Dente 21" }).getAttribute("aria-pressed")).toBe(
      "true",
    );
  });
});

describe("Odontogram — dente inteiro", () => {
  it("procedimento sem faces pinta o desenho do dente", () => {
    const summary = new Map<number, ToothSummary>([[36, { state: "DONE", faces: {} }]]);
    render(
      <Odontogram dentition="PERMANENT" summary={summary} selected={[]} onToothClick={vi.fn()} />,
    );
    const tooth = screen.getByRole("button", { name: "Dente 36, Realizado" });
    expect(tooth.querySelectorAll(".fill-primary").length).toBe(2);
  });
});
