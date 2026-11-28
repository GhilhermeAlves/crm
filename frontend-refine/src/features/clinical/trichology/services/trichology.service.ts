import api from "@/lib/api";
import type {
  AssessmentDetail,
  AssessmentImageView,
  AssessmentSummary,
  CopilotView,
  ExamRule,
  Finding,
  ImageQuality,
  LabResult,
  Measures,
  PublicQuizView,
  PullTest,
  QuizAnswers,
  QuizDefinition,
  ReportSection,
  SkippedRegion,
  TrichologySettings,
  VerifyView,
  CaptureRegion,
} from "../types/trichology.types";

const base = (companyId: string, contactId: string) =>
  `/companies/${companyId}/patients/${contactId}/trichology`;
const exam = (companyId: string, contactId: string, id: string) =>
  `${base(companyId, contactId)}/assessments/${id}`;
const settingsBase = (companyId: string) => `/companies/${companyId}/trichology/settings`;

export const TrichologyService = {
  list: async (companyId: string, contactId: string) =>
    (await api.get<AssessmentSummary[]>(`${base(companyId, contactId)}/assessments`)).data,

  get: async (companyId: string, contactId: string, id: string) =>
    (await api.get<AssessmentDetail>(exam(companyId, contactId, id))).data,

  open: async (
    companyId: string,
    contactId: string,
    body: { protocolId?: string; consentGiven: boolean; chiefComplaint?: string },
  ) => (await api.post<AssessmentDetail>(`${base(companyId, contactId)}/assessments`, body)).data,

  cancel: async (companyId: string, contactId: string, id: string, reason?: string) =>
    (await api.post<AssessmentDetail>(`${exam(companyId, contactId, id)}/cancel`, { reason })).data,

  quizDefinition: async (companyId: string, contactId: string) =>
    (await api.get<QuizDefinition>(`${base(companyId, contactId)}/quiz-definition`)).data,

  quizLink: async (companyId: string, contactId: string, id: string) =>
    (
      await api.post<{ url: string; expiresAt: string }>(
        `${exam(companyId, contactId, id)}/quiz-link`,
      )
    ).data,

  saveQuiz: async (
    companyId: string,
    contactId: string,
    id: string,
    answers: QuizAnswers,
    complete: boolean,
  ) =>
    (
      await api.put<AssessmentDetail>(`${exam(companyId, contactId, id)}/quiz`, {
        answers,
        complete,
      })
    ).data,

  capture: async (
    companyId: string,
    contactId: string,
    id: string,
    input: { file: Blob; regionCode: string; magnification?: string; qualityStatus: ImageQuality },
  ) => {
    const form = new FormData();
    form.append("file", input.file, "tricoscopia.jpg");
    form.append("regionCode", input.regionCode);
    if (input.magnification) form.append("magnification", input.magnification);
    form.append("qualityStatus", input.qualityStatus);
    return (
      await api.post<AssessmentImageView>(`${exam(companyId, contactId, id)}/images`, form, {
        headers: { "Content-Type": "multipart/form-data" },
      })
    ).data;
  },

  linkImage: async (
    companyId: string,
    contactId: string,
    id: string,
    body: { imageId: string; regionCode: string; magnification?: string; qualityStatus?: string },
  ) =>
    (await api.post<AssessmentImageView>(`${exam(companyId, contactId, id)}/images/link`, body))
      .data,

  reanalyze: async (companyId: string, contactId: string, id: string, imageId: string) => {
    await api.post(`${exam(companyId, contactId, id)}/images/${imageId}/reanalyze`);
  },

  reviewImage: async (
    companyId: string,
    contactId: string,
    id: string,
    imageId: string,
    body: { acceptedFindings: Finding[]; confirmedMeasures: Measures; includeInReport: boolean },
  ) =>
    (
      await api.put<AssessmentImageView>(
        `${exam(companyId, contactId, id)}/images/${imageId}/review`,
        body,
      )
    ).data,

  imagingDone: async (
    companyId: string,
    contactId: string,
    id: string,
    skippedRegions: SkippedRegion[],
  ) =>
    (
      await api.put<AssessmentDetail>(`${exam(companyId, contactId, id)}/imaging-done`, {
        skippedRegions,
      })
    ).data,

  clinical: async (
    companyId: string,
    contactId: string,
    id: string,
    body: { pullTest: PullTest | null; notes: string },
  ) => (await api.put<AssessmentDetail>(`${exam(companyId, contactId, id)}/clinical`, body)).data,

  copilot: async (companyId: string, contactId: string, id: string) =>
    (await api.get<CopilotView>(`${exam(companyId, contactId, id)}/copilot`)).data,

  exams: async (companyId: string, contactId: string, id: string, markerCodes: string[]) =>
    (
      await api.put<AssessmentDetail>(`${exam(companyId, contactId, id)}/exams`, {
        markerCodes,
      })
    ).data,

  labs: async (
    companyId: string,
    contactId: string,
    id: string,
    body: { results: LabResult[]; collectedOn: string | null },
  ) => (await api.put<AssessmentDetail>(`${exam(companyId, contactId, id)}/labs`, body)).data,

  draft: async (companyId: string, contactId: string, id: string) =>
    (await api.post<AssessmentDetail>(`${exam(companyId, contactId, id)}/report/draft`)).data,

  updateReport: async (
    companyId: string,
    contactId: string,
    id: string,
    sections: ReportSection[],
  ) =>
    (await api.put<AssessmentDetail>(`${exam(companyId, contactId, id)}/report`, { sections }))
      .data,

  approve: async (
    companyId: string,
    contactId: string,
    id: string,
    body: { professionalName: string; professionalRegistry: string },
  ) =>
    (await api.post<AssessmentDetail>(`${exam(companyId, contactId, id)}/report/approve`, body))
      .data,

  newVersion: async (companyId: string, contactId: string, id: string) =>
    (await api.post<AssessmentDetail>(`${exam(companyId, contactId, id)}/report/new-version`)).data,

  pdf: async (companyId: string, contactId: string, id: string) =>
    (
      await api.get<Blob>(`${exam(companyId, contactId, id)}/report/pdf`, {
        responseType: "blob",
      })
    ).data,

  settings: async (companyId: string) =>
    (await api.get<TrichologySettings>(settingsBase(companyId))).data,

  updateSettings: async (
    companyId: string,
    body: { minConfidence: number; reportHorizonsMonths: number[]; examRules: ExamRule[] },
  ) => (await api.put<TrichologySettings>(settingsBase(companyId), body)).data,

  saveProtocol: async (
    companyId: string,
    id: string | null,
    body: { name: string; regions: CaptureRegion[]; isDefault: boolean; active: boolean },
  ) =>
    (id
      ? await api.put<TrichologySettings>(`${settingsBase(companyId)}/protocols/${id}`, body)
      : await api.post<TrichologySettings>(`${settingsBase(companyId)}/protocols`, body)
    ).data,

  saveFinding: async (
    companyId: string,
    id: string | null,
    body: { code: string; label: string; description: string | null; active: boolean },
  ) =>
    (id
      ? await api.put<TrichologySettings>(`${settingsBase(companyId)}/findings/${id}`, body)
      : await api.post<TrichologySettings>(`${settingsBase(companyId)}/findings`, body)
    ).data,

  saveMarker: async (companyId: string, id: string | null, body: Record<string, unknown>) =>
    (id
      ? await api.put<TrichologySettings>(`${settingsBase(companyId)}/markers/${id}`, body)
      : await api.post<TrichologySettings>(`${settingsBase(companyId)}/markers`, body)
    ).data,
};

