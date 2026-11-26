"use client";

import { useEffect, useId, useState } from "react";
import { Loader2, RefreshCw, Sparkles } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import type {
  AssessmentImageView,
  Finding,
  FindingCatalogItem,
  Measures,
  Severity,
} from "../types/trichology.types";
import { MEASURE_LABELS, QUALITY_LABELS, SEVERITY_LABELS } from "../types/trichology.types";

type Props = {
  image: AssessmentImageView | null;
  catalog: FindingCatalogItem[];
  editable: boolean;
  saving: boolean;
  onClose: () => void;
  onSave: (body: {
    acceptedFindings: Finding[];
    confirmedMeasures: Measures;
    includeInReport: boolean;
  }) => void;
  onReanalyze: () => void;
};

type Row = { code: string; label: string; severity: Severity | null; evidence: string | null };
const MEASURES = Object.keys(MEASURE_LABELS) as (keyof Measures)[];

/**
 * Revisão de uma foto: achados da IA são propostas (aceitar, ajustar a
 * intensidade ou descartar); só o aceito vai para o relatório. Estimativas da
 * IA viram medida apenas quando o profissional confirma o valor.
 */
export function ImageReviewDialog({
  image,
  catalog,
  editable,
  saving,
  onClose,
  onSave,
  onReanalyze,
}: Props) {
  const uid = useId();
  const [accepted, setAccepted] = useState<Record<string, Row>>({});
  const [measures, setMeasures] = useState<Record<string, string>>({});
  const [include, setInclude] = useState(true);
  const [manual, setManual] = useState("");

  useEffect(() => {
    if (!image) return;
    const rows: Record<string, Row> = {};
    image.acceptedFindings.forEach((f) => {
      rows[f.code] = {
        code: f.code,
        label: f.label ?? f.code,
        severity: f.severity,
        evidence: f.evidence,
      };
    });
    setAccepted(rows);
    const m: Record<string, string> = {};
    MEASURES.forEach((k) => {
      const v = image.confirmedMeasures[k];
      if (v !== undefined) m[k] = String(v);
    });
    setMeasures(m);
    setInclude(image.includeInReport);
    setManual("");
  }, [image]);

  if (!image) return null;
  const labelOf = (code: string) => catalog.find((c) => c.code === code)?.label ?? code;
  const analyzing = image.aiStatus === "PENDING" || image.aiStatus === "RUNNING";

  const toggle = (row: Row, on: boolean) =>
    setAccepted((cur) => {
      const next = { ...cur };
      if (on) next[row.code] = row;
      else delete next[row.code];
      return next;
    });

  const save = () => {
    const confirmed: Measures = {};
    MEASURES.forEach((k) => {
      const raw = measures[k];
      if (raw !== undefined && raw.trim() !== "" && !Number.isNaN(Number(raw)))
        confirmed[k] = Number(raw);
    });
    onSave({
      acceptedFindings: Object.values(accepted).map((r) => ({
        code: r.code,
        label: r.label,
        severity: r.severity,
        confidence: null,
        evidence: r.evidence,
        source: "PROFESSIONAL",
      })),
      confirmedMeasures: confirmed,
      includeInReport: include,
    });
  };

  const proposals = image.aiFindings.filter((f) => !accepted[f.code]);
  const sorted = [...proposals].sort((a, b) => Number(a.possible) - Number(b.possible));

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[94dvh] overflow-y-auto sm:max-w-5xl">
        <DialogHeader>
          <DialogTitle>
            {image.regionLabel}
            {image.magnification ? ` · ${image.magnification}` : ""}
          </DialogTitle>
          <DialogDescription>
            Sugestões da IA aparecem como propostas. Só o que você aceitar entra no relatório.
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-4 md:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
          <div className="space-y-2">
            {image.downloadUrl && (
              // eslint-disable-next-line @next/next/no-img-element -- imagem autenticada do storage
              <img
                src={image.downloadUrl}
                alt={`Tricoscopia ${image.regionLabel}`}
                className="w-full rounded-md bg-black object-contain"
              />
            )}
            <div className="flex flex-wrap gap-2 text-xs">
              <Badge variant="outline">Qualidade: {QUALITY_LABELS[image.qualityStatus]}</Badge>
              {image.aiImageQuality && image.aiImageQuality !== "OK" && (
                <Badge variant="outline">IA: {QUALITY_LABELS[image.aiImageQuality]}</Badge>
              )}
              <Badge variant="secondary">
                {analyzing
                  ? "Em análise pela IA…"
                  : image.aiStatus === "FAILED"
                    ? "Falha na análise da IA"
                    : image.aiStatus === "SKIPPED"
                      ? "Sem análise da IA"
                      : "Analisada pela IA"}
              </Badge>
            </div>
            {image.aiNotes && <p className="text-sm text-muted-foreground">IA: {image.aiNotes}</p>}
            {editable && (image.aiStatus === "FAILED" || image.aiStatus === "DONE") && (
              <Button type="button" variant="outline" size="sm" onClick={onReanalyze}>
                <RefreshCw className="mr-2 h-4 w-4" /> Analisar de novo
              </Button>
            )}
          </div>

          <div className="space-y-4">
            <section className="space-y-2">
              <h3 className="text-sm font-semibold">Achados aceitos</h3>
              {Object.values(accepted).length === 0 && (
                <p className="text-sm text-muted-foreground">Nenhum achado aceito.</p>
              )}
              {Object.values(accepted).map((row) => (
                <div
                  key={row.code}
                  className="flex flex-wrap items-center gap-2 rounded-md border p-2"
                >
                  <Checkbox
                    checked
                    disabled={!editable}
                    onCheckedChange={(c) => toggle(row, Boolean(c))}
                    aria-label={`Remover ${row.label}`}
                  />
                  <span className="flex-1 text-sm">{row.label}</span>
                  <Select
                    value={row.severity ?? "none"}
                    disabled={!editable}
                    onValueChange={(v) =>
                      toggle({ ...row, severity: v === "none" ? null : (v as Severity) }, true)
                    }
                  >
                    <SelectTrigger className="h-8 w-32">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="none">Sem intensidade</SelectItem>
                      {(Object.keys(SEVERITY_LABELS) as Severity[]).map((s) => (
                        <SelectItem key={s} value={s}>
                          {SEVERITY_LABELS[s]}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              ))}
            </section>

            {sorted.length > 0 && (
              <section className="space-y-2">
                <h3 className="flex items-center gap-1 text-sm font-semibold">
                  <Sparkles className="h-4 w-4" /> Propostas da IA
                </h3>
                {sorted.map((f) => (
                  <div
                    key={f.code}
                    className={`rounded-md border p-2 text-sm ${f.possible ? "opacity-70" : ""}`}
                  >
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="flex-1 font-medium">
                        {f.label ?? labelOf(f.code)}
                        {f.possible && <span className="ml-1 text-xs font-normal">(possível)</span>}
                      </span>
                      {f.severity && <Badge variant="outline">{SEVERITY_LABELS[f.severity]}</Badge>}
                      {f.confidence !== null && (
                        <span className="text-xs text-muted-foreground">
                          {Math.round(f.confidence * 100)}%
                        </span>
                      )}
                      {editable && (
                        <Button
                          type="button"
                          size="sm"
                          variant="outline"
                          onClick={() =>
                            toggle(
                              {
                                code: f.code,
                                label: f.label ?? labelOf(f.code),
                                severity: f.severity,
                                evidence: f.evidence,
                              },
                              true,
                            )
                          }
                        >
                          Aceitar
                        </Button>
                      )}
                    </div>
                    {f.evidence && (
                      <p className="mt-1 text-xs text-muted-foreground">{f.evidence}</p>
                    )}
                  </div>
                ))}
              </section>
            )}

            {editable && (
              <section className="flex items-end gap-2">
                <div className="flex-1 space-y-1">
                  <Label htmlFor={`${uid}-manual`}>Acrescentar achado</Label>
                  <Select value={manual} onValueChange={setManual}>
                    <SelectTrigger id={`${uid}-manual`}>
                      <SelectValue placeholder="Escolha no catálogo" />
                    </SelectTrigger>
                    <SelectContent>
                      {catalog
                        .filter((c) => c.active && !accepted[c.code])
                        .map((c) => (
                          <SelectItem key={c.code} value={c.code}>
                            {c.label}
                          </SelectItem>
                        ))}
                    </SelectContent>
                  </Select>
                </div>
                <Button
                  type="button"
                  variant="outline"
                  disabled={!manual}
                  onClick={() => {
                    toggle(
                      { code: manual, label: labelOf(manual), severity: null, evidence: null },
                      true,
                    );
                    setManual("");
                  }}
                >
                  Acrescentar
                </Button>
              </section>
            )}

            <section className="space-y-2">
              <h3 className="text-sm font-semibold">Medidas</h3>
              {MEASURES.map((k) => {
                const estimate = image.aiEstimates[k];
                return (
                  <div key={k} className="grid grid-cols-[1fr_7rem] items-center gap-2">
                    <Label htmlFor={`${uid}-${k}`} className="font-normal">
                      {MEASURE_LABELS[k]}
                      {estimate !== undefined && (
                        <button
                          type="button"
                          className="ml-2 text-xs text-primary underline"
                          disabled={!editable}
                          onClick={() => setMeasures((m) => ({ ...m, [k]: String(estimate) }))}
                        >
                          estimativa da IA: {estimate}
                        </button>
                      )}
                    </Label>
                    <Input
                      id={`${uid}-${k}`}
                      inputMode="decimal"
                      value={measures[k] ?? ""}
                      disabled={!editable}
                      onChange={(e) => setMeasures((m) => ({ ...m, [k]: e.target.value }))}
                    />
                  </div>
                );
              })}
              <p className="text-xs text-muted-foreground">
                A estimativa da IA só vira medida quando você confirma o valor; a contagem manual
                prevalece.
              </p>
            </section>

            <div className="flex items-center gap-2">
              <Switch
                id={`${uid}-include`}
                checked={include}
                onCheckedChange={setInclude}
                disabled={!editable}
              />
              <Label htmlFor={`${uid}-include`}>Usar esta foto no relatório</Label>
            </div>
          </div>
        </div>

        <DialogFooter>
          <Button type="button" variant="outline" onClick={onClose}>
            Fechar
          </Button>
          {editable && (
            <Button type="button" onClick={save} disabled={saving}>
              {saving && <Loader2 className="mr-2 h-4 w-4 animate-spin" />} Salvar revisão
            </Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
