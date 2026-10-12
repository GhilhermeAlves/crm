import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { ReportExportService } from "./report-export.service";
import type { ReportExportRequest } from "./types";

const POLL_PENDING_MS = 5_000;

export const reportExportsKey = (companyId: string | null) => ["report-exports", companyId];

/** Exportações recentes do usuário; consulta a cada 5 s enquanto houver alguma gerando. */
export function useReportExports(companyId: string | null) {
  return useQuery({
    queryKey: reportExportsKey(companyId),
    queryFn: () => ReportExportService.list(companyId as string),
    enabled: !!companyId,
    refetchInterval: (query) =>
      query.state.data?.some((item) => item.status === "PENDING") ? POLL_PENDING_MS : false,
  });
}

/**
 * Pede a geração do arquivo. O backend responde na hora (PENDING) e avisa no
 * sino quando o arquivo estiver pronto para baixar.
 */
export function useRequestReportExport(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: ReportExportRequest) =>
      ReportExportService.request(companyId as string, request),
    onSuccess: () => {
      toast.success("Gerando o relatório… você será avisado no sino quando estiver pronto.");
      queryClient.invalidateQueries({ queryKey: reportExportsKey(companyId) });
      queryClient.invalidateQueries({ queryKey: ["notifications", companyId] });
    },
    onError: (error: Error) => {
      toast.error(error.message || "Não foi possível pedir o relatório.");
    },
  });
}
