import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { documentTemplateService } from "../services/document-template.service";
import { patientDocumentService } from "../services/patient-document.service";
import type { IssuePatientDocumentRequest } from "../types/document.types";

export const documentTemplateKeys = {
  all: (companyId: string) => ["document-templates", companyId] as const,
};

export const patientDocumentKeys = {
  all: (companyId: string, contactId: string) =>
    ["patient-documents", companyId, contactId] as const,
};

export function useDocumentTemplates(companyId: string, activeOnly = true) {
  return useQuery({
    queryKey: documentTemplateKeys.all(companyId),
    queryFn: () => documentTemplateService.list(companyId, activeOnly),
    enabled: Boolean(companyId),
  });
}

export function usePatientDocuments(companyId: string, contactId: string) {
  return useQuery({
    queryKey: patientDocumentKeys.all(companyId, contactId),
    queryFn: () => patientDocumentService.list(companyId, contactId),
    enabled: Boolean(companyId && contactId),
  });
}

export function usePreviewPatientDocument(companyId: string, contactId: string) {
  return useMutation({
    mutationFn: (templateId: string) =>
      patientDocumentService.preview(companyId, contactId, { templateId }),
    onError: (error: unknown) => {
      const message = (error as { response?: { data?: { message?: string } } })?.response?.data
        ?.message;
      toast.error(message || "Não foi possível visualizar o modelo");
    },
  });
}

export function useIssuePatientDocument(companyId: string, contactId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: IssuePatientDocumentRequest) =>
      patientDocumentService.issue(companyId, contactId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: patientDocumentKeys.all(companyId, contactId) });
      toast.success("Documento emitido");
    },
    onError: (error: unknown) => {
      const message = (error as { response?: { data?: { message?: string } } })?.response?.data
        ?.message;
      toast.error(message || "Não foi possível emitir o documento");
    },
  });
}

export function useDeletePatientDocument(companyId: string, contactId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (documentId: string) =>
      patientDocumentService.delete(companyId, contactId, documentId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: patientDocumentKeys.all(companyId, contactId) });
      toast.success("Documento excluído");
    },
    onError: (error: unknown) => {
      const message = (error as { response?: { data?: { message?: string } } })?.response?.data
        ?.message;
      toast.error(message || "Não foi possível excluir o documento");
    },
  });
}

export function useOpenPatientDocumentPdf(companyId: string, contactId: string) {
  return useMutation({
    mutationFn: (documentId: string) =>
      patientDocumentService.openPdf(companyId, contactId, documentId),
    onError: (error: unknown) => {
      const message = (error as { response?: { data?: { message?: string } } })?.response?.data
        ?.message;
      toast.error(message || "Não foi possível abrir o PDF");
    },
  });
}
