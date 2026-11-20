import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useActiveCompanyModules } from "@/features/identity/modules/hooks/useCompanyModules";
import { toast } from "sonner";
import { toHttpError } from "@/lib/http-error";
import { PatientBillingService, type ChargeView } from "./service";

export const PATIENT_BILLING_KEYS = {
  status: (companyId: string | null) => ["patient-billing", "status", companyId] as const,
  charges: (companyId: string | null) => ["patient-billing", "charges", companyId] as const,
  admin: (companyId: string) => ["patient-billing", "admin", companyId] as const,
};

/** Recurso ligado na empresa do usuário (falha de leitura = desligado). */
export function usePatientBillingEnabled(companyId: string | null) {
  const q = useQuery({
    queryKey: PATIENT_BILLING_KEYS.status(companyId),
    queryFn: () => PatientBillingService.status(companyId as string),
    enabled: Boolean(companyId),
    staleTime: 60_000,
    retry: false,
  });
  const { isEnabled } = useActiveCompanyModules();
  return q.data?.enabled === true && isEnabled("PATIENT_BILLING");
}

/** Cobranças Asaas dos lançamentos exibidos, por id do lançamento. */
export function usePatientCharges(companyId: string | null, transactionIds: string[], on: boolean) {
  const ids = [...transactionIds].sort();
  return useQuery({
    queryKey: [...PATIENT_BILLING_KEYS.charges(companyId), ids.join(",")],
    queryFn: async () => {
      const list = await PatientBillingService.charges(companyId as string, ids);
      return Object.fromEntries(list.map((c) => [c.transactionId, c])) as Record<
        string,
        ChargeView
      >;
    },
    enabled: Boolean(companyId) && on && ids.length > 0,
  });
}

function reportWhatsApp(view: ChargeView) {
  if (view.status === "FAILED") {
    toast.error(view.error ?? "O Asaas não criou a cobrança. Tente de novo.");
  } else if (view.whatsappSent) {
    toast.success("Cobrança criada e link enviado pelo WhatsApp");
  } else {
    toast.warning(
      `Cobrança criada. WhatsApp não enviado: ${view.whatsappError ?? "verifique a conexão"}`,
    );
  }
}

export function useChargeTransaction(companyId: string | null) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (transactionId: string) =>
      PatientBillingService.charge(companyId as string, transactionId),
    onSuccess: (view) => {
      reportWhatsApp(view);
      qc.invalidateQueries({ queryKey: PATIENT_BILLING_KEYS.charges(companyId) });
    },
    onError: (e) => toast.error(toHttpError(e, "Não foi possível cobrar pelo Asaas.").message),
  });
}

export function useResendWhatsApp(companyId: string | null) {
  return useMutation({
    mutationFn: (transactionId: string) =>
      PatientBillingService.resendWhatsApp(companyId as string, transactionId),
    onSuccess: (view) =>
      view.whatsappSent
        ? toast.success("Link reenviado pelo WhatsApp")
        : toast.error(view.whatsappError ?? "WhatsApp não enviado."),
    onError: (e) => toast.error(toHttpError(e, "Não foi possível reenviar.").message),
  });
}

export function useAdminPatientBilling(companyId: string) {
  return useQuery({
    queryKey: PATIENT_BILLING_KEYS.admin(companyId),
    queryFn: () => PatientBillingService.adminStatus(companyId),
    enabled: Boolean(companyId),
  });
}

export function useSetAdminPatientBilling(companyId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (enabled: boolean) => PatientBillingService.adminSet(companyId, enabled),
    onSuccess: (r) => {
      qc.setQueryData(PATIENT_BILLING_KEYS.admin(companyId), r);
      qc.invalidateQueries({ queryKey: ["patient-billing"] });
      toast.success(r.enabled ? "Cobrança de pacientes ligada" : "Cobrança de pacientes desligada");
    },
    onError: (e) => toast.error(toHttpError(e, "Não foi possível alterar.").message),
  });
}
