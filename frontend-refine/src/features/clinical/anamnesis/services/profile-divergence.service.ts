import api from "@/lib/api";

export type ProfileDivergenceField =
  "NAME" | "BIRTH_DATE" | "GENDER" | "CPF" | "MOBILE" | "EMAIL" | "ADDRESS";

export type ProfileDivergence = {
  id: string;
  field: ProfileDivergenceField;
  currentValue: string | null;
  proposedValue: string;
  reason: string | null;
  createdAt: string;
};

export const PROFILE_FIELD_LABELS: Record<ProfileDivergenceField, string> = {
  NAME: "Nome",
  BIRTH_DATE: "Nascimento",
  GENDER: "Sexo",
  CPF: "CPF",
  MOBILE: "Celular",
  EMAIL: "E-mail",
  ADDRESS: "Endereço",
};

const base = (companyId: string, contactId: string) =>
  `/companies/${companyId}/patients/${contactId}/profile-divergences`;

export const profileDivergenceService = {
  list: async (companyId: string, contactId: string): Promise<ProfileDivergence[]> =>
    (await api.get(base(companyId, contactId))).data,
  resolve: async (
    companyId: string,
    contactId: string,
    id: string,
    action: "accept" | "dismiss",
  ): Promise<ProfileDivergence[]> =>
    (await api.post(`${base(companyId, contactId)}/${id}/${action}`)).data,
};
