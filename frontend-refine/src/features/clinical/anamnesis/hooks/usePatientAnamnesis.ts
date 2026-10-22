import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { patientAnamnesisService } from "../services/patient-anamnesis.service";
import type {
  PatientAnamnesisResponse,
  SavePatientAnamnesisRequest,
  StartPatientAnamnesisRequest,
} from "../types/patient-anamnesis.types";

export const patientAnamnesisKeys = {
  all: (companyId: string, contactId: string) =>
    ["patient-anamneses", companyId, contactId] as const,
  current: (companyId: string, contactId: string) =>
    ["patient-anamnesis-current", companyId, contactId] as const,
  detail: (companyId: string, contactId: string, anamnesisId: string) =>
    ["patient-anamnesis", companyId, contactId, anamnesisId] as const,
};

export function usePatientAnamneses(companyId: string, contactId: string) {
  return useQuery({
    queryKey: patientAnamnesisKeys.all(companyId, contactId),
    queryFn: () => patientAnamnesisService.list(companyId, contactId),
    enabled: Boolean(companyId && contactId),
  });
}

export function useCurrentPatientAnamnesis(companyId: string, contactId: string) {
  return useQuery({
    queryKey: patientAnamnesisKeys.current(companyId, contactId),
    queryFn: () => patientAnamnesisService.getCurrent(companyId, contactId),
    enabled: Boolean(companyId && contactId),
  });
}

export function usePatientAnamnesis(
  companyId: string,
  contactId: string,
  anamnesisId: string,
) {
  return useQuery({
    queryKey: patientAnamnesisKeys.detail(companyId, contactId, anamnesisId),
    queryFn: () => patientAnamnesisService.get(companyId, contactId, anamnesisId),
    enabled: Boolean(companyId && contactId && anamnesisId),
  });
}

export function useStartPatientAnamnesis(companyId: string, contactId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: StartPatientAnamnesisRequest) =>
      patientAnamnesisService.start(companyId, contactId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: patientAnamnesisKeys.all(companyId, contactId) });
      queryClient.invalidateQueries({ queryKey: patientAnamnesisKeys.current(companyId, contactId) });
      toast.success("Anamnese iniciada");
    },
    onError: (error: unknown) => {
      const message = (error as { response?: { data?: { message?: string } } })?.response?.data
        ?.message;
      toast.error(message || "Não foi possível iniciar a anamnese");
    },
  });
}

export function useSavePatientAnamnesis(companyId: string, contactId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      anamnesisId,
      payload,
    }: {
      anamnesisId: string;
      payload: SavePatientAnamnesisRequest;
    }) => patientAnamnesisService.save(companyId, contactId, anamnesisId, payload),
    onSuccess: (data: PatientAnamnesisResponse) => {
      queryClient.invalidateQueries({ queryKey: patientAnamnesisKeys.all(companyId, contactId) });
      queryClient.invalidateQueries({ queryKey: patientAnamnesisKeys.current(companyId, contactId) });
      queryClient.invalidateQueries({ queryKey: patientAnamnesisKeys.detail(companyId, contactId, data.id) });
      toast.success(data.status === "COMPLETED" ? "Anamnese concluída" : "Anamnese salva");
    },
    onError: (error: unknown) => {
      const message = (error as { response?: { data?: { message?: string } } })?.response?.data
        ?.message;
      toast.error(message || "Não foi possível salvar a anamnese");
    },
  });
}

export function useDiscardPatientAnamnesis(companyId: string, contactId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (anamnesisId: string) =>
      patientAnamnesisService.discard(companyId, contactId, anamnesisId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: patientAnamnesisKeys.all(companyId, contactId) });
      queryClient.invalidateQueries({ queryKey: patientAnamnesisKeys.current(companyId, contactId) });
      toast.success("Anamnese descartada");
    },
    onError: (error: unknown) => {
      const message = (error as { response?: { data?: { message?: string } } })?.response?.data
        ?.message;
      toast.error(message || "Não foi possível descartar a anamnese");
    },
  });
}
