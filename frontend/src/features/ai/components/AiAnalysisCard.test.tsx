import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { AiAnalysisCard } from "./AiAnalysisCard";
import type { AiAnalysisResponse } from "../types/ai.types";

const fullAnalysis: AiAnalysisResponse = {
  summary: "Oportunidade no estágio Proposta há 18 dias.",
  facts: [
    {
      key: "opportunity.stage",
      label: "Estágio",
      value: "Proposta",
      source: "opportunity_context",
    },
  ],
  inferences: [
    { key: "inf-1", text: "Existe risco de perda de momentum.", confidence: 70 },
  ],
  recommendations: [
    {
      key: "rec-1",
      title: "Fazer follow-up com o contato",
      description: "Ligar para o contato nos próximos 2 dias.",
      priority: 80,
      justification: "Sem interação recente.",
      action: null,
    },
  ],
};

describe("AiAnalysisCard (AI-06)", () => {
  it("exibe resumo, fatos, inferências e recomendações separadamente", () => {
    render(<AiAnalysisCard analysis={fullAnalysis} loading={false} error={null} />);

    expect(screen.getByText("Análise contextual")).toBeTruthy();
    expect(screen.getByText("Oportunidade no estágio Proposta há 18 dias.")).toBeTruthy();
    expect(screen.getByLabelText("Fatos")).toBeTruthy();
    expect(screen.getByLabelText("Inferências")).toBeTruthy();
    expect(screen.getByLabelText("Recomendações")).toBeTruthy();

    expect(screen.getByText("Estágio")).toBeTruthy();
    expect(screen.getAllByText(/Proposta/).length).toBeGreaterThan(0);
    expect(screen.getByText(/Existe risco de perda de momentum/)).toBeTruthy();
    expect(screen.getByText("Fazer follow-up com o contato")).toBeTruthy();
  });

  it("diferencia visualmente fato, inferência e recomendação (badges)", () => {
    render(<AiAnalysisCard analysis={fullAnalysis} loading={false} error={null} />);

    const badges = screen.getAllByText(/Fatos|Inferências|Recomendações/);
    expect(badges.length).toBeGreaterThanOrEqual(3);
    // labels de seção reforçam a distinção (dado real vs interpretação vs sugestão)
    expect(screen.getByText("Dados reais do CRM")).toBeTruthy();
    expect(screen.getByText("Interpretação da IA")).toBeTruthy();
    expect(screen.getByText(/Sugestões — não executadas/)).toBeTruthy();
  });

  it("exibe empty states adequados quando não há dados (não 'Nenhum risco')", () => {
    const empty: AiAnalysisResponse = { summary: "Sem dados.", facts: [], inferences: [], recommendations: [] };
    render(<AiAnalysisCard analysis={empty} loading={false} error={null} />);

    expect(screen.getByText("Não há dados suficientes para avaliar.")).toBeTruthy();
    expect(screen.getByText("Sem inferências para este registro.")).toBeTruthy();
    expect(screen.getByText("Sem recomendações por enquanto.")).toBeTruthy();
    expect(screen.queryByText(/Nenhum risco/)).toBeNull();
  });

  it("mostra loading acessível enquanto analisa (sem apagar estado anterior)", () => {
    render(<AiAnalysisCard analysis={null} loading={true} error={null} />);
    expect(screen.getByLabelText("Analisando registro")).toBeTruthy();
    expect(screen.getByText(/Analisando o registro atual/)).toBeTruthy();
  });

  it("mostra erro amigável controlado", () => {
    render(
      <AiAnalysisCard
        analysis={null}
        loading={false}
        error="Você não tem permissão para acessar o contexto solicitado."
      />,
    );
    expect(screen.getByText("Você não tem permissão para acessar o contexto solicitado.")).toBeTruthy();
    expect(screen.queryByText(/stack trace/)).toBeNull();
  });
});