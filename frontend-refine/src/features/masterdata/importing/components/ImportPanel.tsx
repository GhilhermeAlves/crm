"use client";

import { useState } from "react";
import { Upload } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { toast } from "sonner";
import type { ImportKind, ImportRowStatus, PatientImportPreview } from "../types";
import { useImportData, usePreviewImport } from "../useImports";

const STATUS_LABEL: Record<ImportRowStatus, string> = {
  OK: "Pronto",
  IMPORTADO: "Importado",
  DUPLICADO: "Duplicado",
  ERRO: "Erro",
  IGNORADO: "Ignorado",
};

const TEXT: Record<ImportKind, { title: string; help: string; singular: string; plural: string }> =
  {
    patients: {
      title: "Pacientes",
      help: "Lê o patients.csv. Duplicados (CPF, prontuário ou e-mail) são pulados e nada é sobrescrito.",
      singular: "paciente",
      plural: "pacientes",
    },
    procedures: {
      title: "Procedimentos (odontograma)",
      help: "Lê procedures.csv e procedure_types.csv. Importe os pacientes antes. Não gera lançamentos financeiros.",
      singular: "procedimento",
      plural: "procedimentos",
    },
  };

export function errorMessage(err: unknown): string {
  const data = (err as { response?: { data?: { message?: string } } })?.response?.data;
  return data?.message ?? "Não foi possível processar o arquivo.";
}

export function ImportPanel({ companyId, kind }: { companyId: string; kind: ImportKind }) {
  const t = TEXT[kind];
  const inputId = `import-zip-${kind}`;
  const [file, setFile] = useState<File | null>(null);
  const [result, setResult] = useState<PatientImportPreview | null>(null);
  const previewMut = usePreviewImport(companyId, kind);
  const importMut = useImportData(companyId, kind);

  const shown = result ?? previewMut.data ?? null;
  const done = result !== null;
  const imported = shown?.rows.filter((r) => r.status === "IMPORTADO").length ?? 0;

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

  return (
    <section className="space-y-4 rounded-md border p-4">
      <h2 className="font-medium">{t.title}</h2>
      <div className="space-y-2">
        <Label htmlFor={inputId}>Arquivo .zip exportado do outro sistema</Label>
        <Input
          id={inputId}
          type="file"
          accept=".zip,application/zip"
          onChange={(e) => onFile(e.target.files?.[0] ?? null)}
        />
        <p className="text-sm text-muted-foreground">{t.help}</p>
      </div>

      {previewMut.isPending && <p className="text-sm">Analisando arquivo…</p>}

      {shown && (
        <div className="space-y-4">
          <div className="flex flex-wrap gap-2 text-sm">
            <Badge variant="secondary">Total: {shown.total}</Badge>
            <Badge>
              {done ? `${imported} ${t.singular}(s) importado(s)` : `Prontos: ${shown.ok}`}
            </Badge>
            <Badge variant="outline">Duplicados: {shown.duplicates}</Badge>
            <Badge variant="destructive">Erros: {shown.errors}</Badge>
            {shown.ignored > 0 && <Badge variant="outline">Ignorados: {shown.ignored}</Badge>}
          </div>
          {shown.notices.map((n) => (
            <p key={n} className="text-sm">
              {n}
            </p>
          ))}
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
                : `Importar ${shown.ok} ${shown.ok === 1 ? t.singular : t.plural}`}
            </Button>
          )}
          <div className="max-h-[420px] overflow-auto rounded-md border">
            <table className="w-full text-sm">
              <thead className="sticky top-0 bg-muted">
                <tr className="text-left">
                  <th className="p-2">Linha</th>
                  <th className="p-2">{kind === "patients" ? "Nome" : "Procedimento"}</th>
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
    </section>
  );
}
