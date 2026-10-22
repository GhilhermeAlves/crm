import api from "@/lib/api";
import type {
  CompanyHoliday,
  CreateHolidayRequest,
  UpdateHolidayRequest,
} from "../types/holiday.types";

export class HolidayService {
  static async list(companyId: string, from?: string, to?: string): Promise<CompanyHoliday[]> {
    const params = from && to ? { from, to } : undefined;
    const response = await api.get<CompanyHoliday[]>(`/api/v1/companies/${companyId}/holidays`, {
      params,
    });
    return response.data;
  }

  static async create(companyId: string, data: CreateHolidayRequest): Promise<CompanyHoliday> {
    const response = await api.post<CompanyHoliday>(
      `/api/v1/companies/${companyId}/holidays`,
      data,
    );
    return response.data;
  }

  static async update(
    companyId: string,
    id: string,
    data: UpdateHolidayRequest,
  ): Promise<CompanyHoliday> {
    const response = await api.put<CompanyHoliday>(
      `/api/v1/companies/${companyId}/holidays/${id}`,
      data,
    );
    return response.data;
  }

  static async delete(companyId: string, id: string): Promise<void> {
    await api.delete(`/api/v1/companies/${companyId}/holidays/${id}`);
  }
}
