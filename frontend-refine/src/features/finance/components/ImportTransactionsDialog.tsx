"use client";

import { useEffect, useRef, useState } from "react";
import { CheckCircle2, Download, FileText, Upload, X } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { cn } from "@/lib/utils";

import { useImportTransactions } from "../hooks/useFinance";
import { FinanceService } from "../services/finance.service";
import type { CsvImportResult } from "../types/finance.types";

const MAX_SIZE_BYTES = 5 * 1024 * 1024;

type ImportTransactionsDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  companyId: string | null;
};

const formatSize = (bytes: number): string => {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
};

const validateFile = (file: File): string | null => {
  if (!file.name.toLowerCase().endsWith(".csv")) return "Envie um arquivo no formato CSV.";
  if (file.size > MAX_SIZE_BYTES) return "O arquivo deve ter no máximo 5 MB.";
  return null;
};

export function ImportTransactionsDialog({
  open,
  onOpenChange,
  companyId,
}: ImportTransactionsDialogProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [dragging, setDragging] = useState(false);
  const [result, setResult] = useState<CsvImportResult | null>(null);

  const importTransactions = useImportTransactions(companyId);

  useEffect(() => {
    if (open) return;
    setFile(null);
    setError(null);
    setDragging(false);
    setResult(null);
  }, [open]);

  const selectFile = (next: File | null) => {
    if (!next) return;
    const message = validateFile(next);
    if (message) {
      setError(message);
      setFile(null);
      return;
    }
    setError(null);
    setFile(next);
  };

  const handleDownloadTemplate = async () => {
    if (!companyId) return;
    try {
      const blob = await FinanceService.downloadImportTemplate(companyId);
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = "modelo-lancamentos.csv";
      anchor.click();
      URL.revokeObjectURL(url);
    } catch {
      toast.error("Não foi possível baixar o modelo.");
    }
  };

  const handleSubmit = () => {
    if (!file) return;
    importTransactions.mutate(file, { onSuccess: (data) => setResult(data) });
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[520px]">
        <DialogHeader>
          <DialogTitle>Enviar tabela de lançamentos</DialogTitle>
        </DialogHeader>

        <div className="space-y-4">
          <p className="text-sm text-muted-foreground">
            Envie um arquivo CSV com um lançamento por linha. Use o modelo para ver as colunas
            aceitas.
          </p>

          <Button
            type="button"
            variant="ghost"
            size="sm"
            className="gap-2 px-0"
            onClick={handleDownloadTemplate}
          >
            <Download className="h-4 w-4" />
            Baixar modelo (.csv)
          </Button>

          {result ? (
            <div className="space-y-3">
              <div className="flex items-center gap-2 text-sm font-medium">
                <CheckCircle2 className="h-4 w-4 text-crm-success" />
                {result.imported} lançamentos importados
              </div>

              {result.errors.length > 0 && (
                <div className="space-y-2">
                  <p className="text-sm font-medium">Linhas com problema</p>
                  <div className="max-h-48 overflow-y-auto rounded-md border p-2">
                    <ul className="space-y-1 text-sm">
                      {result.errors.map((lineError, index) => (
                        <li key={`${lineError.line}-${index}`} className="text-muted-foreground">
                          Linha {lineError.line}: {lineError.message}
                        </li>
                      ))}
                    </ul>
                  </div>
                </div>
              )}
            </div>
          ) : (
            <div className="space-y-3">
              <input
                ref={inputRef}
                type="file"
                accept=".csv,text/csv"
                className="hidden"
                aria-label="Selecionar arquivo CSV"
                onChange={(event) => selectFile(event.target.files?.[0] ?? null)}
              />

              <div
                role="button"
                tabIndex={0}
                onClick={() => inputRef.current?.click()}
                onKeyDown={(event) => {
                  if (event.key === "Enter" || event.key === " ") inputRef.current?.click();
                }}
                onDragOver={(event) => {
                  event.preventDefault();
                  setDragging(true);
                }}
                onDragLeave={() => setDragging(false)}
                onDrop={(event) => {
                  event.preventDefault();
                  setDragging(false);
                  selectFile(event.dataTransfer.files?.[0] ?? null);
                }}
                className={cn(
                  "flex cursor-pointer flex-col items-center justify-center gap-2 rounded-lg border border-dashed px-4 py-10 text-center transition-colors",
                  dragging ? "border-primary bg-primary/5" : "border-input hover:bg-muted/40",
                )}
              >
                <Upload className="h-6 w-6 text-muted-foreground" />
                <p className="text-sm font-medium">Arraste o arquivo CSV ou clique para escolher</p>
                <p className="text-xs text-muted-foreground">até 5 MB</p>
              </div>

              {file && (
                <div className="flex items-center gap-3 rounded-md border px-3 py-2">
                  <FileText className="h-4 w-4 shrink-0 text-muted-foreground" />
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium">{file.name}</p>
                    <p className="text-xs text-muted-foreground">{formatSize(file.size)}</p>
                  </div>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="gap-1"
                    onClick={() => {
                      setFile(null);
                      setError(null);
                      if (inputRef.current) inputRef.current.value = "";
                    }}
                  >
                    <X className="h-3.5 w-3.5" />
                    Trocar
                  </Button>
                </div>
              )}

              {error && <p className="text-sm font-medium text-destructive">{error}</p>}
            </div>
          )}
        </div>

        <DialogFooter>
          {result ? (
            <Button type="button" onClick={() => onOpenChange(false)}>
              Concluir
            </Button>
          ) : (
            <>
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                Fechar
              </Button>
              <Button
                type="button"
                disabled={!file || importTransactions.isPending}
                onClick={handleSubmit}
              >
                {importTransactions.isPending ? "Enviando…" : "Enviar"}
              </Button>
            </>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
