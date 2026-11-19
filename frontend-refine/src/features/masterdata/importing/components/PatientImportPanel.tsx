"use client";

import { useState } from "react";
import { Undo2, Upload } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { toast } from "sonner";
import type { ImportRowStatus, PatientImportPreview } from "../types";
import { useImportPatients, useImports, usePreviewImport, useUndoImport } from "../useImports";

const STATUS_LABEL: Record<ImportRowStatus, string> = {
  OK: "Pronto",
  IMPORTADO: "Importado",
  DUPLICADO: "Duplicado",
  ERRO: "Erro",
  IGNORADO: "Ignorado",
};

function errorMessage(err: unknown): string {
  const data = (err as { response?: { data?: { message?: string } } })?.response?.data;
  return data?.message ?? "Não foi possível processar o arquivo.";
}

export function PatientImportPanel({ companyId }: { companyId: string }) {
  const [file, setFile] = useState<File | null>(null);
  const [result, setResult] = useState<PatientImportPreview | null>(null);
  const previewMut = usePreviewImport(companyId);
  const importMut = useImportPatients(companyId);
  const undoMut = useUndoImport(companyId);
  const { data: history = [] } = useImports(companyId);

  const shown = result ?? previewMut.data ?? null;
  const done = result !== null;

  function onFile(f: File | null) {
    setFile(f);
    setResult(null);
    if (f) previewMut.mutate(f, { onError: (e) => toast.error(errorMessage(e)) });
  }

  function onImport() {
    if (!file) return;
    importMut.mutate(file, {
      onSuccess: (r) => setResult(r),
      onError: (e) => toast.error(errorMessage(e)),
    });
  }

  function onUndo(id: string) {
    undoMut.mutate(id, {
      onSuccess: (r) =>
        toast.success(
          `${r.removed} paciente(s) removido(s)` +
            (r.kept ? `; ${r.kept} mantido(s) por já terem agenda ou procedimentos` : ""),
        ),
      onError: (e) => toast.error(errorMessage(e)),
    });
  }

  const imported = shown?.rows.filter((r) => r.status === "IMPORTADO").length ?? 0;

  return (
    <div className="space-y-6">
      <div className="space-y-2">
        <Label htmlFor="import-zip">Arquivo .zip exportado do outro sistema</Label>
        <Input
          id="import-zip"
          type="file"
          accept=".zip,application/zip"
          onChange={(e) => onFile(e.target.files?.[0] ?? null)}
        />
        <p className="text-sm text-muted-foreground">
          Nesta versão só os pacientes (patients.csv) são importados. Duplicados (CPF, prontuário ou
          e-mail) são pulados e nada é sobrescrito.
        </p>
      </div>

      {previewMut.isPending && <p className="text-sm">Analisando arquivo…</p>}

      {shown && (
        <div className="space-y-4">
          <div className="flex flex-wrap gap-2 text-sm">
            <Badge variant="secondary">Total: {shown.total}</Badge>
            <Badge>{done ? `${imported} paciente(s) importado(s)` : `Prontos: ${shown.ok}`}</Badge>
            <Badge variant="outline">Duplicados: {shown.duplicates}</Badge>
            <Badge variant="destructive">Erros: {shown.errors}</Badge>
            {shown.ignored > 0 && <Badge variant="outline">Ignorados: {shown.ignored}</Badge>}
          </div>
          {shown.unsupportedFiles.length > 0 && (
            <p className="text-sm text-muted-foreground">
              Ainda não suportado: {shown.unsupportedFiles.join(", ")}
            </p>
          )}
          {!done && (
            <Button onClick={onImport} disabled={shown.ok === 0 || importMut.isPending}>
              <Upload className="mr-2 h-4 w-4" />
              {importMut.isPending
                ? "Importando…"
                : `Importar ${shown.ok} paciente${shown.ok === 1 ? "" : "s"}`}
            </Button>
          )}
          <div className="max-h-[420px] overflow-auto rounded-md border">
            <table className="w-full text-sm">
              <thead className="sticky top-0 bg-muted">
                <tr className="text-left">
                  <th className="p-2">Linha</th>
                  <th className="p-2">Nome</th>
                  <th className="p-2">Situação</th>
                  <th className="p-2">Detalhe</th>
                </tr>
              </thead>
              <tbody>
                {shown.rows.map((r) => (
                  <tr key={r.line} className="border-t">
                    <td className="p-2">{r.line}</td>
                    <td className="p-2">{r.name || "—"}</td>
                    <td className="p-2">{STATUS_LABEL[r.status]}</td>
                    <td className="p-2">
                      {[r.message, ...r.warnings].filter(Boolean).join(" · ")}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {history.length > 0 && (
        <div className="space-y-2">
          <h3 className="font-medium">Importações anteriores</h3>
          <ul className="divide-y rounded-md border text-sm">
            {history.map((h) => (
              <li key={h.id} className="flex items-center justify-between gap-2 p-2">
                <span>
                  {new Date(h.createdAt).toLocaleString("pt-BR")} — {h.sourceFile ?? "arquivo"}:{" "}
                  {h.imported} importado(s), {h.skipped} pulado(s), {h.failed} erro(s)
                  {h.undoneAt && " · desfeita"}
                </span>
                {!h.undoneAt && (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => onUndo(h.id)}
                    disabled={undoMut.isPending}
                  >
                    <Undo2 className="mr-1 h-4 w-4" /> Desfazer
                  </Button>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
