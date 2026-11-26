import api from "@/lib/api";
import type {
  Address,
  AddressRequest,
  BankAccount,
  BankAccountRequest,
  City,
  ContactChannel,
  ContactChannelRequest,
  Country,
  CustomerRequest,
  CustomerRow,
  DocumentRequest,
  DuplicateMember,
  EmployeeRequest,
  EmployeeRow,
  FieldSettingChange,
  FieldSettings,
  HistoryEntry,
  JobTitle,
  Neighborhood,
  PageResult,
  PersonDetail,
  PersonDocument,
  PersonRequest,
  PersonSummary,
  PostalCodeResult,
  Relationship,
  RelationshipRequest,
  Specialty,
  SpecialtyRequest,
  StateItem,
  SupplierRequest,
  SupplierRow,
  UserOption,
} from "../types/people.types";

const base = (companyId: string) => `/companies/${companyId}`;
const person = (companyId: string, id: string) => `${base(companyId)}/persons/${id}`;

type Params = Record<string, string | number | boolean | undefined | null>;

function clean(params: Params) {
  return Object.fromEntries(
    Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== ""),
  );
}

export const PeopleService = {
  // ------------------------------------------------------------ pessoas
  async search(companyId: string, params: Params) {
    const r = await api.get<PageResult<PersonSummary>>(`${base(companyId)}/persons`, {
      params: clean(params),
    });
    return r.data;
  },
  async check(companyId: string, params: { taxId?: string; phone?: string; excludeId?: string }) {
    const r = await api.get<DuplicateMember[]>(`${base(companyId)}/persons/check`, {
      params: clean(params),
    });
    return r.data;
  },
  async get(companyId: string, id: string) {
    return (await api.get<PersonDetail>(person(companyId, id))).data;
  },
  async create(companyId: string, data: PersonRequest) {
    return (await api.post<PersonDetail>(`${base(companyId)}/persons`, data)).data;
  },
  async update(companyId: string, id: string, data: PersonRequest) {
    return (await api.put<PersonDetail>(person(companyId, id), data)).data;
  },
  async setStatus(companyId: string, id: string, active: boolean) {
    return (await api.patch<PersonDetail>(`${person(companyId, id)}/status`, { active })).data;
  },
  async remove(companyId: string, id: string, reason: string) {
    await api.delete(person(companyId, id), { data: { reason } });
  },
  async restore(companyId: string, id: string) {
    return (await api.post<PersonDetail>(`${person(companyId, id)}/restore`)).data;
  },
  async history(companyId: string, id: string) {
    return (await api.get<HistoryEntry[]>(`${person(companyId, id)}/history`)).data;
  },

  // ------------------------------------------------------------ filhas
  async addAddress(companyId: string, id: string, data: AddressRequest) {
    return (await api.post<Address[]>(`${person(companyId, id)}/addresses`, data)).data;
  },
  async updateAddress(companyId: string, id: string, addressId: string, data: AddressRequest) {
    return (await api.put<Address[]>(`${person(companyId, id)}/addresses/${addressId}`, data)).data;
  },
  async deleteAddress(companyId: string, id: string, addressId: string) {
    return (await api.delete<Address[]>(`${person(companyId, id)}/addresses/${addressId}`)).data;
  },
  async addChannel(companyId: string, id: string, data: ContactChannelRequest) {
    return (await api.post<ContactChannel[]>(`${person(companyId, id)}/contact-channels`, data))
      .data;
  },
  async updateChannel(
    companyId: string,
    id: string,
    channelId: string,
    data: ContactChannelRequest,
  ) {
    return (
      await api.put<ContactChannel[]>(
        `${person(companyId, id)}/contact-channels/${channelId}`,
        data,
      )
    ).data;
  },
  async deleteChannel(companyId: string, id: string, channelId: string) {
    return (
      await api.delete<ContactChannel[]>(`${person(companyId, id)}/contact-channels/${channelId}`)
    ).data;
  },
  async addDocument(companyId: string, id: string, data: DocumentRequest) {
    return (await api.post<PersonDocument[]>(`${person(companyId, id)}/documents`, data)).data;
  },
  async deleteDocument(companyId: string, id: string, docId: string) {
    return (await api.delete<PersonDocument[]>(`${person(companyId, id)}/documents/${docId}`)).data;
  },
  async bankAccounts(companyId: string, id: string) {
    return (await api.get<BankAccount[]>(`${person(companyId, id)}/bank-accounts`)).data;
  },
  async addBankAccount(companyId: string, id: string, data: BankAccountRequest) {
    return (await api.post<BankAccount[]>(`${person(companyId, id)}/bank-accounts`, data)).data;
  },
  async deleteBankAccount(companyId: string, id: string, accountId: string) {
    return (await api.delete<BankAccount[]>(`${person(companyId, id)}/bank-accounts/${accountId}`))
      .data;
  },
  async addRelationship(companyId: string, id: string, data: RelationshipRequest) {
    return (await api.post<Relationship[]>(`${person(companyId, id)}/relationships`, data)).data;
  },
  async deleteRelationship(companyId: string, id: string, relId: string) {
    return (await api.delete<Relationship[]>(`${person(companyId, id)}/relationships/${relId}`))
      .data;
  },

  // ------------------------------------------------------------ papéis
  async saveCustomer(companyId: string, id: string, data: CustomerRequest) {
    return (await api.put<PersonDetail>(`${person(companyId, id)}/customer`, data)).data;
  },
  async disableCustomer(companyId: string, id: string) {
    return (await api.delete<PersonDetail>(`${person(companyId, id)}/customer`)).data;
  },
  async saveSupplier(companyId: string, id: string, data: SupplierRequest) {
    return (await api.put<PersonDetail>(`${person(companyId, id)}/supplier`, data)).data;
  },
  async disableSupplier(companyId: string, id: string) {
    return (await api.delete<PersonDetail>(`${person(companyId, id)}/supplier`)).data;
  },
  async saveEmployee(companyId: string, id: string, data: EmployeeRequest) {
    return (await api.put<PersonDetail>(`${person(companyId, id)}/employee`, data)).data;
  },
  async disableEmployee(companyId: string, id: string) {
    return (await api.delete<PersonDetail>(`${person(companyId, id)}/employee`)).data;
  },
  async addSpecialty(companyId: string, id: string, data: SpecialtyRequest) {
    return (await api.post<Specialty[]>(`${person(companyId, id)}/employee/specialties`, data))
      .data;
  },
  async deleteSpecialty(companyId: string, id: string, specialtyId: string) {
    return (
      await api.delete<Specialty[]>(`${person(companyId, id)}/employee/specialties/${specialtyId}`)
    ).data;
  },

  // ------------------------------------------------------------ listas
  async customers(companyId: string, params: Params) {
    return (
      await api.get<PageResult<CustomerRow>>(`${base(companyId)}/customers`, {
        params: clean(params),
      })
    ).data;
  },
  async suppliers(companyId: string, params: Params) {
    return (
      await api.get<PageResult<SupplierRow>>(`${base(companyId)}/suppliers`, {
        params: clean(params),
      })
    ).data;
  },
  async employees(companyId: string, params: Params) {
    return (
      await api.get<PageResult<EmployeeRow>>(`${base(companyId)}/employees`, {
        params: clean(params),
      })
    ).data;
  },
  async teamUsers(companyId: string) {
    return (await api.get<UserOption[]>(`${base(companyId)}/employees/team-users`)).data;
  },
  async jobTitles(companyId: string) {
    return (await api.get<JobTitle[]>(`${base(companyId)}/job-titles`)).data;
  },
  async saveJobTitle(
    companyId: string,
    id: string | null,
    data: { title: string; active?: boolean },
  ) {
    const url = id ? `${base(companyId)}/job-titles/${id}` : `${base(companyId)}/job-titles`;
    return (id ? await api.put<JobTitle[]>(url, data) : await api.post<JobTitle[]>(url, data)).data;
  },

  // ------------------------------------------------------------ obrigatoriedade
  async fieldSettings(companyId: string) {
    return (await api.get<FieldSettings>(`${base(companyId)}/settings/person-fields`)).data;
  },
  async saveFieldSettings(companyId: string, changes: FieldSettingChange[]) {
    return (await api.put<FieldSettings>(`${base(companyId)}/settings/person-fields`, { changes }))
      .data;
  },
  async resetFieldSettings(companyId: string) {
    return (await api.post<FieldSettings>(`${base(companyId)}/settings/person-fields/reset`)).data;
  },
};

