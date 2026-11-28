"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import { useMutationDefaults } from "@/lib/query/mutation-utils";
import { patientDocumentKeys } from "@/features/clinical/documents/hooks/usePatientDocuments";
import { trichoscopyKeys } from "@/features/clinical/trichoscopy/hooks/useTrichoscopy";
import { TrichologyService } from "../services/trichology.service";
import type {
  AssessmentDetail,
  ExamRule,
  Finding,
  ImageQuality,
  LabResult,
  Measures,
  PullTest,
  QuizAnswers,
  ReportSection,
  SkippedRegion,
  CaptureRegion,
} from "../types/trichology.types";

export const trichologyKeys = {
  list: (companyId: string, contactId: string) => ["trichology", companyId, contactId] as const,
  detail: (companyId: string, contactId: string, id: string) =>
    ["trichology", companyId, contactId, id] as const,
  copilot: (companyId: string, contactId: string, id: string) =>
    ["trichology", companyId, contactId, id, "copilot"] as const,
  quiz: (companyId: string) => ["trichology-quiz", companyId] as const,
  settings: (companyId: string) => ["trichology-settings", companyId] as const,
};

/** Tudo que um exame afeta na ficha do paciente (exame, lista, copiloto, galeria, notas, documentos, arquivos). */
function affected(companyId: string, contactId: string, id?: string): unknown[][] {
  const keys: unknown[][] = [
    [...trichologyKeys.list(companyId, contactId)],
    [...trichoscopyKeys.all(companyId, contactId)],
    ["contact-notes", companyId, contactId],
  ];
  if (id) keys.push([...trichologyKeys.detail(companyId, contactId, id)]);
  return keys;
}

export function useAssessments(companyId: string, contactId: string) {
  return useQuery({
    queryKey: trichologyKeys.list(companyId, contactId),
    queryFn: () => TrichologyService.list(companyId, contactId),
    enabled: Boolean(companyId && contactId),
  });
}

/** Enquanto alguma foto estiver em análise, atualiza o exame a cada 4 s. */
export function useAssessment(companyId: string, contactId: string, id: string | null) {
  return useQuery({
    queryKey: trichologyKeys.detail(companyId, contactId, id ?? "none"),
    queryFn: () => TrichologyService.get(companyId, contactId, id as string),
    enabled: Boolean(companyId && contactId && id),
    refetchInterval: (query) => {
      const data = query.state.data as AssessmentDetail | undefined;
      return data?.images.some((i) => i.aiStatus === "PENDING" || i.aiStatus === "RUNNING")
        ? 4000
        : false;
    },
  });
}

export function useCopilot(companyId: string, contactId: string, id: string, enabled: boolean) {
  return useQuery({
    queryKey: trichologyKeys.copilot(companyId, contactId, id),
    queryFn: () => TrichologyService.copilot(companyId, contactId, id),
    enabled: Boolean(companyId && contactId && id && enabled),
  });
}

export function useQuizDefinition(companyId: string, contactId: string) {
  return useQuery({
    queryKey: trichologyKeys.quiz(companyId),
    queryFn: () => TrichologyService.quizDefinition(companyId, contactId),
    enabled: Boolean(companyId && contactId),
    staleTime: Infinity,
  });
}

export function useTrichologySettings(companyId: string) {
  return useQuery({
    queryKey: trichologyKeys.settings(companyId),
    queryFn: () => TrichologyService.settings(companyId),
    enabled: Boolean(companyId),
  });
}

/**
 * Mutations do exame. Cada uma invalida o exame, a lista, o copiloto e o que
 * mais muda na ficha (galeria, notas; documentos e arquivos na publicação).
 */
