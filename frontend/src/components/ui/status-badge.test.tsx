import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import {
  StatusBadge,
  LeadStatusBadgePreset,
  DealStageBadgePreset,
  UserStatusBadgePreset,
  ChannelStatusBadgePreset,
} from "./status-badge";

describe("StatusBadge Component", () => {
  it("renders with default props and text children", () => {
    render(<StatusBadge>Status Padrão</StatusBadge>);
    expect(screen.getByText("Status Padrão")).toBeTruthy();
  });

  it("applies semantic intent classes correctly", () => {
    const { container } = render(<StatusBadge intent="success">Ativo</StatusBadge>);
    const badge = container.querySelector("span");
    expect(badge?.className).toContain("text-emerald-800");
  });

  it("renders withDot indicator dot when withDot is true", () => {
    const { container } = render(<StatusBadge withDot>Com Indicador</StatusBadge>);
    const dots = container.querySelectorAll("span span");
    expect(dots.length).toBeGreaterThan(0);
  });

  it("renders with pulseDot animation when enabled", () => {
    const { container } = render(<StatusBadge withDot pulseDot>Pulsante</StatusBadge>);
    const pingDot = container.querySelector(".animate-ping");
    expect(pingDot).toBeTruthy();
  });

  it("renders solid appearance with proper high-contrast text", () => {
    const { container } = render(<StatusBadge intent="success" appearance="solid">Sólido</StatusBadge>);
    const badge = container.querySelector("span");
    expect(badge?.className).toContain("bg-emerald-600");
    expect(badge?.className).toContain("text-white");
  });
});

describe("StatusBadge Business Entity Presets", () => {
  it("LeadStatusBadgePreset maps statuses to pt-BR labels and intents", () => {
    const cases = [
      ["NEW", "Novo"],
      ["CONTACTED", "Contatado"],
      ["QUALIFIED", "Qualificado"],
      ["UNQUALIFIED", "Não qualificado"],
      ["CONVERTED", "Convertido"],
      ["LOST", "Perdido"],
    ];

    for (const [status, label] of cases) {
      const { unmount } = render(<LeadStatusBadgePreset status={status} />);
      expect(screen.getByText(label)).toBeTruthy();
      unmount();
    }
  });

  it("DealStageBadgePreset maps pipeline stages to pt-BR labels", () => {
    const stages = ["Novo", "Descoberta", "Proposta", "Negociação", "Fechado/Ganho", "Perdido"];

    for (const stage of stages) {
      const { unmount } = render(<DealStageBadgePreset stage={stage} />);
      expect(screen.getByText(stage)).toBeTruthy();
      unmount();
    }
  });

  it("UserStatusBadgePreset maps user statuses to pt-BR labels", () => {
    const statuses = [
      ["active", "Ativo"],
      ["inactive", "Inativo"],
      ["locked", "Bloqueado"],
      ["pending", "Pendente"],
    ];

    for (const [status, label] of statuses) {
      const { unmount } = render(<UserStatusBadgePreset status={status} />);
      expect(screen.getByText(label)).toBeTruthy();
      unmount();
    }
  });

  it("ChannelStatusBadgePreset maps omnichannel channel status", () => {
    const channels = [
      ["ACTIVE", "Conectado"],
      ["INACTIVE", "Desconectado"],
      ["ERROR", "Falha de Conexão"],
    ];

    for (const [status, label] of channels) {
      const { unmount } = render(<ChannelStatusBadgePreset status={status} />);
      expect(screen.getByText(label)).toBeTruthy();
      unmount();
    }
  });
});
