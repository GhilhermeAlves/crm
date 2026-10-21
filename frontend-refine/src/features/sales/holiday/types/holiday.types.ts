export type CompanyHoliday = {
  id: string;
  companyId: string;
  date: string;
  name: string;
  recurringYearly: boolean;
  blocksSchedule: boolean;
  createdAt: string;
  updatedAt: string;
};

export type CreateHolidayRequest = {
  date: string;
  name: string;
  recurringYearly?: boolean;
  blocksSchedule?: boolean;
};

export type UpdateHolidayRequest = Partial<CreateHolidayRequest>;
