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
    const r = await api.get<Availability>(`${BASE}/${companyId}/users/${userId}/availability`);
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

export interface BirthdayMessageSettings {
  enabled: boolean;
  template: string;
}

export const BirthdayMessageService = {
  async get(companyId: string): Promise<BirthdayMessageSettings | null> {
    const r = await api.get<BirthdayMessageSettings>(`${BASE}/${companyId}/birthday-message`);
    return r.status === 204 ? null : r.data;
  },

  async save(companyId: string, data: BirthdayMessageSettings): Promise<BirthdayMessageSettings> {
    const r = await api.put<BirthdayMessageSettings>(`${BASE}/${companyId}/birthday-message`, data);
    return r.data;
  },
};

/** Ajustes da agenda da empresa. Horários no formato "HH:mm". */
export interface AgendaSettings {
  defaultDurationMinutes: number;
  workHoursStart: string;
  workHoursEnd: string;
  notifyOnBooking: boolean;
  notify24hBefore: boolean;
  notify1hBefore: boolean;
  notifyOnCancel: boolean;
  notifyOnReschedule: boolean;
}

/** Mesmos valores padrão do backend (V099), usados enquanto os ajustes carregam. */
export const DEFAULT_AGENDA_SETTINGS: AgendaSettings = {
  defaultDurationMinutes: 30,
  workHoursStart: "08:00",
  workHoursEnd: "18:00",
  notifyOnBooking: true,
  notify24hBefore: true,
  notify1hBefore: false,
  notifyOnCancel: true,
  notifyOnReschedule: true,
};

/** O backend devolve LocalTime como "08:00:00"; a tela trabalha com "08:00". */
export function normalizeAgendaSettings(data: AgendaSettings): AgendaSettings {
  return {
    ...data,
    workHoursStart: data.workHoursStart.slice(0, 5),
    workHoursEnd: data.workHoursEnd.slice(0, 5),
  };
}

export const AgendaSettingsService = {
  async get(companyId: string): Promise<AgendaSettings> {
    const r = await api.get<AgendaSettings>(`${BASE}/${companyId}/agenda-settings`);
    return normalizeAgendaSettings(r.data);
  },

  async save(companyId: string, data: AgendaSettings): Promise<AgendaSettings> {
    const r = await api.put<AgendaSettings>(`${BASE}/${companyId}/agenda-settings`, data);
    return normalizeAgendaSettings(r.data);
  },
};
