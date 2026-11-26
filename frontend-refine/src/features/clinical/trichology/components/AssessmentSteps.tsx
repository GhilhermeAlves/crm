"use client";

import { useEffect, useId, useState } from "react";
import { format } from "date-fns";
import { Camera, CheckCircle2, Copy, Link2, Loader2, Plus, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
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
import { Textarea } from "@/components/ui/textarea";
import { TrichoscopyCamera } from "@/features/clinical/trichoscopy/components/TrichoscopyCamera";
import type { useAssessmentMutations } from "../hooks/useTrichology";
import { useQuizDefinition, useTrichologySettings } from "../hooks/useTrichology";
import type {
  AssessmentDetail,
  AssessmentImageView,
  CopilotView,
  PullTestSite,
  QuizAnswers,
} from "../types/trichology.types";
import { QUALITY_LABELS } from "../types/trichology.types";
import { ImageReviewDialog } from "./ImageReviewDialog";
import { QuizForm, missingRequired } from "./QuizForm";

type Mutations = ReturnType<typeof useAssessmentMutations>;
type StepProps = {
  companyId: string;
  contactId: string;
  assessment: AssessmentDetail;
  editable: boolean;
  mutations: Mutations;
};

// ------------------------------------------------------------------ quiz

export function QuizStep({ companyId, contactId, assessment, editable, mutations }: StepProps) {
  const definition = useQuizDefinition(companyId, contactId);
  const [answers, setAnswers] = useState<QuizAnswers>(assessment.quizAnswers ?? {});
  const [showMissing, setShowMissing] = useState(false);
  const [link, setLink] = useState<string | null>(null);

  useEffect(() => {
    setAnswers(assessment.quizAnswers ?? {});
  }, [assessment.quizAnswers]);

  if (!definition.data) return <p className="text-sm text-muted-foreground">Carregando quiz…</p>;
  const questions = definition.data.questions;

  const complete = () => {
    if (missingRequired(questions, answers).length > 0) {
      setShowMissing(true);
      toast.error("Responda as perguntas obrigatórias.");
      return;
    }
    mutations.saveQuiz.mutate({ answers, complete: true });
  };

  const generateLink = () =>
    mutations.quizLink.mutate(undefined, {
      onSuccess: (data) => {
        setLink(data.url);
        void navigator.clipboard?.writeText(data.url).then(
          () => toast.success("Link copiado. Envie ao paciente."),
          () => undefined,
        );
      },
    });

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center gap-2">
        {assessment.quizCompletedAt ? (
          <Badge variant="secondary">
            <CheckCircle2 className="mr-1 h-3 w-3" /> Respondido em{" "}
            {format(new Date(assessment.quizCompletedAt), "dd/MM/yyyy HH:mm")}
          </Badge>
        ) : (
          <Badge variant="outline">Não respondido</Badge>
        )}
        {editable && (
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={generateLink}
            disabled={mutations.quizLink.isPending}
          >
            <Link2 className="mr-2 h-4 w-4" /> Link para o paciente responder
          </Button>
        )}
      </div>
      {(link || assessment.quizLinkExpiresAt) && !assessment.quizCompletedAt && (
        <div className="rounded-md border bg-muted/40 p-3 text-sm">
          {link ? (
            <div className="flex flex-wrap items-center gap-2">
              <code className="break-all">{link}</code>
              <Button
                type="button"
                size="icon"
                variant="ghost"
                aria-label="Copiar link"
                onClick={() => void navigator.clipboard?.writeText(link)}
              >
                <Copy className="h-4 w-4" />
              </Button>
            </div>
          ) : (
            <span>Link enviado ao paciente.</span>
          )}
          {assessment.quizLinkExpiresAt && (
            <p className="text-xs text-muted-foreground">
              Válido até {format(new Date(assessment.quizLinkExpiresAt), "dd/MM/yyyy HH:mm")}. Gerar
              outro link invalida o anterior.
            </p>
          )}
        </div>
      )}
      <QuizForm
        questions={questions}
        answers={answers}
        onChange={setAnswers}
        disabled={!editable}
        showMissing={showMissing}
      />
      {editable && (
        <div className="flex flex-wrap gap-2">
          <Button type="button" onClick={complete} disabled={mutations.saveQuiz.isPending}>
            {mutations.saveQuiz.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            Concluir quiz
          </Button>
          <Button
            type="button"
            variant="outline"
            onClick={() => mutations.saveQuiz.mutate({ answers, complete: false })}
            disabled={mutations.saveQuiz.isPending}
          >
            Salvar parcial
          </Button>
        </div>
      )}
    </div>
  );
}

// ------------------------------------------------------------------ captura

function aiLabel(image: AssessmentImageView): string {
  switch (image.aiStatus) {
    case "PENDING":
    case "RUNNING":
      return "Em análise";
    case "FAILED":
      return "Falha da IA";
    case "SKIPPED":
      return "Sem IA";
    default:
      return image.reviewedAt ? "Revisada" : `${image.aiFindings.length} proposta(s)`;
  }
}

export function CaptureStep({ companyId, contactId, assessment, editable, mutations }: StepProps) {
  const settings = useTrichologySettings(companyId);
  const [cameraOpen, setCameraOpen] = useState(false);
  const [reviewing, setReviewing] = useState<string | null>(null);
  const [finishing, setFinishing] = useState(false);
  const [reasons, setReasons] = useState<Record<string, string>>({});
  const image = assessment.images.find((i) => i.id === reviewing) ?? null;
  const regions = assessment.regions.map((r) => ({
    ...r,
    magnifications:
      assessment.protocol?.regions.find((p) => p.code === r.code)?.magnifications ?? [],
  }));
  const short = assessment.regions.filter((r) => r.images < r.minImages && !r.skipped);

  const finish = () =>
    mutations.imagingDone.mutate(
      short.map((r) => ({ regionCode: r.code, reason: reasons[r.code] ?? "" })),
      { onSuccess: () => setFinishing(false) },
    );

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap gap-2">
        {assessment.regions.map((r) => (
          <Badge
            key={r.code}
            variant={r.images >= r.minImages ? "secondary" : "outline"}
            title={r.skipReason ?? undefined}
          >
            {r.label}: {r.images}/{r.minImages}
            {r.skipped ? " · pulada" : ""}
          </Badge>
        ))}
      </div>
      {editable && (
        <div className="flex flex-wrap gap-2">
          <Button type="button" onClick={() => setCameraOpen(true)}>
            <Camera className="mr-2 h-4 w-4" /> Capturar fotos
          </Button>
          <Button type="button" variant="outline" onClick={() => setFinishing(true)}>
            Concluir captura
          </Button>
        </div>
      )}

      {assessment.images.length === 0 ? (
        <p className="text-sm text-muted-foreground">Nenhuma foto no exame ainda.</p>
      ) : (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
          {assessment.images.map((img) => (
            <button
              key={img.id}
              type="button"
              className="overflow-hidden rounded-md border text-left hover:ring-2 hover:ring-primary"
              onClick={() => setReviewing(img.id)}
            >
              {img.downloadUrl && (
                // eslint-disable-next-line @next/next/no-img-element -- imagem autenticada do storage
                <img
                  src={img.downloadUrl}
                  alt={img.regionLabel}
                  className="aspect-video w-full bg-black object-cover"
                />
              )}
              <div className="space-y-1 p-2 text-xs">
                <div className="font-medium">
                  {img.regionLabel}
                  {img.magnification ? ` · ${img.magnification}` : ""}
                </div>
                <div className="flex flex-wrap gap-1">
                  <Badge variant="outline">{aiLabel(img)}</Badge>
                  {img.qualityStatus !== "OK" && img.qualityStatus !== "UNCHECKED" && (
                    <Badge variant="outline">{QUALITY_LABELS[img.qualityStatus]}</Badge>
                  )}
                  {!img.includeInReport && <Badge variant="outline">Fora do relatório</Badge>}
                </div>
              </div>
            </button>
          ))}
        </div>
      )}

      <Dialog open={cameraOpen} onOpenChange={setCameraOpen}>
        <DialogContent className="flex h-[96dvh] w-[96vw] max-w-[1600px] flex-col overflow-y-auto sm:max-w-[1600px]">
          <DialogHeader>
            <DialogTitle>Captura guiada</DialogTitle>
            <DialogDescription>
              Escolha a região, capture e salve. A câmera continua aberta para a próxima região.
            </DialogDescription>
          </DialogHeader>
          {cameraOpen && (
            <TrichoscopyCamera
              companyId={companyId}
              contactId={contactId}
              exam={{
                regions,
                saving: mutations.capture.isPending,
                onCapture: (input) => mutations.capture.mutateAsync(input),
              }}
            />
          )}
        </DialogContent>
      </Dialog>

      <ImageReviewDialog
        image={image}
        catalog={settings.data?.findings ?? []}
        editable={editable}
        saving={mutations.reviewImage.isPending}
        onClose={() => setReviewing(null)}
        onReanalyze={() => image && mutations.reanalyze.mutate(image.id)}
        onSave={(body) =>
          image &&
          mutations.reviewImage.mutate(
            { imageId: image.id, ...body },
            { onSuccess: () => setReviewing(null) },
          )
        }
      />

      <Dialog open={finishing} onOpenChange={setFinishing}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Concluir captura</DialogTitle>
            <DialogDescription>
              {short.length === 0
                ? "Todas as regiões têm o mínimo de fotos."
                : "Informe o motivo para as regiões sem o mínimo de fotos."}
            </DialogDescription>
          </DialogHeader>
          {short.map((r) => (
            <div key={r.code} className="space-y-1">
              <Label htmlFor={`skip-${r.code}`}>
                {r.label} ({r.images}/{r.minImages})
              </Label>
              <Input
                id={`skip-${r.code}`}
                value={reasons[r.code] ?? ""}
                maxLength={300}
                placeholder="Ex.: sem queixa nesta região"
                onChange={(e) => setReasons((cur) => ({ ...cur, [r.code]: e.target.value }))}
              />
            </div>
          ))}
          <DialogFooter>
            <Button
              type="button"
              onClick={finish}
              disabled={
                mutations.imagingDone.isPending ||
                short.some((r) => !(reasons[r.code] ?? "").trim())
              }
            >
              Concluir
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

// ------------------------------------------------------------------ exame clínico

export function ClinicalStep({ assessment, editable, mutations }: StepProps) {
  const uid = useId();
  const [sites, setSites] = useState<PullTestSite[]>(assessment.pullTest?.sites ?? []);
  const [notes, setNotes] = useState(assessment.clinicalNotes ?? "");
  const [noPullTest, setNoPullTest] = useState(false);

  const save = () =>
    mutations.clinical.mutate({
      pullTest: noPullTest || sites.length === 0 ? null : { sites, notes: null },
      notes,
    });

  return (
    <div className="space-y-4">
      <section className="space-y-2">
        <h3 className="text-sm font-semibold">Pull test</h3>
        <p className="text-xs text-muted-foreground">
          Tração suave de ~60 fios; positivo quando saem mais de 6 fios.
        </p>
        {sites.map((site, index) => (
          <div key={index} className="flex flex-wrap items-end gap-2">
            <div className="min-w-48 flex-1 space-y-1">
              <Label htmlFor={`${uid}-r${index}`}>Região</Label>
              <Select
                value={site.regionCode}
                disabled={!editable}
                onValueChange={(v) =>
                  setSites((cur) => cur.map((s, i) => (i === index ? { ...s, regionCode: v } : s)))
                }
              >
                <SelectTrigger id={`${uid}-r${index}`}>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {assessment.regions.map((r) => (
                    <SelectItem key={r.code} value={r.code}>
                      {r.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="w-32 space-y-1">
              <Label htmlFor={`${uid}-h${index}`}>Fios soltos</Label>
              <Input
                id={`${uid}-h${index}`}
                type="number"
                min={0}
                max={200}
                value={site.hairsPulled}
                disabled={!editable}
                onChange={(e) =>
                  setSites((cur) =>
                    cur.map((s, i) =>
                      i === index ? { ...s, hairsPulled: Number(e.target.value) } : s,
                    ),
                  )
                }
              />
            </div>
            <Badge variant={site.hairsPulled > 6 ? "destructive" : "secondary"}>
              {site.hairsPulled > 6 ? "Positivo" : "Negativo"}
            </Badge>
            {editable && (
              <Button
                type="button"
                variant="ghost"
                size="icon"
                aria-label="Remover região do pull test"
                onClick={() => setSites((cur) => cur.filter((_, i) => i !== index))}
              >
                <Trash2 className="h-4 w-4" />
              </Button>
            )}
          </div>
        ))}
        {editable && assessment.regions.length > 0 && (
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => {
              setNoPullTest(false);
              setSites((cur) => [
                ...cur,
                { regionCode: assessment.regions[0].code, hairsPulled: 0 },
              ]);
            }}
          >
            <Plus className="mr-2 h-4 w-4" /> Região no pull test
          </Button>
        )}
      </section>
      <div className="space-y-1">
        <Label htmlFor={`${uid}-notes`}>Observações do exame presencial</Label>
        <Textarea
          id={`${uid}-notes`}
          rows={5}
          maxLength={10000}
          value={notes}
          disabled={!editable}
          onChange={(e) => setNotes(e.target.value)}
        />
      </div>
      {editable && (
        <Button type="button" onClick={save} disabled={mutations.clinical.isPending}>
          {mutations.clinical.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />} Salvar
          exame clínico
        </Button>
      )}
    </div>
  );
}

// ------------------------------------------------------------------ laboratório

export function LabsStep({
  companyId,
  assessment,
  editable,
  mutations,
  copilot,
}: StepProps & { copilot: CopilotView | undefined }) {
  const uid = useId();
  const settings = useTrichologySettings(companyId);
  const sex = assessment.quizAnswers?.sex as string | undefined;
  const markers = (settings.data?.markers ?? []).filter(
    (m) => m.active && (m.sex === null || !sex || m.sex === sex),
  );
  const codes = Array.from(new Set(markers.map((m) => m.code)));
  const [requested, setRequested] = useState<string[]>(assessment.requestedExams ?? []);
  const [values, setValues] = useState<Record<string, string>>(() => {
    const v: Record<string, string> = {};
    (assessment.labResults ?? []).forEach((r) => (v[r.markerCode] = String(r.value)));
    return v;
  });
  const [collectedOn, setCollectedOn] = useState(assessment.labCollectedOn ?? "");

  const markerOf = (code: string) => markers.find((m) => m.code === code);
  const shown = requested.length > 0 ? requested : codes;

  const toggle = (code: string) =>
    setRequested((cur) => (cur.includes(code) ? cur.filter((c) => c !== code) : [...cur, code]));

  const saveLabs = () =>
    mutations.labs.mutate({
      results: shown
        .filter((code) => (values[code] ?? "").trim() !== "" && !Number.isNaN(Number(values[code])))
        .map((code) => ({
          markerCode: code,
          value: Number(values[code]),
          unit: markerOf(code)?.unit ?? null,
        })),
      collectedOn: collectedOn || null,
    });

  return (
    <div className="space-y-5">
      <section className="space-y-2">
        <h3 className="text-sm font-semibold">Exames a pedir</h3>
        {copilot && copilot.suggestedExams.length > 0 && (
          <div className="rounded-md border bg-muted/40 p-3 text-sm">
            <p className="mb-2 font-medium">Sugeridos pelo copiloto</p>
            <div className="flex flex-wrap gap-2">
              {copilot.suggestedExams.map((s) => (
                <Button
                  key={s.markerCode}
                  type="button"
                  size="sm"
                  variant={requested.includes(s.markerCode) ? "secondary" : "outline"}
                  disabled={!editable}
                  title={s.reason}
                  onClick={() => toggle(s.markerCode)}
                >
                  {s.label}
                </Button>
              ))}
            </div>
          </div>
        )}
        <div className="flex flex-wrap gap-2">
          {codes.map((code) => (
            <Button
              key={code}
              type="button"
              size="sm"
              variant={requested.includes(code) ? "secondary" : "outline"}
              disabled={!editable}
              onClick={() => toggle(code)}
            >
              {markerOf(code)?.label ?? code}
            </Button>
          ))}
        </div>
        {editable && (
          <Button
            type="button"
            variant="outline"
            onClick={() => mutations.exams.mutate(requested)}
            disabled={mutations.exams.isPending || requested.length === 0}
          >
            Registrar pedido ({requested.length})
          </Button>
        )}
      </section>

      <section className="space-y-2">
        <h3 className="text-sm font-semibold">Resultados</h3>
        <div className="w-48 space-y-1">
          <Label htmlFor={`${uid}-date`}>Data da coleta</Label>
          <Input
            id={`${uid}-date`}
            type="date"
            value={collectedOn}
            disabled={!editable}
            onChange={(e) => setCollectedOn(e.target.value)}
          />
        </div>
        <div className="grid gap-2 sm:grid-cols-2">
          {shown.map((code) => {
            const m = markerOf(code);
            return (
              <div key={code} className="grid grid-cols-[1fr_7rem] items-center gap-2">
                <Label htmlFor={`${uid}-${code}`} className="font-normal">
                  {m?.label ?? code}
                  {m?.unit ? ` (${m.unit})` : ""}
                  {m?.referenceMin !== null && m?.referenceMin !== undefined && (
                    <span className="block text-xs text-muted-foreground">
                      ref. {m.referenceMin} a {m.referenceMax}
                    </span>
                  )}
                </Label>
                <Input
                  id={`${uid}-${code}`}
                  inputMode="decimal"
                  value={values[code] ?? ""}
                  disabled={!editable}
                  onChange={(e) => setValues((cur) => ({ ...cur, [code]: e.target.value }))}
                />
              </div>
            );
          })}
        </div>
        {editable && (
          <Button type="button" onClick={saveLabs} disabled={mutations.labs.isPending}>
            {mutations.labs.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />} Lançar
            resultados e calcular RPS
          </Button>
        )}
      </section>

      {assessment.rpsSummary && assessment.rpsSummary.markers.length > 0 && (
        <section className="space-y-2">
          <h3 className="text-sm font-semibold">RPS: {assessment.rpsSummary.overallScore}/100</h3>
          <div className="flex flex-wrap gap-2">
            {assessment.rpsSummary.systems.map((s) => (
              <Badge key={s.system} variant="outline">
                {s.system}: {s.score}
              </Badge>
            ))}
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b text-left text-muted-foreground">
                  <th className="py-1 pr-2">Marcador</th>
                  <th className="py-1 pr-2">Resultado</th>
                  <th className="py-1 pr-2">Quartil</th>
                  <th className="py-1">Nota</th>
                </tr>
              </thead>
              <tbody>
                {assessment.rpsSummary.markers.map((m) => (
                  <tr key={m.markerCode} className="border-b">
                    <td className="py-1 pr-2">{m.label}</td>
                    <td className="py-1 pr-2">
                      {m.value} {m.unit}
                    </td>
                    <td className="py-1 pr-2">
                      {m.position === "LOW"
                        ? "abaixo"
                        : m.position === "HIGH"
                          ? "acima"
                          : m.quartile
                            ? `Q${m.quartile}${m.targetQuartile ? ` (meta Q${m.targetQuartile})` : ""}`
                            : "—"}
                    </td>
                    <td className="py-1">{m.score}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {assessment.rpsSummary.unknownMarkers.length > 0 && (
            <p className="text-xs text-muted-foreground">
              Sem faixa cadastrada: {assessment.rpsSummary.unknownMarkers.join(", ")}.
            </p>
          )}
        </section>
      )}
    </div>
  );
}
