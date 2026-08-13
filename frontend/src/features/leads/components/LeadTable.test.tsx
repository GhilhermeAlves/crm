import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { LeadTable } from "./LeadTable";
import type { Lead } from "../types/lead.types";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
}));

function makeLead(overrides: Partial<Lead> = {}): Lead {
  return {
    id: "lead-1",
    companyId: "company-1",
    contactId: "contact-1",
    status: "NEW",
    score: 50,
    classification: "WARM",
    source: "WHATSAPP",
    campaignId: null,
    assignedTo: null,
    notes: null,
    createdAt: "2026-08-13T00:00:00",
    updatedAt: "2026-08-13T00:00:00",
    ...overrides,
  };
}

describe("LeadTable (Sprint 10)", () => {
  beforeEach(() => vi.clearAllMocks());

  it("renders the loading skeleton while loading", () => {
    const { container } = render(<LeadTable leads={[]} isLoading />);
    expect(container.querySelectorAll(".animate-pulse").length).toBe(5);
  });

  it("renders an empty state when there are no leads", () => {
    render(<LeadTable leads={[]} />);
    expect(screen.getByText("Nenhum lead encontrado")).toBeTruthy();
  });

  it("renders lead rows with their status, source and score", () => {
    render(<LeadTable leads={[makeLead()]} />);
    expect(screen.getByText("Novo")).toBeTruthy();
    expect(screen.getByText("WHATSAPP")).toBeTruthy();
    expect(screen.getByText("50")).toBeTruthy();
  });

  it("invokes onDelete when the delete action is triggered", async () => {
    const onDelete = vi.fn();
    const lead = makeLead();
    render(<LeadTable leads={[lead]} onDelete={onDelete} />);

    fireEvent.pointerDown(screen.getByRole("button"));
    const deleteItem = await screen.findByText("Excluir");
    fireEvent.click(deleteItem);

    expect(onDelete).toHaveBeenCalledWith(lead);
  });
});