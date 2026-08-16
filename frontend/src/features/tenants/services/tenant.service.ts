import api from "@/lib/api";
import type {
  Tenant,
  CreateTenantRequest,
  UpdateTenantRequest,
  ListTenantsParams,
} from "../types/tenant.types";

const BASE_PATH = "/companies";

/**
 * Contrato de criação no backend (`CreateCompanyRequest`): campos de endereço
 * achatados (`addressZipCode`, `addressStreet`, ...) e plano/status em UPPERCASE.
 * Aqui é o único ponto de normalização do wire-format para a criação — o
 * formulário mantém `address` aninhado; esta camada achata e ajusta o case.
 */
export function mapCreateTenantRequest(data: CreateTenantRequest) {
  return {
    legalName: data.legalName,
    tradingName: data.tradingName,
    cnpj: data.cnpj,
    stateRegistration: data.stateRegistration ?? "",
    municipalRegistration: data.municipalRegistration ?? "",
    email: data.email,
    phone: data.phone,
    website: data.website ?? "",
    addressZipCode: data.address.zipCode,
    addressStreet: data.address.street,
    addressNumber: data.address.number,
    addressComplement: data.address.complement ?? "",
    addressNeighborhood: data.address.neighborhood,
    addressCity: data.address.city,
    addressState: data.address.state,
    addressCountry: data.address.country ?? "Brasil",
    plan: data.plan.toUpperCase(),
    maxUsers: data.maxUsers,
    maxStorageMb: data.maxStorageMb,
    maxContacts: data.maxContacts,
    logoUrl: data.logoUrl ?? null,
    notes: data.notes ?? "",
  };
}

/**
 * Contrato de atualização no backend (`UpdateCompanyRequest`): campos de
 * endereço achatados + plano/status em UPPERCASE. Atualiza apenas os campos
 * definidos (payload parcial).
 */
export function mapUpdateTenantRequest(data: UpdateTenantRequest) {
  return {
    ...(data.legalName !== undefined && { legalName: data.legalName }),
    ...(data.tradingName !== undefined && { tradingName: data.tradingName }),
    ...(data.email !== undefined && { email: data.email }),
    ...(data.phone !== undefined && { phone: data.phone }),
    ...(data.website !== undefined && { website: data.website }),
    ...(data.address && {
      addressZipCode: data.address.zipCode,
      addressStreet: data.address.street,
      addressNumber: data.address.number,
      addressComplement: data.address.complement ?? "",
      addressNeighborhood: data.address.neighborhood,
      addressCity: data.address.city,
      addressState: data.address.state,
      addressCountry: data.address.country ?? "Brasil",
    }),
    ...(data.plan !== undefined && { plan: data.plan.toUpperCase() }),
    ...(data.status !== undefined && { status: data.status.toUpperCase() }),
    ...(data.maxUsers !== undefined && { maxUsers: data.maxUsers }),
    ...(data.maxStorageMb !== undefined && { maxStorageMb: data.maxStorageMb }),
    ...(data.maxContacts !== undefined && { maxContacts: data.maxContacts }),
    ...(data.logoUrl !== undefined && { logoUrl: data.logoUrl }),
    ...(data.notes !== undefined && { notes: data.notes }),
  };
}

export const TenantService = {
  async list(params?: ListTenantsParams): Promise<Tenant[]> {
    const response = await api.get<Tenant[]>(BASE_PATH, { params });
    return response.data;
  },

  async findById(id: string): Promise<Tenant> {
    const response = await api.get<Tenant>(`${BASE_PATH}/${id}`);
    return response.data;
  },

  async create(data: CreateTenantRequest): Promise<Tenant> {
    const response = await api.post<Tenant>(BASE_PATH, mapCreateTenantRequest(data));
    return response.data;
  },

  async update(id: string, data: UpdateTenantRequest): Promise<Tenant> {
    const response = await api.put<Tenant>(`${BASE_PATH}/${id}`, mapUpdateTenantRequest(data));
    return response.data;
  },

  async delete(id: string): Promise<void> {
    await api.delete(`${BASE_PATH}/${id}`);
  },
};
