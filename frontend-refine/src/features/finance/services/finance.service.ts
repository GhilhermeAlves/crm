import api from "@/lib/api";
import type {
  AccountingReportRequest,
  AgendaReportResponse,
  CashFlowResponse,
  CreateFinancialTransactionRequest,
  CsvImportResult,
  FinancialTransaction,
  HeadlineResponse,
  HealthResponse,
  ListTransactionsFilters,
  OverviewResponse,
  PageResponse,
  ReportGranularity,
  RevenueDistributionResponse,
  SummaryResponse,
  TransactionFilters,
  UpdateFinancialTransactionRequest,
} from "../types/finance.types";

const base = (companyId: string) => `/companies/${companyId}/finance`;

type RangeParams = {
  from?: string;
  to?: string;
  granularity?: ReportGranularity;
};

function fileNameFromDisposition(disposition?: string): string | null {
  if (!disposition) return null;
  const match = /filename="?([^";]+)"?/i.exec(disposition);
  return match ? match[1].trim() : null;
}

export const FinanceService = {
  async listTransactions(
    companyId: string,
    filters: ListTransactionsFilters = {},
  ): Promise<PageResponse<FinancialTransaction>> {
    const response = await api.get<PageResponse<FinancialTransaction>>(
      `${base(companyId)}/transactions`,
      { params: filters },
    );
    return response.data;
  },

  async getTransaction(companyId: string, id: string): Promise<FinancialTransaction> {
    const response = await api.get<FinancialTransaction>(`${base(companyId)}/transactions/${id}`);
    return response.data;
  },

  async createTransaction(
    companyId: string,
    body: CreateFinancialTransactionRequest,
  ): Promise<FinancialTransaction[]> {
    const response = await api.post<FinancialTransaction[]>(
      `${base(companyId)}/transactions`,
      body,
    );
    return response.data;
  },

  async updateTransaction(
    companyId: string,
    id: string,
    body: UpdateFinancialTransactionRequest,
  ): Promise<FinancialTransaction> {
    const response = await api.put<FinancialTransaction>(
      `${base(companyId)}/transactions/${id}`,
      body,
    );
    return response.data;
  },

  async payTransaction(
    companyId: string,
    id: string,
    paidAt: string,
  ): Promise<FinancialTransaction> {
    const response = await api.post<FinancialTransaction>(
      `${base(companyId)}/transactions/${id}/pay`,
      { paidAt },
    );
    return response.data;
  },

  async cancelTransaction(companyId: string, id: string): Promise<FinancialTransaction> {
    const response = await api.post<FinancialTransaction>(
      `${base(companyId)}/transactions/${id}/cancel`,
    );
    return response.data;
  },

  async deleteTransaction(companyId: string, id: string): Promise<void> {
    await api.delete(`${base(companyId)}/transactions/${id}`);
  },

  async importTransactions(companyId: string, file: File): Promise<CsvImportResult> {
    const formData = new FormData();
    formData.append("file", file);
    const response = await api.post<CsvImportResult>(
      `${base(companyId)}/transactions/import`,
      formData,
      // `null` remove o default `application/json` para o browser montar o
      // multipart/form-data com o boundary correto.
      { headers: { "Content-Type": null } },
    );
    return response.data;
  },

  async downloadImportTemplate(companyId: string): Promise<Blob> {
    const response = await api.get<Blob>(`${base(companyId)}/transactions/import/template`, {
      responseType: "blob",
    });
    return response.data;
  },

  async getSummary(companyId: string, filters: TransactionFilters = {}): Promise<SummaryResponse> {
    const response = await api.get<SummaryResponse>(`${base(companyId)}/summary`, {
      params: filters,
    });
    return response.data;
  },

  async getHealth(companyId: string, months = 6): Promise<HealthResponse> {
    const response = await api.get<HealthResponse>(`${base(companyId)}/health`, {
      params: { months },
    });
    return response.data;
  },

  async getCashFlow(companyId: string, params: RangeParams = {}): Promise<CashFlowResponse> {
    const response = await api.get<CashFlowResponse>(`${base(companyId)}/cash-flow`, { params });
    return response.data;
  },

  async getRevenueDistribution(
    companyId: string,
    params: RangeParams & { catalogItemIds?: string[] } = {},
  ): Promise<RevenueDistributionResponse> {
    const response = await api.get<RevenueDistributionResponse>(
      `${base(companyId)}/revenue-distribution`,
      { params: { ...params, catalogItemIds: params.catalogItemIds?.join(",") } },
    );
    return response.data;
  },

  async getOverview(
    companyId: string,
    params: RangeParams & { professionalUserIds?: string[] } = {},
  ): Promise<OverviewResponse> {
    const response = await api.get<OverviewResponse>(`${base(companyId)}/overview`, {
      params: { ...params, professionalUserIds: params.professionalUserIds?.join(",") },
    });
    return response.data;
  },

  async getAgendaReport(
    companyId: string,
    params: RangeParams & {
      appointmentType?: string;
      statuses?: string[];
      professionalUserIds?: string[];
    } = {},
  ): Promise<AgendaReportResponse> {
    const response = await api.get<AgendaReportResponse>(`${base(companyId)}/agenda-report`, {
      params: {
        ...params,
        statuses: params.statuses?.join(","),
        professionalUserIds: params.professionalUserIds?.join(","),
      },
    });
    return response.data;
  },

  async getHeadline(companyId: string): Promise<HeadlineResponse> {
    const response = await api.get<HeadlineResponse>(`${base(companyId)}/headline`);
    return response.data;
  },

  async generateAccountingReport(
    companyId: string,
    body: AccountingReportRequest,
  ): Promise<{ blob: Blob; fileName: string }> {
    const response = await api.post<Blob>(`${base(companyId)}/accounting-reports`, body, {
      responseType: "blob",
    });
    const headers = response.headers as Record<string, string | undefined>;
    const fileName =
      fileNameFromDisposition(headers["content-disposition"]) ??
      (body.format === "PDF" ? "relatorio.pdf" : "relatorio.csv");
    return { blob: response.data, fileName };
  },

  async getAvailableMonths(companyId: string): Promise<string[]> {
    const response = await api.get<string[]>(
      `${base(companyId)}/accounting-reports/available-months`,
    );
    return response.data;
  },
};
