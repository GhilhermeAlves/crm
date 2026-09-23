import { describe, it, expect, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { DeleteLeadDialog } from "./DeleteLeadDialog";
import type { Lead } from "../types/lead.types";

const lead: Lead = {
  id: "lead-1",
  companyId: "company-1",
  contactId: "contact-1",
  status: "NEW",
  score: 50,
  classification: null,
  source: "WHATSAPP",
  campaignId: null,
  assignedTo: null,
  notes: null,
  createdAt: "2026-08-13T00:00:00",
  updatedAt: "2026-08-13T00:00:00",
};

describe("DeleteLeadDialog (Sprint 10)", () => {
  it("renders nothing when there is no lead selected", () => {
    const { container } = render(
      <DeleteLeadDialog lead={null} open onOpenChange={vi.fn()} onConfirm={vi.fn()} />,
    );
    expect(container.querySelectorAll("*").length).toBe(0);
  });

  it("shows title, description and buttons when a lead is selected", () => {
    render(<DeleteLeadDialog lead={lead} open onOpenChange={vi.fn()} onConfirm={vi.fn()} />);
    expect(screen.getByText("Excluir Lead")).toBeTruthy();
    expect(screen.getByText(/Tem certeza que deseja excluir este lead/)).toBeTruthy();
    expect(screen.getByRole("button", { name: "Cancelar" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "Excluir" })).toBeTruthy();
  });

  it("confirms deletion", () => {
    const onConfirm = vi.fn();
    render(<DeleteLeadDialog lead={lead} open onOpenChange={vi.fn()} onConfirm={onConfirm} />);
    fireEvent.click(screen.getByRole("button", { name: "Excluir" }));
    expect(onConfirm).toHaveBeenCalledTimes(1);
  });

  it("cancels and closes the dialog", () => {
    const onOpenChange = vi.fn();
    render(<DeleteLeadDialog lead={lead} open onOpenChange={onOpenChange} onConfirm={vi.fn()} />);
    fireEvent.click(screen.getByRole("button", { name: "Cancelar" }));
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it("disables the confirm button and shows progress while loading", () => {
    render(
      <DeleteLeadDialog lead={lead} open onOpenChange={vi.fn()} onConfirm={vi.fn()} isLoading />,
    );
    const confirm = screen.getByRole("button", {
      name: "Excluindo...",
    }) as HTMLButtonElement;
    expect(confirm.disabled).toBe(true);
  });
});
