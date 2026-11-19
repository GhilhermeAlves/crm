export type ImportRowStatus = "OK" | "DUPLICADO" | "ERRO" | "IGNORADO" | "IMPORTADO";

export interface ImportRowResult {
  line: number;
  name: string;
  status: ImportRowStatus;
  message: string | null;
  warnings: string[];
}

export interface PatientImportPreview {
  total: number;
  ok: number;
  duplicates: number;
  errors: number;
  ignored: number;
  unsupportedFiles: string[];
  rows: ImportRowResult[];
  importId: string | null;
}

export interface DataImportSummary {
  id: string;
  sourceFile: string | null;
  total: number;
  imported: number;
  skipped: number;
  failed: number;
  createdAt: string;
  undoneAt: string | null;
}

export interface UndoResult {
  removed: number;
  kept: number;
}
