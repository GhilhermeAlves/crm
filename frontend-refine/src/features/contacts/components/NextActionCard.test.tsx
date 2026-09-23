import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { NextActionCard } from "./NextActionCard";
import type { NextAction } from "../types/contact.types";

describe("NextActionCard (Sprint 13)", () => {
  it("renders the recommended follow-up action", () => {
    const action: NextAction = {
      type: "FOLLOW_UP",
      title: "Agendar follow-up",
      description: "Sem contato há 10 dias.",
      priority: 90,
    };
    render(<NextActionCard nextAction={action} />);
    expect(screen.getByText("Agendar follow-up")).toBeTruthy();
    expect(screen.getByText("Sem contato há 10 dias.")).toBeTruthy();
    expect(screen.getByText("Próxima ação")).toBeTruthy();
  });

  it("shows a neutral state when nothing is urgent", () => {
    render(
      <NextActionCard
        nextAction={{
          type: "NONE",
          title: "Tudo em dia",
          description: "Sem urgência.",
          priority: 0,
        }}
      />,
    );
    expect(screen.getByText("Tudo em dia")).toBeTruthy();
  });
});
