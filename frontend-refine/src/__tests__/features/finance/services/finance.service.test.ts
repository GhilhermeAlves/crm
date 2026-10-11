import { describe, it, expect, vi, beforeEach } from "vitest";
import { FinanceService } from "@/features/finance/services/finance.service";
import type {
  AccountingReportRequest,
  ListTransactionsFilters,
} from "@/features/finance/types/finance.types";

const { getMock, postMock, putMock, deleteMock } = vi.hoisted(() => ({
  getMock: vi.fn(),
  postMock: vi.fn(),
  putMock: vi.fn(),
  deleteMock: vi.fn(),
}));

vi.mock("@/lib/api", () => ({
  default: { get: getMock, post: postMock, put: putMock, delete: deleteMock },
}));

const COMPANY = "c-1";
const base = `/companies/${COMPANY}/finance`;

describe("FinanceService", () => {
  beforeEach(() => {
    getMock.mockReset();
    postMock.mockReset();
    putMock.mockReset();
    deleteMock.mockReset();
  });

  it("listTransactions busca a página com os filtros como params", async () => {
    getMock.mockResolvedValue({ data: { content: [] } });
    const filters: ListTransactionsFilters = {
      type: "INCOME",
      status: "OVERDUE",
      from: "2026-01-01",
      to: "2026-01-31",
      page: 0,
      pageSize: 20,
    };

    await FinanceService.listTransactions(COMPANY, filters);

    expect(getMock).toHaveBeenCalledWith(`${base}/transactions`, { params: filters });
  });

  it("createTransaction envia o corpo no POST", async () => {
    postMock.mockResolvedValue({ data: [] });
    const body = {
      type: "EXPENSE" as const,
      description: "Aluguel da sala",
      category: "RENT" as const,
      amountCents: 400000,
      paymentMethod: "BOLETO" as const,
      dueDate: "2026-01-10",
    };

    await FinanceService.createTransaction(COMPANY, body);

    expect(postMock).toHaveBeenCalledWith(`${base}/transactions`, body);
  });

  it("payTransaction faz POST em /pay com { paidAt }", async () => {
    postMock.mockResolvedValue({ data: { id: "t-1" } });

    await FinanceService.payTransaction(COMPANY, "t-1", "2026-01-10");

    expect(postMock).toHaveBeenCalledWith(`${base}/transactions/t-1/pay`, {
      paidAt: "2026-01-10",
    });
  });

  it("getHealth envia months como param", async () => {
    getMock.mockResolvedValue({ data: { months: [] } });

    await FinanceService.getHealth(COMPANY, 12);

    expect(getMock).toHaveBeenCalledWith(`${base}/health`, { params: { months: 12 } });
  });

  it("generateAccountingReport usa responseType blob e lê o nome do Content-Disposition", async () => {
    postMock.mockResolvedValue({
      data: new Blob(["x"]),
      headers: { "content-disposition": 'attachment; filename="balancete-2026-01.pdf"' },
    });
    const body: AccountingReportRequest = {
      type: "MONTHLY_ACCOUNTING",
      month: "2026-01",
      format: "PDF",
    };

    const result = await FinanceService.generateAccountingReport(COMPANY, body);

    expect(postMock).toHaveBeenCalledWith(`${base}/accounting-reports`, body, {
      responseType: "blob",
    });
    expect(result.blob).toBeInstanceOf(Blob);
    expect(result.fileName).toBe("balancete-2026-01.pdf");
  });
});
