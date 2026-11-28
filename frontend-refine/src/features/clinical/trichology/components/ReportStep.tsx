"use client";

import { useEffect, useId, useState } from "react";
import { AlertTriangle, CheckCircle2, Copy, FileDown, Loader2, Sparkles } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import type { useAssessmentMutations } from "../hooks/useTrichology";
import { TrichologyService } from "../services/trichology.service";
import type { AssessmentDetail, ReportSection } from "../types/trichology.types";

type Props = {
  companyId: string;
  contactId: string;
  assessment: AssessmentDetail;
  canWrite: boolean;
  canApprove: boolean;
  mutations: ReturnType<typeof useAssessmentMutations>;
  onOpenAssessment: (id: string) => void;
};

const ORIGIN: Record<ReportSection["origin"], string> = {
  CODE: "Montado pelo sistema",
  AI: "Sugestão da IA",
  EDITED: "Editado",
};

/**
 * Editor do relatório por seção: marca o que veio da IA e o que foi editado,
 * mostra as pendências do guardrail e publica (PDF + documento + código).
 */
export function ReportStep({
  companyId,
  contactId,
  assessment,
  canWrite,
  canApprove,
  mutations,
  onOpenAssessment,
}: Props) {
  const uid = useId();
  const published = assessment.status === "PUBLISHED";
  const editable = canWrite && !published && assessment.status !== "CANCELLED";
  const [sections, setSections] = useState<ReportSection[]>(assessment.report?.sections ?? []);
  const [name, setName] = useState(assessment.professionalName ?? "");
  const [registry, setRegistry] = useState(assessment.professionalRegistry ?? "");
  const [downloading, setDownloading] = useState(false);

  useEffect(() => {
    setSections(assessment.report?.sections ?? []);
  }, [assessment.report]);

  const dirty =
    JSON.stringify(sections.map((s) => s.content)) !==
    JSON.stringify((assessment.report?.sections ?? []).map((s) => s.content));
  const violations = assessment.report?.guardrailViolations ?? [];
  const verifyUrl =
    assessment.verificationCode && typeof window !== "undefined"
      ? `${window.location.origin}/verificar-relatorio/${assessment.verificationCode}`
      : null;

  const download = async () => {
    setDownloading(true);
    try {
      const blob = await TrichologyService.pdf(companyId, contactId, assessment.id);
      const url = URL.createObjectURL(blob);
      window.open(url, "_blank", "noopener");
      setTimeout(() => URL.revokeObjectURL(url), 60_000);
    } finally {
      setDownloading(false);
    }
  };

  if (!assessment.report) {
    return (
      <div className="space-y-3">
        <p className="text-sm text-muted-foreground">
          O rascunho usa só os achados aceitos, as medidas confirmadas, a anamnese e o laboratório.
          Números e quadros são montados pelo sistema; a IA redige o texto para você revisar.
        </p>
        {editable && (
          <Button
            type="button"
            onClick={() => mutations.draft.mutate()}
            disabled={mutations.draft.isPending}
          >
            {mutations.draft.isPending ? (
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
            ) : (
              <Sparkles className="mr-2 h-4 w-4" />
            )}
            Gerar rascunho do relatório
          </Button>
        )}
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {published && (
        <div className="space-y-2 rounded-md border border-emerald-300 bg-emerald-50 p-3 text-sm text-emerald-900 dark:border-emerald-800 dark:bg-emerald-950 dark:text-emerald-100">
          <p className="flex items-center gap-2 font-medium">
            <CheckCircle2 className="h-4 w-4" /> Relatório publicado (versão{" "}
            {assessment.reportVersion})
          </p>
          <p>
            Código de verificação: <code>{assessment.verificationCode}</code>
            {verifyUrl && (
              <Button
                type="button"
                size="icon"
                variant="ghost"
                aria-label="Copiar link de verificação"
                onClick={() => void navigator.clipboard?.writeText(verifyUrl)}
              >
                <Copy className="h-4 w-4" />
              </Button>
            )}
          </p>
          <div className="flex flex-wrap gap-2">
            <Button type="button" size="sm" onClick={() => void download()} disabled={downloading}>
              <FileDown className="mr-2 h-4 w-4" /> Abrir PDF
            </Button>
            {canApprove && (
              <Button
                type="button"
                size="sm"
                variant="outline"
                disabled={mutations.newVersion.isPending}
                onClick={() =>
                  mutations.newVersion.mutate(undefined, {
                    onSuccess: (next) => onOpenAssessment(next.id),
                  })
                }
              >
                Corrigir (nova versão)
              </Button>
            )}
          </div>
        </div>
      )}

      {violations.length > 0 && (
        <div className="rounded-md border border-amber-300 bg-amber-50 p-3 text-sm text-amber-900 dark:border-amber-700 dark:bg-amber-950 dark:text-amber-100">
          <p className="flex items-center gap-2 font-medium">
            <AlertTriangle className="h-4 w-4" /> Corrija antes de publicar
          </p>
          <ul className="list-disc pl-5">
            {violations.map((v) => (
              <li key={v}>{v}</li>
            ))}
          </ul>
        </div>
      )}

      {sections.map((section, index) => (
        <div key={section.key} className="space-y-1">
          <div className="flex flex-wrap items-center gap-2">
            <Label htmlFor={`${uid}-${section.key}`} className="text-sm font-semibold">
              {section.title}
            </Label>
            <Badge variant={section.origin === "AI" ? "default" : "outline"}>
              {ORIGIN[section.origin]}
            </Badge>
          </div>
          <Textarea
            id={`${uid}-${section.key}`}
            rows={Math.min(14, Math.max(4, section.content.split("\n").length + 1))}
            value={section.content}
            disabled={!editable}
            onChange={(e) =>
              setSections((cur) =>
                cur.map((s, i) => (i === index ? { ...s, content: e.target.value } : s)),
              )
            }
          />
        </div>
      ))}

      {editable && (
        <div className="flex flex-wrap gap-2">
          <Button
            type="button"
            onClick={() => mutations.updateReport.mutate(sections)}
            disabled={mutations.updateReport.isPending}
          >
            {mutations.updateReport.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            Salvar revisão
          </Button>
          <Button
            type="button"
            variant="outline"
            onClick={() => mutations.draft.mutate()}
            disabled={mutations.draft.isPending}
          >
            Gerar rascunho de novo
          </Button>
        </div>
      )}

      {editable && canApprove && (
        <section className="space-y-2 rounded-md border p-3">
          <h3 className="text-sm font-semibold">Publicar</h3>
          {assessment.status !== "REVIEWED" || dirty ? (
            <p className="text-sm text-muted-foreground">
              Salve a revisão sem pendências para liberar a publicação.
            </p>
          ) : (
            <>
              <div className="grid gap-2 sm:grid-cols-2">
                <div className="space-y-1">
                  <Label htmlFor={`${uid}-name`}>Profissional que assina</Label>
                  <Input
                    id={`${uid}-name`}
                    value={name}
                    maxLength={160}
                    onChange={(e) => setName(e.target.value)}
                  />
                </div>
                <div className="space-y-1">
                  <Label htmlFor={`${uid}-registry`}>Registro profissional (opcional)</Label>
                  <Input
                    id={`${uid}-registry`}
                    value={registry}
                    maxLength={40}
                    placeholder="Ex.: CRM-SP 123456"
                    onChange={(e) => setRegistry(e.target.value)}
                  />
                </div>
              </div>
              <p className="text-xs text-muted-foreground">
                Depois de publicado o relatório não é editado; correções geram uma nova versão.
              </p>
              <Button
                type="button"
                onClick={() =>
                  mutations.approve.mutate({
                    professionalName: name,
                    professionalRegistry: registry,
                  })
                }
                disabled={mutations.approve.isPending || !name.trim()}
              >
                {mutations.approve.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}{" "}
                Aprovar e publicar
              </Button>
            </>
          )}
        </section>
      )}
    </div>
  );
}
