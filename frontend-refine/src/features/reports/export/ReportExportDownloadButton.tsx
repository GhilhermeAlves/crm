"use client";

import { useState } from "react";
import { Download, Loader2 } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";

import { downloadReportExport } from "./report-export.service";
import { parseReportExportMetadata } from "./types";

type ReportExportDownloadButtonProps = {
  companyId: string | null;
  metadata: string | null;
  /** Chamado após o download (ex.: marcar a notificação como lida). */
  onDownloaded?: () => void;
};

/**
 * Botão "Baixar" das notificações de relatório pronto. Não renderiza nada se a
 * notificação não for de exportação ({@code metadata.kind !== "REPORT_EXPORT"}).
 */
export function ReportExportDownloadButton({
  companyId,
  metadata,
  onDownloaded,
}: ReportExportDownloadButtonProps) {
  const [busy, setBusy] = useState(false);
  const report = parseReportExportMetadata(metadata);
  if (!report || !companyId) return null;

  const handleClick = async (event: React.MouseEvent) => {
    event.preventDefault();
    event.stopPropagation();
    setBusy(true);
    try {
      await downloadReportExport(companyId, report.exportId, report.fileName);
      onDownloaded?.();
    } catch {
      toast.error("Não foi possível baixar o relatório.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <Button
      type="button"
      size="sm"
      variant="outline"
      className="h-7 gap-1 px-2 text-xs"
      disabled={busy}
      onClick={handleClick}
      onPointerDown={(event) => event.stopPropagation()}
    >
      {busy ? <Loader2 className="h-3 w-3 animate-spin" /> : <Download className="h-3 w-3" />}
      Baixar
    </Button>
  );
}
