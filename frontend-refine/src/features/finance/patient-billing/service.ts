import api from "@/lib/api";

export type ChargeStatus = "PENDING" | "RECEIVED" | "RECEIVED_IN_CASH" | "CANCELLED" | "FAILED";

export type ChargeView = {
  transactionId: string;
  status: ChargeStatus;
  invoiceUrl: string | null;
  error: string | null;
  whatsappSent: boolean;
  whatsappError: string | null;
};

const base = (companyId: string) => `/companies/${companyId}/patient-billing`;

export const PatientBillingService = {
  status: async (companyId: string) => (await api.get<{ enabled: boolean }>(base(companyId))).data,
  charges: async (companyId: string, transactionIds: string[]) =>
    (
      await api.get<ChargeView[]>(`${base(companyId)}/charges`, {
        params: { transactionIds: transactionIds.join(",") },
      })
    ).data,
  charge: async (companyId: string, transactionId: string) =>
    (await api.post<ChargeView>(`${base(companyId)}/transactions/${transactionId}/charge`)).data,
  resendWhatsApp: async (companyId: string, transactionId: string) =>
    (await api.post<ChargeView>(`${base(companyId)}/transactions/${transactionId}/whatsapp`)).data,
  adminStatus: async (companyId: string) =>
    (await api.get<{ enabled: boolean }>(`/admin/billing/companies/${companyId}/patient-billing`))
      .data,
  adminSet: async (companyId: string, enabled: boolean) =>
    (
      await api.put<{ enabled: boolean }>(`/admin/billing/companies/${companyId}/patient-billing`, {
        enabled,
      })
    ).data,
};
