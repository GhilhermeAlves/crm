import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";

import { RevenueReport } from "@/features/finance/components/reports/RevenueReport";

vi.mock("@/features/reports/export/useReportExports", () => ({
  useRequestReportExport: () => ({ mutate: vi.fn(), isPending: false }),
}));

vi.mock("@/features/finance/hooks/useFinance", () => ({
  useRevenueDistribution: () => ({
    data: { byProcedure: [], averageByProcedure: [], byProfessional: [] },
    isLoading: false,
    isError: false,
    refetch: () => {},
  }),
}));

vi.mock("@/features/masterdata/catalog/hooks/useCatalog", () => ({
  useCatalog: () => ({ data: { content: [] }, isLoading: false }),
}));

describe("RevenueReport", () => {
  it("mostra 'Sem resultados' nas seções vazias", () => {
    render(<RevenueReport companyId="c1" />);

    expect(screen.getAllByText("Receita por Procedimento").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Preço Médio por Procedimento").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Receita por Profissional").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Sem resultados").length).toBe(3);
  });
});
