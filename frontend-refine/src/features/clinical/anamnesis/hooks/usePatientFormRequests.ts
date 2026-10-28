import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import api from "@/lib/api";
import { patientAnamnesisKeys } from "./usePatientAnamnesis";

export type FormRequestMode = "LINK" | "RECEPTION";
export type FormRequestStatus = "PENDING" | "ANSWERED" | "EXPIRED";

export type FormRequest = {
  id: string;
  anamnesisId: string;
  modelName: string | null;
  mode: FormRequestMode;
  status: FormRequestStatus;
  createdAt: string;
  expiresAt: string;
  answeredAt: string | null;
  signatureUrl: string | null;
};

export type CreatedFormRequest = {
  id: string;
  anamnesisId: string;
  url: string;
  mode: FormRequestMode;
  expiresAt: string;
  /** Link enviado automaticamente pelo WhatsApp conectado da clínica. */
  whatsAppSent: boolean;
  /** Link enviado por e-mail (com o nome da clínica como remetente). */
  emailSent: boolean;
};

const base = (companyId: string, contactId: string) =>
  `/companies/${companyId}/patients/${contactId}/form-requests`;

export const formRequestKeys = {
  all: (companyId: string, contactId: string) =>
    ["patient-form-requests", companyId, contactId] as const,
};

/** Pedidos de formulário do paciente (enviados por link ou abertos na recepção). */
export function usePatientFormRequests(companyId: string, contactId: string) {
  return useQuery({
    queryKey: formRequestKeys.all(companyId, contactId),
    queryFn: async () => (await api.get<FormRequest[]>(base(companyId, contactId))).data,
    enabled: Boolean(companyId && contactId),
    // A resposta chega do paciente, fora desta tela: sempre busca ao abrir.
    staleTime: 0,
    refetchOnMount: "always",
  });
}

/** WhatsApp da clínica conectado (Evolution)? Sem permissão ou erro, trata como não. */
export function useWhatsAppConnected() {
  const { data } = useQuery({
    queryKey: ["whatsapp-connection"],
    queryFn: async () =>
      (await api.get<{ state: string }>("/omnichannel/whatsapp/connection")).data,
    retry: false,
    staleTime: 60_000,
  });
  return data?.state === "CONNECTED";
}

/** O servidor envia e-mail de verdade (SMTP)? Em desenvolvimento ele só registra no log. */
export function useFormEmailEnabled(companyId: string) {
  const { data } = useQuery({
    queryKey: ["form-request-options", companyId],
    queryFn: async () =>
      (await api.get<{ emailEnabled: boolean }>(`/companies/${companyId}/form-requests/options`))
        .data,
    enabled: Boolean(companyId),
    staleTime: 5 * 60_000,
  });
  return data?.emailEnabled ?? false;
}

export function useCreateFormRequest(companyId: string, contactId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: {
      modelId: string;
      mode: FormRequestMode;
      sendWhatsApp?: boolean;
      sendEmail?: boolean;
    }) => (await api.post<CreatedFormRequest>(base(companyId, contactId), payload)).data,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: formRequestKeys.all(companyId, contactId) });
      void queryClient.invalidateQueries({
        queryKey: patientAnamnesisKeys.all(companyId, contactId),
      });
    },
    onError: (error: unknown) => {
      const message = (error as { response?: { data?: { message?: string } } })?.response?.data
        ?.message;
      toast.error(message || "Não foi possível gerar o formulário");
    },
  });
}
