export type DocumentType = "PRESCRIPTION" | "CERTIFICATE" | "CONSENT" | "OTHER";

export type DocumentTemplateResponse = {
  id: string;
  type: DocumentType;
  title: string;
  body: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type PatientDocumentResponse = {
  id: string;
  contactId: string;
  templateId: string | null;
  type: DocumentType;
  title: string;
  body: string;
  professionalName: string;
  professionalRegistry: string | null;
  issuedBy: string | null;
  issuedByName: string | null;
  issuedAt: string;
};

export type PreviewPatientDocumentRequest = {
  templateId: string;
};

export type PreviewPatientDocumentResponse = {
  type: DocumentType;
  title: string;
  body: string;
};

export type IssuePatientDocumentRequest = {
  templateId?: string | null;
  type: DocumentType;
  title: string;
  body: string;
  professionalName: string;
  professionalRegistry?: string | null;
};

export const DOCUMENT_TYPE_LABELS: Record<DocumentType, string> = {
  PRESCRIPTION: "Receituário",
  CERTIFICATE: "Atestado",
  CONSENT: "Termo",
  OTHER: "Outro",
};