export const LocationService = {
  async countries(onlyActive = true) {
    return (await api.get<Country[]>("/countries", { params: { onlyActive } })).data;
  },
  async states(countryId: number, onlyActive = true) {
    return (
      await api.get<StateItem[]>(`/countries/${countryId}/states`, { params: { onlyActive } })
    ).data;
  },
  async cities(stateId: number, q?: string, onlyActive = true) {
    return (
      await api.get<City[]>(`/states/${stateId}/cities`, {
        params: clean({ q, onlyActive, limit: 6000 }),
      })
    ).data;
  },
  async neighborhoods(cityId: number, q?: string, onlyActive = true) {
    return (
      await api.get<Neighborhood[]>(`/cities/${cityId}/neighborhoods`, {
        params: clean({ q, onlyActive }),
      })
    ).data;
  },
  async postalCode(cep: string) {
    return (await api.get<PostalCodeResult>(`/postal-codes/${cep.replace(/\D/g, "")}`)).data;
  },
  async saveCountry(id: number | null, data: Partial<Country>) {
    return (
      id
        ? await api.put<Country>(`/countries/${id}`, data)
        : await api.post<Country>("/countries", data)
    ).data;
  },
  async saveState(id: number | null, data: Partial<StateItem>) {
    return (
      id
        ? await api.put<StateItem>(`/states/${id}`, data)
        : await api.post<StateItem>("/states", data)
    ).data;
  },
  async saveCity(id: number | null, data: Partial<City>) {
    return (id ? await api.put<City>(`/cities/${id}`, data) : await api.post<City>("/cities", data))
      .data;
  },
  async saveNeighborhood(
    id: string | null,
    data: { cityId: number; name: string; active?: boolean },
  ) {
    return (
      id
        ? await api.put<Neighborhood>(`/neighborhoods/${id}`, data)
        : await api.post<Neighborhood>("/neighborhoods", data)
    ).data;
  },
};
