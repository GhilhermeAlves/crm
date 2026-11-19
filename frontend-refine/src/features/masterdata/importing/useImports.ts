import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { importService } from "./import.service";

const PATIENT_KEYS = ["patients", "contacts", "patient", "contacts-search"];

export function useImports(companyId: string | undefined) {
  return useQuery({
    queryKey: ["data-imports", companyId],
    queryFn: () => importService.list(companyId!),
    enabled: !!companyId,
  });
}

export function usePreviewImport(companyId: string | undefined) {
  return useMutation({ mutationFn: (file: File) => importService.preview(companyId!, file) });
}

function useInvalidatePatients() {
  const qc = useQueryClient();
  return () => {
    qc.invalidateQueries({ queryKey: ["data-imports"] });
    PATIENT_KEYS.forEach((k) => qc.invalidateQueries({ queryKey: [k] }));
  };
}

export function useImportPatients(companyId: string | undefined) {
  const invalidate = useInvalidatePatients();
  return useMutation({
    mutationFn: (file: File) => importService.importPatients(companyId!, file),
    onSuccess: invalidate,
  });
}

export function useUndoImport(companyId: string | undefined) {
  const invalidate = useInvalidatePatients();
  return useMutation({
    mutationFn: (importId: string) => importService.undo(companyId!, importId),
    onSuccess: invalidate,
  });
}
