import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import * as React from "react";
import AgentConfigPage from "@/app/(dashboard)/settings/agent-config/page";
import type { AgentConfig } from "@/features/automation/ai/types/ai.types";

const { state, mutateMock } = vi.hoisted(() => ({
  state: { config: null as unknown },
  mutateMock: vi.fn(),
}));

vi.mock("@/features/automation/ai/hooks/useAi", () => ({
  useAiPermissions: () => ({ canManageAgentConfig: true }),
  useAgentConfig: () => ({ data: state.config, isLoading: false }),
  useUpdateAgentConfig: () => ({ mutate: mutateMock, isPending: false }),
}));

vi.mock("@/components/common/PageTitle", () => ({
  PageTitle: ({ children }: { children: React.ReactNode }) => <h1>{children}</h1>,
}));

const legacy: AgentConfig = {
  id: "1",
  aiEnabled: true,
  allowAutoReply: true,
  systemPrompt: "Você é a recepcionista da clínica.",
  model: null,
  temperature: null,
  maxTokens: null,
  cooldownMinutes: 5,
  maxChars: 500,
  updatedAt: null,
  voiceReplyMode: "MIRROR",
  identity: { name: null, description: null, persona: null },
  behavior: { objective: null, tone: null, rules: [], instructions: [] },
  conversation: {
    cooldownMinutes: 5,
    maxChars: 500,
    voiceReplyMode: "MIRROR",
    memory: { enabled: false },
  },
  tools: { humanTransferEnabled: false },
  usesLegacyPrompt: true,
};

describe("Configuração do agente — nova arquitetura", () => {
  beforeEach(() => {
    mutateMock.mockReset();
    state.config = legacy;
  });

  it("mostra as seções da arquitetura do agente", () => {
    render(<AgentConfigPage />);
    for (const title of ["Identidade", "Comportamento", "Ferramentas", "Modelo", "Conversação"]) {
      expect(screen.getAllByText(title).length).toBeGreaterThan(0);
    }
    expect(screen.getByText("Prompt legado")).toBeTruthy();
  });

  it("migra o prompt legado para a persona e envia regras como lista", () => {
    render(<AgentConfigPage />);

    fireEvent.click(screen.getByRole("button", { name: /Copiar para a Persona/ }));
    fireEvent.change(screen.getByLabelText("Regras (uma por linha)"), {
      target: { value: "- Não inventar informações.\n\n• Confirmar antes de agendar." },
    });
    fireEvent.click(screen.getByLabelText("Memória"));
    fireEvent.click(screen.getByRole("button", { name: /Salvar configuração/ }));

    expect(mutateMock).toHaveBeenCalledTimes(1);
    const request = mutateMock.mock.calls[0][0];
    expect(request.identity.persona).toBe("Você é a recepcionista da clínica.");
    expect(request.behavior.rules).toEqual([
      "Não inventar informações.",
      "Confirmar antes de agendar.",
    ]);
    expect(request.memoryEnabled).toBe(true);
    expect(request.systemPrompt).toBe("Você é a recepcionista da clínica.");
  });

  it("sem alterações, o botão salvar fica desabilitado", () => {
    render(<AgentConfigPage />);
    expect(
      (screen.getByRole("button", { name: /Salvar configuração/ }) as HTMLButtonElement).disabled,
    ).toBe(true);
  });
});
