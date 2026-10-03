export type LocationKind = "GOOGLE_MEET" | "PHONE" | "IN_PERSON";
export type AssignmentMode = "ROUND_ROBIN" | "CHOOSE_HOST";
export type AppointmentStatus = "SCHEDULED" | "CONFIRMED" | "CANCELED" | "COMPLETED" | "NO_SHOW";
export type AppointmentSource = "INTERNAL" | "PUBLIC_LINK";
export type BlockSource = "INTERNAL" | "GOOGLE";

export type AppointmentType = {
  id: string;
  companyId: string;
  name: string;
  slug: string;
  description: string | null;
  durationMinutes: number;
  bufferBeforeMinutes: number;
  bufferAfterMinutes: number;
  minNoticeHours: number;
  maxDaysAhead: number;
  slotIntervalMinutes: number;
  color: string | null;
  locationKind: LocationKind | null;
  locationDetail: string | null;
  assignmentMode: AssignmentMode | null;
  publicBookingEnabled: boolean;
  active: boolean;
  hostIds: string[];
  createdAt: string;
  updatedAt: string;
};

export type Appointment = {
  id: string;
  companyId: string;
  appointmentTypeId: string | null;
  hostId: string;
  contactId: string | null;
  opportunityId: string | null;
  title: string;
  startAt: string;
  endAt: string;
  status: AppointmentStatus;
  source: AppointmentSource;
  locationKind: LocationKind | null;
  locationDetail: string | null;
  meetingUrl: string | null;
  notes: string | null;
  cancelReason: string | null;
  publicToken: string | null;
  googleEventId: string | null;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
};

export type ScheduleBlock = {
  id: string;
  companyId: string;
  hostId: string;
  startAt: string;
  endAt: string;
  reason: string | null;
  source: BlockSource;
  externalId: string | null;
  createdBy: string;
  createdAt: string;
};

export type AvailabilityRule = {
  weekday: number;
  startTime: string;
  endTime: string;
};

export type AvailabilityOverride = {
  date: string;
  windows: { start: string; end: string }[];
};

export type Availability = {
  timezone: string;
  rules: AvailabilityRule[];
  overrides: AvailabilityOverride[];
};

export type Slot = {
  start: string;
  end: string;
};

export type CreateAppointmentTypeRequest = {
  name: string;
  slug?: string;
  description?: string;
  durationMinutes: number;
  bufferBeforeMinutes?: number;
  bufferAfterMinutes?: number;
  minNoticeHours?: number;
  maxDaysAhead?: number;
  slotIntervalMinutes?: number;
  color?: string;
  locationKind?: LocationKind;
  locationDetail?: string;
  assignmentMode?: AssignmentMode;
  publicBookingEnabled?: boolean;
  hostIds: string[];
};

export type UpdateAppointmentTypeRequest = Partial<CreateAppointmentTypeRequest>;

export type CreateAppointmentRequest = {
  appointmentTypeId?: string;
  hostId: string;
  contactId?: string;
  opportunityId?: string;
  title: string;
  startAt: string;
  endAt: string;
  locationKind?: LocationKind;
  locationDetail?: string;
  notes?: string;
  force?: boolean;
};

export type UpdateAppointmentRequest = {
  title?: string;
  contactId?: string;
  opportunityId?: string;
  locationKind?: LocationKind;
  locationDetail?: string;
  notes?: string;
};

export type RescheduleRequest = {
  startAt: string;
  endAt: string;
  force?: boolean;
};

export type CreateBlockRequest = {
  hostId: string;
  startAt: string;
  endAt: string;
  reason?: string;
};

export const APPOINTMENT_STATUS_LABELS: Record<AppointmentStatus, string> = {
  SCHEDULED: "Agendado",
  CONFIRMED: "Confirmado",
  CANCELED: "Cancelado",
  COMPLETED: "Concluído",
  NO_SHOW: "Não compareceu",
};

export const APPOINTMENT_STATUS_COLORS: Record<AppointmentStatus, string> = {
  SCHEDULED: "bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200",
  CONFIRMED: "bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200",
  CANCELED: "bg-gray-100 text-gray-800 dark:bg-gray-900 dark:text-gray-200",
  COMPLETED: "bg-emerald-100 text-emerald-800 dark:bg-emerald-900 dark:text-emerald-200",
  NO_SHOW: "bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200",
};

export const LOCATION_KIND_LABELS: Record<LocationKind, string> = {
  GOOGLE_MEET: "Google Meet",
  PHONE: "Telefone",
  IN_PERSON: "Presencial",
};
