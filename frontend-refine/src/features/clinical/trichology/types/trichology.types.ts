/** Tipos do módulo Tricologia (espelham TrichologyDtos do backend). */

export type AssessmentStatus =
  | "DRAFT"
  | "QUIZ_COMPLETED"
  | "IMAGING_IN_PROGRESS"
  | "IMAGING_DONE"
  | "CLINICAL_DONE"
  | "EXAMS_REQUESTED"
  | "LABS_LOADED"
  | "AI_DRAFT_READY"
  | "GUARDRAIL_BLOCKED"
  | "REVIEWED"
  | "PUBLISHED"
  | "CANCELLED";

export type ImageQuality = "UNCHECKED" | "OK" | "BLURRED" | "DARK" | "GLARE";
export type AiStatus = "PENDING" | "RUNNING" | "DONE" | "FAILED" | "SKIPPED";
export type Severity = "MILD" | "MODERATE" | "SEVERE";

export type CaptureRegion = {
  code: string;
  label: string;
  magnifications: string[];
  minImages: number;
};

export type CaptureProtocol = {
  id: string;
  companyId: string;
  name: string;
  regions: CaptureRegion[];
  isDefault: boolean;
  active: boolean;
};

export type RegionProgress = {
  code: string;
  label: string;
  minImages: number;
  images: number;
  skipped: boolean;
  skipReason: string | null;
};

export type Finding = {
  code: string;
  label: string | null;
  severity: Severity | null;
  confidence: number | null;
  evidence: string | null;
  source: "AI" | "PROFESSIONAL";
};

export type FindingView = Finding & { possible: boolean };

export type Measures = Partial<
  Record<
    | "hairDensityPerCm2"
    | "thinHairPercent"
    | "singleHairFollicularUnitsPercent"
    | "meanShaftDiameterMicrons",
    number
  >
>;

export type AssessmentImageView = {
  id: string;
  imageId: string;
  regionCode: string;
  regionLabel: string;
  magnification: string | null;
  qualityStatus: ImageQuality;
  aiStatus: AiStatus;
  aiImageQuality: ImageQuality | null;
  aiFindings: FindingView[];
  aiEstimates: Measures;
  aiNotes: string | null;
  acceptedFindings: Finding[];
  confirmedMeasures: Measures;
  includeInReport: boolean;
  reviewedAt: string | null;
  takenAt: string | null;
  downloadUrl: string | null;
};

export type PullTestSite = { regionCode: string; hairsPulled: number };
export type PullTest = { sites: PullTestSite[]; notes: string | null };
export type SkippedRegion = { regionCode: string; reason: string };
export type LabResult = { markerCode: string; value: number; unit: string | null };

export type MarkerScore = {
  markerCode: string;
  label: string;
  system: string;
  value: number;
  unit: string | null;
  referenceMin: number | null;
  referenceMax: number | null;
  quartile: number | null;
  targetQuartile: number | null;
  position: "LOW" | "HIGH" | "IN_RANGE" | "NO_RANGE";
  score: number;
};

export type RpsSummary = {
  overallScore: number;
  systems: { system: string; score: number; markers: number }[];
  markers: MarkerScore[];
  unknownMarkers: string[];
};

export type ReportSection = {
  key: string;
  title: string;
  content: string;
  origin: "CODE" | "AI" | "EDITED";
};

export type ReportPayload = {
  sections: ReportSection[];
  facts: Record<string, unknown>;
  guardrailViolations: string[];
};

export type QuizAnswers = Record<string, string | string[] | boolean | number>;

export type AssessmentSummary = {
  id: string;
  status: AssessmentStatus;
  reportVersion: number;
  trichoscopyPerformed: boolean;
  supersedesId: string | null;
  professionalName: string | null;
  createdAt: string;
  publishedAt: string | null;
  cancelledAt: string | null;
};

