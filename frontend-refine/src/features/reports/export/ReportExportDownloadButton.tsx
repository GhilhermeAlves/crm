"use client";

import { useState } from "react";
import { Download, Loader2 } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";

import { StorageService } from "@/features/masterdata/storage/services/storage.service";

import { downloadReportExport } from "./report-export.service";
import { parseReportExportMetadata } from "./types";

type StorageFileMetadata = { kind: "STORAGE_FILE"; objectId: string; fileName: string };

/** Notificação de arquivo do paciente enviado (backend: PatientFileNotifier). */
function parseStorageFileMetadata(metadata: string | null): StorageFileMetadata | null {
  if (!metadata) return null;
  try {
    const parsed = JSON.parse(metadata) as Partial<StorageFileMetadata>;
    if (parsed.kind !== "STORAGE_FILE" || !parsed.objectId || !parsed.fileName) return null;
    return parsed as StorageFileMetadata;
  } catch {
    return null;
  }
}

async function downloadStorageFile(companyId: string, file: StorageFileMetadata) {
  const blob = await StorageService.download(companyId, file.objectId);
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = file.fileName;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
}

type ReportExportDownloadButtonProps = {
  companyId: string | null;
  metadata: string | null;
  /** Chamado após o download (ex.: marcar a notificação como lida). */
  onDownloaded?: () => void;
};

/**
 * Botão "Fazer download" das notificações de relatório pronto e de arquivo do
 * paciente enviado. Não renderiza nada para outros tipos de notificação.
 */
export function ReportExportDownloadButton({
  companyId,
  metadata,
  onDownloaded,
}: ReportExportDownloadButtonProps) {
  const [busy, setBusy] = useState(false);
  const report = parseReportExportMetadata(metadata);
  const storageFile = report ? null : parseStorageFileMetadata(metadata);
  if ((!report && !storageFile) || !companyId) return null;

  const handleClick = async (event: React.MouseEvent) => {
    event.preventDefault();
    event.stopPropagation();
    setBusy(true);
    try {
      if (report) {
        await downloadReportExport(companyId, report.exportId, report.fileName);
      } else if (storageFile) {
        await downloadStorageFile(companyId, storageFile);
      }
      onDownloaded?.();
    } catch {
      toast.error(
        report ? "Não foi possível baixar o relatório." : "Não foi possível baixar o arquivo.",
      );
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
      Fazer download
    </Button>
  );
}
