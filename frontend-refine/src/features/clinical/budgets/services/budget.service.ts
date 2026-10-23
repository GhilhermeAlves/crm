import api from "@/lib/api";
import type {
  ApproveBudgetRequest,
  BudgetDetail,
  BudgetRequest,
  BudgetSummary,
} from "../types/budget.types";

const base = (companyId: string, contactId: string) =>
  `/companies/${companyId}/patients/${contactId}/budgets`;

export const budgetService = {
  list: async (companyId: string, contactId: string): Promise<BudgetSummary[]> => {
    const response = await api.get(base(companyId, contactId));
    return response.data;
  },
  get: async (companyId: string, contactId: string, budgetId: string): Promise<BudgetDetail> => {
    const response = await api.get(`${base(companyId, contactId)}/${budgetId}`);
    return response.data;
  },
  create: async (
    companyId: string,
    contactId: string,
    payload: BudgetRequest,
  ): Promise<BudgetDetail> => {
    const response = await api.post(base(companyId, contactId), payload);
    return response.data;
  },
  update: async (
    companyId: string,
    contactId: string,
    budgetId: string,
    payload: BudgetRequest,
  ): Promise<BudgetDetail> => {
    const response = await api.put(`${base(companyId, contactId)}/${budgetId}`, payload);
    return response.data;
  },
  approve: async (
    companyId: string,
    contactId: string,
    budgetId: string,
    payload: ApproveBudgetRequest,
  ): Promise<BudgetDetail> => {
    const response = await api.post(`${base(companyId, contactId)}/${budgetId}/approve`, payload);
    return response.data;
  },
  lose: async (companyId: string, contactId: string, budgetId: string): Promise<BudgetDetail> => {
    const response = await api.post(`${base(companyId, contactId)}/${budgetId}/lose`);
    return response.data;
  },
  delete: async (companyId: string, contactId: string, budgetId: string): Promise<void> => {
    await api.delete(`${base(companyId, contactId)}/${budgetId}`);
  },
};
