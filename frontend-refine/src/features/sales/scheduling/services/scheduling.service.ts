import api from "@/lib/api";
import type {
  AppointmentType,
  Appointment,
  ScheduleBlock,
  Availability,
  Slot,
  CreateAppointmentTypeRequest,
  UpdateAppointmentTypeRequest,
  CreateAppointmentRequest,
  UpdateAppointmentRequest,
  RescheduleRequest,
  CreateBlockRequest,
  AppointmentStatus,
} from "../types/scheduling.types";

const BASE = "/companies";

export const AppointmentTypeService = {
  async list(companyId: string): Promise<AppointmentType[]> {
    const r = await api.get<AppointmentType[]>(`${BASE}/${companyId}/appointment-types`);
    return r.data;
  },

  async getById(companyId: string, id: string): Promise<AppointmentType> {
    const r = await api.get<AppointmentType>(`${BASE}/${companyId}/appointment-types/${id}`);
    return r.data;
  },

  async create(companyId: string, data: CreateAppointmentTypeRequest): Promise<AppointmentType> {
    const r = await api.post<AppointmentType>(`${BASE}/${companyId}/appointment-types`, data);
    return r.data;
  },

  async update(
    companyId: string,
    id: string,
    data: UpdateAppointmentTypeRequest,
  ): Promise<AppointmentType> {
    const r = await api.put<AppointmentType>(`${BASE}/${companyId}/appointment-types/${id}`, data);
    return r.data;
  },

  async delete(companyId: string, id: string): Promise<void> {
    await api.delete(`${BASE}/${companyId}/appointment-types/${id}`);
  },

  async getSlots(companyId: string, typeId: string, from: string, to: string): Promise<Slot[]> {
    const r = await api.get<Slot[]>(
      `${BASE}/${companyId}/appointment-types/${typeId}/slots?from=${from}&to=${to}`,
    );
    return r.data;
  },
};

export const AppointmentService = {
  async list(
    companyId: string,
    from: string,
    to: string,
    hostIds?: string[],
  ): Promise<Appointment[]> {
    const params = new URLSearchParams({ from, to });
    hostIds?.forEach((id) => params.append("hostIds", id));
    const r = await api.get<Appointment[]>(`${BASE}/${companyId}/appointments?${params}`);
    return r.data;
  },

  async getById(companyId: string, id: string): Promise<Appointment> {
    const r = await api.get<Appointment>(`${BASE}/${companyId}/appointments/${id}`);
    return r.data;
  },

  async create(companyId: string, data: CreateAppointmentRequest): Promise<Appointment> {
    const r = await api.post<Appointment>(`${BASE}/${companyId}/appointments`, data);
    return r.data;
  },

  async update(
    companyId: string,
    id: string,
    data: UpdateAppointmentRequest,
  ): Promise<Appointment> {
    const r = await api.put<Appointment>(`${BASE}/${companyId}/appointments/${id}`, data);
    return r.data;
  },

  async reschedule(companyId: string, id: string, data: RescheduleRequest): Promise<Appointment> {
    const r = await api.patch<Appointment>(
      `${BASE}/${companyId}/appointments/${id}/reschedule`,
      data,
    );
    return r.data;
  },

  async changeStatus(
    companyId: string,
    id: string,
    status: AppointmentStatus,
  ): Promise<Appointment> {
    const r = await api.post<Appointment>(
      `${BASE}/${companyId}/appointments/${id}/status/${status}`,
    );
    return r.data;
  },

  async delete(companyId: string, id: string): Promise<void> {
    await api.delete(`${BASE}/${companyId}/appointments/${id}`);
  },
};

export const BlockService = {
  async list(
    companyId: string,
    from: string,
    to: string,
    hostIds?: string[],
  ): Promise<ScheduleBlock[]> {
    const params = new URLSearchParams({ from, to });
    hostIds?.forEach((id) => params.append("hostIds", id));
    const r = await api.get<ScheduleBlock[]>(`${BASE}/${companyId}/blocks?${params}`);
    return r.data;
  },

  async getById(companyId: string, id: string): Promise<ScheduleBlock> {
    const r = await api.get<ScheduleBlock>(`${BASE}/${companyId}/blocks/${id}`);
    return r.data;
  },

  async create(companyId: string, data: CreateBlockRequest): Promise<ScheduleBlock> {
    const r = await api.post<ScheduleBlock>(`${BASE}/${companyId}/blocks`, data);
    return r.data;
  },

  async delete(companyId: string, id: string): Promise<void> {
    await api.delete(`${BASE}/${companyId}/blocks/${id}`);
  },
};

export const AvailabilityService = {
  async get(companyId: string, userId: string): Promise<Availability> {
    const r = await api.get<Availability>(
      `${BASE}/${companyId}/users/${userId}/availability`,
    );
    return r.data;
  },

  async set(companyId: string, userId: string, data: Availability): Promise<Availability> {
    const r = await api.put<Availability>(
      `${BASE}/${companyId}/users/${userId}/availability`,
      data,
    );
    return r.data;
  },
};