export type AssessmentDetail = {
  id: string;
  contactId: string;
  status: AssessmentStatus;
  reportVersion: number;
  supersedesId: string | null;
  protocol: CaptureProtocol | null;
  regions: RegionProgress[];
  consentAt: string | null;
  chiefComplaint: string | null;
  quizAnswers: QuizAnswers | null;
  quizCompletedAt: string | null;
  quizLinkExpiresAt: string | null;
  anamnesisId: string | null;
  anamnesisCompletedAt: string | null;
  clinicalNotes: string | null;
  pullTest: PullTest | null;
  pullTestPositive: boolean;
  trichoscopyPerformed: boolean;
  images: AssessmentImageView[];
  requestedExams: string[] | null;
  labResults: LabResult[] | null;
  labCollectedOn: string | null;
  rpsSummary: RpsSummary | null;
  report: ReportPayload | null;
  verificationCode: string | null;
  professionalName: string | null;
  professionalRegistry: string | null;
  reviewedAt: string | null;
  publishedAt: string | null;
  cancelledAt: string | null;
  cancelReason: string | null;
  createdAt: string;
  updatedAt: string;
};

export type QuizQuestion = {
  id: string;
  text: string;
  type: "SINGLE" | "MULTI" | "TEXT" | "NUMBER" | "BOOLEAN";
  required: boolean;
  options: { value: string; label: string }[];
  showIf: { dependsOn: string; values: string[] } | null;
};

export type QuizDefinition = { version: string; questions: QuizQuestion[] };

export type Hypothesis = { code: string; label: string; support: string[] };
export type SuggestedExam = { markerCode: string; label: string; reason: string };
export type MeasureDelta = {
  regionCode: string;
  measure: string;
  previous: number;
  current: number;
};

export type CopilotView = {
  pending: string[];
  hypotheses: Hypothesis[];
  suggestedExams: SuggestedExam[];
  evolution: {
    previousAssessmentId: string;
    previousPublishedAt: string;
    deltas: MeasureDelta[];
    previousRps: number | null;
    currentRps: number | null;
  } | null;
  disclaimer: string;
};

export type FindingCatalogItem = {
  id: string;
  companyId: string;
  code: string;
  label: string;
  description: string | null;
  active: boolean;
};

export type Marker = {
  id: string;
  companyId: string;
  code: string;
  label: string;
  unit: string | null;
  system: string;
  sex: "F" | "M" | null;
  referenceMin: number | null;
  referenceMax: number | null;
  targetQuartile: number | null;
  active: boolean;
};

export type ExamRule = { hypothesis: string; markerCodes: string[] };

export type TrichologySettings = {
  minConfidence: number;
  reportHorizonsMonths: number[];
  examRules: ExamRule[];
  protocols: CaptureProtocol[];
  findings: FindingCatalogItem[];
  markers: Marker[];
};

export type VerifyView = {
  valid: boolean;
  publishedAt: string | null;
  professionalName: string | null;
  professionalRegistry: string | null;
  clinicName: string | null;
  reportVersion: number | null;
  pdfSha256: string | null;
  superseded: boolean;
};

export type PublicQuizView = {
  clinicName: string;
  patientFirstName: string | null;
  quiz: QuizDefinition;
};

export const STATUS_LABELS: Record<AssessmentStatus, string> = {
  DRAFT: "Aberto",
  QUIZ_COMPLETED: "Quiz respondido",
  IMAGING_IN_PROGRESS: "Captura em andamento",
  IMAGING_DONE: "Captura concluída",
  CLINICAL_DONE: "Exame clínico feito",
  EXAMS_REQUESTED: "Exames pedidos",
  LABS_LOADED: "Laboratório lançado",
  AI_DRAFT_READY: "Relatório em rascunho",
  GUARDRAIL_BLOCKED: "Relatório com pendências",
  REVIEWED: "Relatório revisado",
  PUBLISHED: "Publicado",
  CANCELLED: "Cancelado",
};

export const SEVERITY_LABELS: Record<Severity, string> = {
  MILD: "Leve",
  MODERATE: "Moderada",
  SEVERE: "Intensa",
};

export const QUALITY_LABELS: Record<ImageQuality, string> = {
  UNCHECKED: "Não conferida",
  OK: "Boa",
  BLURRED: "Desfocada",
  DARK: "Escura",
  GLARE: "Com reflexo",
};

export const MEASURE_LABELS: Record<keyof Measures, string> = {
  hairDensityPerCm2: "Densidade (fios/cm²)",
  thinHairPercent: "Fios finos (%)",
  singleHairFollicularUnitsPercent: "UF com fio único (%)",
  meanShaftDiameterMicrons: "Diâmetro médio (µm)",
};
