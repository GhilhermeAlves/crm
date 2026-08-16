export type TenantStatus = "active" | "suspended" | "onboarding" | "inactive";

export type TenantPlan = "starter" | "professional" | "business" | "enterprise";

export type TenantAddress = {
  zipCode: string;
  street: string;
  number: string;
  complement: string;
  neighborhood: string;
  city: string;
  state: string;
  country: string;
};

export type Tenant = {
  id: string;
  legalName: string;
  tradingName: string;
  cnpj: string;
  stateRegistration: string;
  municipalRegistration: string;
  email: string;
  phone: string;
  website: string;
  status: TenantStatus;
  plan: TenantPlan;
  maxUsers: number;
  maxStorageMb: number;
  maxContacts: number;
  logoUrl: string | null;
  notes: string;
  address: TenantAddress;
  createdAt: string;
  updatedAt: string;
};

export type CreateTenantRequest = Omit<Tenant, "id" | "createdAt" | "updatedAt">;

export type UpdateTenantRequest = Partial<CreateTenantRequest>;

export type ListTenantsParams = {
  page?: number;
  pageSize?: number;
  search?: string;
  status?: TenantStatus;
  plan?: TenantPlan;
  sortBy?: string;
  sortDirection?: "asc" | "desc";
};
