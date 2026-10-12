import api from "@/lib/api";
import { downloadBlob } from "@/features/finance/utils/accounting-report";
import type { ReportExport, ReportExportRequest } from "./types";

const base = (companyId: string) => `/companies/${companyId}/report-exports`;

export const ReportExportService = {
  async request(companyId: string, request: ReportExportRequest): Promise<ReportExport> {
    const response = await api.post<ReportExport>(base(companyId), request);
    return response.data;
  },

  async list(companyId: string): Promise<ReportExport[]> {
    const response = await api.get<ReportExport[]>(base(companyId));
    return response.data;
  },

  async download(companyId: string, exportId: string): Promise<Blob> {
    const response = await api.get<Blob>(`${base(companyId)}/${exportId}/download`, {
      responseType: "blob",
    });
    return response.data;
  },
};

/** Baixa o arquivo de uma exportação pronta (sino, notificações e lista recente). */
export async function downloadReportExport(
  companyId: string,
  exportId: string,
  fileName: string,
): Promise<void> {
  const blob = await ReportExportService.download(companyId, exportId);
  downloadBlob(blob, fileName);
}
