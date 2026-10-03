import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { LeadStatusBadge, LeadSourceBadge, LeadClassificationBadge } from "./LeadBadges";

describe("LeadBadges (Sprint 10)", () => {
  it("renders the pt-BR label for each lead status", () => {
    const cases: Array<[string, string]> = [
      ["NEW", "Novo"],
      ["CONTACTED", "Contatado"],
      ["QUALIFIED", "Qualificado"],
      ["UNQUALIFIED", "Não qualificado"],
      ["CONVERTED", "Convertido"],
      ["LOST", "Perdido"],
    ];
    for (const [status, label] of cases) {
      const { unmount } = render(<LeadStatusBadge status={status as never} />);
      expect(screen.getByText(label)).toBeTruthy();
      unmount();
    }
  });

  it("falls back to NEW label for an unknown status", () => {
    render(<LeadStatusBadge status={"UNKNOWN" as never} />);
    expect(screen.getByText("Novo")).toBeTruthy();
  });

  it("renders the source as-is", () => {
    render(<LeadSourceBadge source="WHATSAPP" />);
    expect(screen.getByText("WHATSAPP")).toBeTruthy();
  });

  it("renders a dash for a missing classification", () => {
    render(<LeadClassificationBadge classification={null} />);
    expect(screen.getByText("—")).toBeTruthy();
  });

  it("renders the mapped label for a known classification", () => {
    const cases: Array<[string, string]> = [
      ["HOT", "Quente"],
      ["WARM", "Morno"],
      ["COLD", "Frio"],
      ["DISQUALIFIED", "Desqualificado"],
    ];
    for (const [value, label] of cases) {
      const { unmount } = render(<LeadClassificationBadge classification={value} />);
      expect(screen.getByText(label)).toBeTruthy();
      unmount();
    }
  });
});
