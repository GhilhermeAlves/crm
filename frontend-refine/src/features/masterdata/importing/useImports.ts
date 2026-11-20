import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { importService } from "./import.service";
import type { ImportKind } from "./types";

// Tudo que uma importação (ou o desfazer) pode alterar.
const AFFECTED_KEYS = [
  "data-imports",
  "patients",
  "contacts",
  "patient",
  "contacts-search",
  "patient-procedures",
  "catalog",
];

export function useImports(companyId: string | undefined) {
  return useQuery({
    queryKey: ["data-imports", companyId],
    queryFn: () => importService.list(companyId!),
    enabled: !!companyId,
  });
}

export function usePreviewImport(companyId: string | undefined, kind: ImportKind) {
  return useMutation({ mutationFn: (file: File) => importService.preview(companyId!, kind, file) });
}

function useInvalidateAffected() {
  const qc = useQueryClient();
  return () => AFFECTED_KEYS.forEach((k) => qc.invalidateQueries({ queryKey: [k] }));
}

export function useImportData(companyId: string | undefined, kind: ImportKind) {
  const invalidate = useInvalidateAffected();
  return useMutation({
    mutationFn: (file: File) => importService.importData(companyId!, kind, file),
    onSuccess: invalidate,
  });
}

export function useUndoImport(companyId: string | undefined) {
  const invalidate = useInvalidateAffected();
  return useMutation({
    mutationFn: (importId: string) => importService.undo(companyId!, importId),
    onSuccess: invalidate,
  });
}