export function useAssessmentMutations(companyId: string, contactId: string, id: string) {
  const { onSuccess, onError, queryClient } = useMutationDefaults();
  const keys = affected(companyId, contactId, id);
  const refreshCopilot = () =>
    queryClient.invalidateQueries({ queryKey: trichologyKeys.copilot(companyId, contactId, id) });
  const ok = (successMessage?: string, extra: unknown[][] = []) =>
    onSuccess({
      successMessage,
      invalidateKeys: [...keys, ...extra],
      extraInvalidate: () => void refreshCopilot(),
    });

  return {
    cancel: useMutation({
      mutationFn: (reason?: string) => TrichologyService.cancel(companyId, contactId, id, reason),
      onSuccess: ok("Exame cancelado"),
      onError: onError({ errorMessage: "Erro ao cancelar o exame" }),
    }),
    quizLink: useMutation({
      mutationFn: () => TrichologyService.quizLink(companyId, contactId, id),
      onSuccess: ok(),
      onError: onError({ errorMessage: "Erro ao gerar o link do quiz" }),
    }),
    saveQuiz: useMutation({
      mutationFn: ({ answers, complete }: { answers: QuizAnswers; complete: boolean }) =>
        TrichologyService.saveQuiz(companyId, contactId, id, answers, complete),
      onSuccess: ok("Quiz salvo"),
      onError: onError({ errorMessage: "Erro ao salvar o quiz" }),
    }),
    capture: useMutation({
      mutationFn: (input: {
        file: Blob;
        regionCode: string;
        magnification?: string;
        qualityStatus: ImageQuality;
      }) => TrichologyService.capture(companyId, contactId, id, input),
      onSuccess: ok("Foto salva no exame", [["storage", companyId]]),
      onError: onError({ errorMessage: "Erro ao salvar a foto" }),
    }),
    reanalyze: useMutation({
      mutationFn: (imageId: string) =>
        TrichologyService.reanalyze(companyId, contactId, id, imageId),
      onSuccess: ok("Nova análise solicitada"),
      onError: onError({ errorMessage: "Erro ao reprocessar a foto" }),
    }),
    reviewImage: useMutation({
      mutationFn: ({
        imageId,
        ...body
      }: {
        imageId: string;
        acceptedFindings: Finding[];
        confirmedMeasures: Measures;
        includeInReport: boolean;
      }) => TrichologyService.reviewImage(companyId, contactId, id, imageId, body),
      onSuccess: ok("Revisão salva"),
      onError: onError({ errorMessage: "Erro ao salvar a revisão" }),
    }),
    imagingDone: useMutation({
      mutationFn: (skipped: SkippedRegion[]) =>
        TrichologyService.imagingDone(companyId, contactId, id, skipped),
      onSuccess: ok("Captura concluída"),
      onError: onError({ errorMessage: "Erro ao concluir a captura" }),
    }),
    clinical: useMutation({
      mutationFn: (body: { pullTest: PullTest | null; notes: string }) =>
        TrichologyService.clinical(companyId, contactId, id, body),
      onSuccess: ok("Exame clínico salvo"),
      onError: onError({ errorMessage: "Erro ao salvar o exame clínico" }),
    }),
    exams: useMutation({
      mutationFn: (codes: string[]) => TrichologyService.exams(companyId, contactId, id, codes),
      onSuccess: ok("Exames pedidos"),
      onError: onError({ errorMessage: "Erro ao pedir os exames" }),
    }),
    labs: useMutation({
      mutationFn: (body: { results: LabResult[]; collectedOn: string | null }) =>
        TrichologyService.labs(companyId, contactId, id, body),
      onSuccess: ok("Resultados lançados"),
      onError: onError({ errorMessage: "Erro ao lançar os resultados" }),
    }),
    draft: useMutation({
      mutationFn: () => TrichologyService.draft(companyId, contactId, id),
      onSuccess: ok("Rascunho do relatório gerado"),
      onError: onError({ errorMessage: "Erro ao gerar o relatório" }),
    }),
    updateReport: useMutation({
      mutationFn: (sections: ReportSection[]) =>
        TrichologyService.updateReport(companyId, contactId, id, sections),
      onSuccess: ok("Relatório salvo"),
      onError: onError({ errorMessage: "Erro ao salvar o relatório" }),
    }),
    approve: useMutation({
      mutationFn: (body: { professionalName: string; professionalRegistry: string }) =>
        TrichologyService.approve(companyId, contactId, id, body),
      onSuccess: ok("Relatório publicado", [
        [...patientDocumentKeys.all(companyId, contactId)],
        ["storage", companyId],
      ]),
      onError: onError({ errorMessage: "Erro ao publicar o relatório" }),
    }),
    newVersion: useMutation({
      mutationFn: () => TrichologyService.newVersion(companyId, contactId, id),
      onSuccess: ok("Nova versão criada"),
      onError: onError({ errorMessage: "Erro ao criar a nova versão" }),
    }),
  };
}

export function useOpenAssessment(companyId: string, contactId: string) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (body: { protocolId?: string; consentGiven: boolean; chiefComplaint?: string }) =>
      TrichologyService.open(companyId, contactId, body),
    onSuccess: onSuccess({
      successMessage: "Exame aberto",
      invalidateKeys: affected(companyId, contactId),
    }),
    onError: onError({ errorMessage: "Erro ao abrir o exame" }),
  });
}

export function useSettingsMutations(companyId: string) {
  const { onSuccess, onError } = useMutationDefaults();
  const invalidateKeys = [[...trichologyKeys.settings(companyId)]];
  return {
    update: useMutation({
      mutationFn: (body: {
        minConfidence: number;
        reportHorizonsMonths: number[];
        examRules: ExamRule[];
      }) => TrichologyService.updateSettings(companyId, body),
      onSuccess: onSuccess({ successMessage: "Configurações salvas", invalidateKeys }),
      onError: onError({ errorMessage: "Erro ao salvar as configurações" }),
    }),
    protocol: useMutation({
      mutationFn: ({
        id,
        ...body
      }: {
        id: string | null;
        name: string;
        regions: CaptureRegion[];
        isDefault: boolean;
        active: boolean;
      }) => TrichologyService.saveProtocol(companyId, id, body),
      onSuccess: onSuccess({ successMessage: "Protocolo salvo", invalidateKeys }),
      onError: onError({ errorMessage: "Erro ao salvar o protocolo" }),
    }),
    finding: useMutation({
      mutationFn: ({
        id,
        ...body
      }: {
        id: string | null;
        code: string;
        label: string;
        description: string | null;
        active: boolean;
      }) => TrichologyService.saveFinding(companyId, id, body),
      onSuccess: onSuccess({ successMessage: "Achado salvo", invalidateKeys }),
      onError: onError({ errorMessage: "Erro ao salvar o achado" }),
    }),
    marker: useMutation({
      mutationFn: ({ id, ...body }: { id: string | null } & Record<string, unknown>) =>
        TrichologyService.saveMarker(companyId, id, body),
      onSuccess: onSuccess({ successMessage: "Marcador salvo", invalidateKeys }),
      onError: onError({ errorMessage: "Erro ao salvar o marcador" }),
    }),
  };
}