/** Rotas públicas (sem sessão): o status HTTP decide a tela. */
export type PublicResult<T> =
  { ok: true; data: T } | { ok: false; status: number; message?: string };

const PUBLIC_BASE = "/api/v1/public/trichology";

async function toResult<T>(response: Response, parse: boolean): Promise<PublicResult<T>> {
  if (response.ok) {
    return { ok: true, data: (parse ? await response.json() : undefined) as T };
  }
  let message: string | undefined;
  try {
    message = ((await response.json()) as { message?: string }).message;
  } catch {
    message = undefined;
  }
  return { ok: false, status: response.status, message };
}

export const PublicTrichologyService = {
  quiz: async (token: string) =>
    toResult<PublicQuizView>(
      await fetch(`${PUBLIC_BASE}/quiz/${encodeURIComponent(token)}`, { cache: "no-store" }),
      true,
    ),

  submitQuiz: async (token: string, answers: QuizAnswers, website: string) =>
    toResult<void>(
      await fetch(`${PUBLIC_BASE}/quiz/${encodeURIComponent(token)}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ answers, website }),
      }),
      false,
    ),

  verify: async (code: string) =>
    toResult<VerifyView>(
      await fetch(`${PUBLIC_BASE}/verify/${encodeURIComponent(code)}`, { cache: "no-store" }),
      true,
    ),
};
