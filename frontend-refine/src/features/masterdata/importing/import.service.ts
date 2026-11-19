import api from "@/lib/api";
import type { DataImportSummary, PatientImportPreview, UndoResult } from "./types";

const base = (companyId: string) => `/companies/${companyId}/imports`;

function zipForm(file: File): FormData {
  const form = new FormData();
  form.append("file", file);
  return form;
}

// `Content-Type: null` deixa o browser montar o multipart com boundary.
const multipart = { headers: { "Content-Type": null } };

export const importService = {
  async preview(companyId: string, file: File): Promise<PatientImportPreview> {
    const r = await api.post<PatientImportPreview>(
      `${base(companyId)}/patients/preview`,
      zipForm(file),
      multipart,
    );
    return r.data;
  },
  async importPatients(companyId: string, file: File): Promise<PatientImportPreview> {
    const r = await api.post<PatientImportPreview>(
      `${base(companyId)}/patients`,
      zipForm(file),
      multipart,
    );
    return r.data;
  },
  async list(companyId: string): Promise<DataImportSummary[]> {
    const r = await api.get<DataImportSummary[]>(base(companyId));
    return r.data;
  },
  async undo(companyId: string, importId: string): Promise<UndoResult> {
    const r = await api.delete<UndoResult>(`${base(companyId)}/${importId}`);
    return r.data;
  },
};
