"use client";

import { useState } from "react";
import { Download, Loader2 } from "lucide-react";
import { toast } from "sonner";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { formatRelativeTime } from "@/features/communication/notifications/lib/format";

import { downloadReportExport } from "./report-export.service";
import type { ReportExport, ReportExportStatus } from "./types";

type RecentReportExportsProps = {
  companyId: string | null;
  exports: ReportExport[];
  isLoading: boolean;
};

const STATUS: Record<
  ReportExportStatus,
  { label: string; variant: "secondary" | "default" | "destructive" }
> = {
  PENDING: { label: "Gerando", variant: "secondary" },
  READY: { label: "Pronto", variant: "default" },
  FAILED: { label: "Falhou", variant: "destructive" },
};

function formatDate(value: string): string {
  const [year, month, day] = value.split("-");
  return `${day}/${month}/${year}`;
}

export function RecentReportExports({ companyId, exports, isLoading }: RecentReportExportsProps) {
  const [downloadingId, setDownloadingId] = useState<string | null>(null);

  const handleDownload = async (item: ReportExport) => {
    if (!companyId || !item.fileName) return;
    setDownloadingId(item.id);
    try {
      await downloadReportExport(companyId, item.id, item.fileName);
    } catch {
      toast.error("Não foi possível baixar o arquivo.");
    } finally {
      setDownloadingId(null);
    }
  };

  return (
    <Card className="p-5">
      <h2 className="mb-3 text-sm font-semibold">Exportações recentes</h2>
      {isLoading ? (
        <Skeleton className="h-16 w-full" />
      ) : exports.length === 0 ? (
        <p className="text-sm text-muted-foreground">Nenhum relatório gerado ainda.</p>
      ) : (
        <ul className="divide-y">
          {exports.map((item) => {
            const status = STATUS[item.status];
            return (
              <li
                key={item.id}
                className="flex flex-wrap items-center justify-between gap-2 py-2.5"
                data-testid="report-export-item"
              >
                <div className="min-w-0 space-y-0.5">
                  <p className="truncate text-sm font-medium">
                    {item.typeLabel} · {item.format === "PDF" ? "PDF" : "Excel"}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {formatDate(item.periodFrom)} a {formatDate(item.periodTo)} · pedido{" "}
                    {formatRelativeTime(item.createdAt)}
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <Badge variant={status.variant}>
                    {item.status === "PENDING" && <Loader2 className="mr-1 h-3 w-3 animate-spin" />}
                    {status.label}
                  </Badge>
                  {item.status === "READY" && (
                    <Button
                      type="button"
                      size="sm"
                      variant="outline"
                      disabled={downloadingId === item.id}
                      onClick={() => handleDownload(item)}
                    >
                      <Download className="h-4 w-4" />
                      Baixar
                    </Button>
                  )}
                </div>
              </li>
            );
          })}
        </ul>
      )}
    </Card>
  );
}
