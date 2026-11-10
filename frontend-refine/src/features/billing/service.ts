import api from "@/lib/api";
import type { BillingView, CompanyBillingStatus, InvoiceView } from "./types";

const admin = (companyId: string) => `/admin/billing/companies/${companyId}`;

export const BillingService = {
  my: async () => (await api.get<BillingView>("/billing")).data,
  myInvoices: async () => (await api.get<InvoiceView[]>("/billing/invoices")).data,
  checkPayment: async () => (await api.post<BillingView>("/billing/check-payment")).data,

  adminView: async (companyId: string) => (await api.get<BillingView>(admin(companyId))).data,
  adminInvoices: async (companyId: string) =>
    (await api.get<InvoiceView[]>(`${admin(companyId)}/invoices`)).data,
  activate: async (companyId: string) =>
    (await api.post<BillingView>(`${admin(companyId)}/activate`)).data,
  exempt: async (companyId: string) =>
    (await api.post<BillingView>(`${admin(companyId)}/exempt`)).data,
  cancel: async (companyId: string) =>
    (await api.post<BillingView>(`${admin(companyId)}/cancel`)).data,
  changePrice: async (companyId: string, monthlyPriceCents: number) =>
    (await api.put<BillingView>(`${admin(companyId)}/price`, { monthlyPriceCents })).data,
  grant: async (companyId: string, days: number) =>
    (await api.post<BillingView>(`${admin(companyId)}/grant`, { days })).data,
  statuses: async () => (await api.get<CompanyBillingStatus[]>("/admin/billing/companies")).data,
};
