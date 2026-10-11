import { describe, it, expect, vi, beforeEach } from "vitest";

import { ReportsService } from "@/features/reports/services/reports.service";

const { postMock } = vi.hoisted(() => ({ postMock: vi.fn() }));

vi.mock("@/lib/api", () => ({
  default: { post: postMock },
}));

const COMPANY = "c-1";

describe("ReportsService", () => {
  beforeEach(() => {
    postMock.mockReset();
  });

  it("submitReportSuggestion envia categorias e notas no POST", async () => {
    postMock.mockResolvedValue({ data: { id: "s-1" } });

    const result = await ReportsService.submitReportSuggestion(COMPANY, {
      categories: ["FINANCE", "OTHER"],
      notes: "quero por profissional",
    });

    expect(postMock).toHaveBeenCalledWith(`/companies/${COMPANY}/report-suggestions`, {
      categories: ["FINANCE", "OTHER"],
      notes: "quero por profissional",
    });
    expect(result).toEqual({ id: "s-1" });
  });
});
