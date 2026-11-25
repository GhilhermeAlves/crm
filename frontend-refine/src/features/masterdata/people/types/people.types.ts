// Cadastro central de pessoas (doc "Arquitetura do Cadastro de Pessoas" v1.4, seções 9 e 22).

export type PersonType = "F" | "J";
export type PersonRole = "CUSTOMER" | "PATIENT" | "SUPPLIER" | "EMPLOYEE";

export type PageResult<T> = { items: T[]; total: number; page: number; size: number };

export type PersonSummary = {
  id: string;
  personType: PersonType;
  name: string;
  tradeName: string | null;
  taxId: string | null;
  provisional: boolean;
  active: boolean;
  roles: PersonRole[];
  mobile: string | null;
  email: string | null;
  city: string | null;
  uf: string | null;
  createdAt: string;
};

export type Address = {
  id: string;
  addressType: AddressType;
  postalCode: string | null;
  street: string | null;
  addressNumber: string | null;
  complement: string | null;
  neighborhoodId: string | null;
  neighborhoodName: string | null;
  cityId: number | null;
  cityName: string | null;
  uf: string | null;
  countryId: number | null;
  foreignRegion: string | null;
  primary: boolean;
  active: boolean;
};

export type AddressType = "RESIDENTIAL" | "COMMERCIAL" | "BILLING" | "DELIVERY" | "OTHER";

export type AddressRequest = {
  addressType?: AddressType;
  postalCode?: string | null;
  street?: string | null;
  addressNumber?: string | null;
  complement?: string | null;
  neighborhoodId?: string | null;
  neighborhoodName?: string | null;
  cityId?: number | null;
  cityName?: string | null;
  uf?: string | null;
  countryId?: number | null;
  foreignRegion?: string | null;
  primary?: boolean;
};

export type ContactType = "MOBILE" | "PHONE" | "EMAIL" | "WEBSITE" | "OTHER";

export type ContactChannel = {
  id: string;
  contactType: ContactType;
  value: string;
  contactName: string | null;
  whatsapp: boolean;
  primary: boolean;
  active: boolean;
};

export type ContactChannelRequest = {
  contactType: ContactType;
  value: string;
  contactName?: string | null;
  whatsapp?: boolean;
  primary?: boolean;
};

export type DocumentType = "RG" | "CNH" | "PASSPORT" | "CNS" | "RNE" | "SOCIAL_CONTRACT" | "OTHER";

export type PersonDocument = {
  id: string;
  documentType: DocumentType;
  documentNumber: string;
  issuer: string | null;
  issuerUf: string | null;
  issueDate: string | null;
  expiryDate: string | null;
  storageObjectId: string | null;
  primary: boolean;
};

export type DocumentRequest = Omit<PersonDocument, "id" | "primary"> & { primary?: boolean };

export type BankAccount = {
  id: string;
  bankCode: string | null;
  branchNumber: string | null;
  accountNumber: string | null;
  accountDigit: string | null;
  accountType: "CHECKING" | "SAVINGS" | "PAYMENT" | "OTHER" | null;
  pixKey: string | null;
  pixKeyType: "CPF" | "CNPJ" | "EMAIL" | "PHONE" | "RANDOM" | null;
  holderName: string | null;
  primary: boolean;
};

export type BankAccountRequest = Omit<BankAccount, "id" | "primary"> & { primary?: boolean };

export type RelationshipType = "GUARDIAN" | "SPOUSE" | "PARTNER" | "LEGAL_REPRESENTATIVE" | "OTHER";

export type Relationship = {
  id: string;
  relatedPersonId: string;
  relatedName: string;
  relationshipType: RelationshipType;
  /** OUT: a outra pessoa é {tipo} desta. IN: esta pessoa é {tipo} da outra. */
  direction: "OUT" | "IN";
  startDate: string | null;
  endDate: string | null;
  notes: string | null;
};

export type RelationshipRequest = {
  relatedPersonId: string;
  relationshipType: RelationshipType;
  direction: "OUT" | "IN";
  notes?: string | null;
};

export type PatientInfo = {
  recordNumber: string | null;
  healthPlan: string | null;
  healthPlanNumber: string | null;
  referralSource: string | null;
};

export type CustomerInfo = {
  customerCode: string | null;
  creditLimit: number | null;
  blocked: boolean;
  blockReason: string | null;
  customerSince: string | null;
  active: boolean;
  notes: string | null;
  patient: PatientInfo | null;
};

export type CustomerRequest = {
  creditLimit?: number | null;
  blocked?: boolean;
  blockReason?: string | null;
  customerSince?: string | null;
  active?: boolean;
  notes?: string | null;
  patient?: PatientInfo | null;
};

export type SupplierType = "LAB" | "MATERIAL" | "SERVICE" | "OTHER";

export type SupplierInfo = {
  supplierCode: string | null;
  supplierType: SupplierType;
  paymentTermsDays: number | null;
  blocked: boolean;
  blockReason: string | null;
  active: boolean;
  notes: string | null;
};

export type SupplierRequest = Partial<Omit<SupplierInfo, "supplierCode">>;

export type SpecialtyCode = "GENERIC" | "DENTAL" | "TRICHOLOGY" | "OCCUPATIONAL_SAFETY";

export type Specialty = {
  id: string;
  specialty: SpecialtyCode;
  council: string | null;
  councilNumber: string | null;
  councilUf: string | null;
  primary: boolean;
};

export type SpecialtyRequest = Omit<Specialty, "id" | "primary"> & { primary?: boolean };

export type EmployeeStatus = "ACTIVE" | "ON_LEAVE" | "TERMINATED";
export type ContractType = "CLT" | "PJ" | "INTERN" | "OTHER";

export type EmployeeInfo = {
  registrationNumber: string | null;
  jobTitleId: string | null;
  jobTitle: string | null;
  hireDate: string | null;
  terminationDate: string | null;
  contractType: ContractType | null;
  status: EmployeeStatus;
  salary: number | null;
  salaryVisible: boolean;
  userId: string | null;
  userName: string | null;
  notes: string | null;
  specialties: Specialty[];
};

export type EmployeeRequest = {
  registrationNumber?: string | null;
  jobTitleId?: string | null;
  hireDate?: string | null;
  terminationDate?: string | null;
  contractType?: ContractType | null;
  status?: EmployeeStatus;
  salary?: number | null;
  userId?: string | null;
  notes?: string | null;
};

export type PersonDetail = {
  id: string;
  personType: PersonType;
  name: string;
  tradeName: string | null;
  taxId: string | null;
  rgOrIe: string | null;
  rgIssuer: string | null;
  birthOrFoundationDate: string | null;
  gender: string | null;
  maritalStatus: string | null;
  professionalStatus: string | null;
  nationality: string | null;
  countryId: number | null;
  provisional: boolean;
  active: boolean;
  deleted: boolean;
  deleteReason: string | null;
  notes: string | null;
  avatarUrl: string | null;
  hasContact: boolean;
  healthSegment: boolean;
  roles: PersonRole[];
  addresses: Address[];
  contacts: ContactChannel[];
  documents: PersonDocument[];
  relationships: Relationship[];
  customer: CustomerInfo | null;
  supplier: SupplierInfo | null;
  employee: EmployeeInfo | null;
  openIssues: number;
  createdAt: string;
  updatedAt: string;
};

export type PersonRequest = {
  personType: PersonType;
  name: string;
  tradeName?: string | null;
  taxId?: string | null;
  rgOrIe?: string | null;
  rgIssuer?: string | null;
  birthOrFoundationDate?: string | null;
  gender?: string | null;
  maritalStatus?: string | null;
  professionalStatus?: string | null;
  nationality?: string | null;
  countryId?: number | null;
  provisional?: boolean;
  notes?: string | null;
  // somente na inclusão
  roles?: ("CUSTOMER" | "SUPPLIER" | "EMPLOYEE")[];
  address?: AddressRequest | null;
  mobile?: string | null;
  email?: string | null;
  customer?: CustomerRequest | null;
  supplier?: SupplierRequest | null;
  employee?: EmployeeRequest | null;
};

export type HistoryEntry = {
  id: string;
  occurredAt: string;
  operation: string;
  tableName: string;
  oldValues: Record<string, unknown> | null;
  newValues: Record<string, unknown> | null;
  reason: string | null;
  userId: string | null;
  userName: string;
};

export type CustomerRow = {
  id: string;
  name: string;
  taxId: string | null;
  mobile: string | null;
  customerCode: string | null;
  customerSince: string | null;
  active: boolean;
  blocked: boolean;
  patient: boolean;
  recordNumber: string | null;
  healthPlan: string | null;
  birthDate: string | null;
};

export type SupplierRow = {
  id: string;
  name: string;
  tradeName: string | null;
  taxId: string | null;
  supplierCode: string | null;
  supplierType: SupplierType;
  phone: string | null;
  email: string | null;
  active: boolean;
  blocked: boolean;
};

export type EmployeeRow = {
  id: string;
  name: string;
  registrationNumber: string | null;
  jobTitle: string | null;
  specialties: SpecialtyCode[];
  council: string | null;
  userId: string | null;
  userName: string | null;
  hireDate: string | null;
  status: EmployeeStatus;
};

export type JobTitle = { id: string; title: string; active: boolean };
export type UserOption = { id: string; name: string; email: string };

export type DuplicateMember = {
  id: string;
  name: string;
  taxId: string | null;
  birthDate: string | null;
  mobile: string | null;
  createdAt: string;
  issueId: string | null;
  issueType: string | null;
};

export type DuplicateGroup = { reason: string; key: string | null; members: DuplicateMember[] };

export type MergeResult = {
  keptId: string;
  removedId: string;
  movedTables: string[];
  skippedTables: string[];
};

export type FieldContext = "PF" | "PJ" | "CUSTOMER" | "SUPPLIER" | "EMPLOYEE";

export type FieldSetting = {
  fieldKey: string;
  label: string;
  groupName:
    "IDENTIFICATION" | "ADDRESS" | "CONTACT" | "COMPLEMENTARY" | "PATIENT" | "PROFESSIONAL";
  systemRequired: boolean;
  sortOrder: number;
  /** null = não se aplica ao contexto */
  contexts: Record<FieldContext, boolean | null>;
};

export type FieldSettings = {
  fields: FieldSetting[];
  updatedAt: string | null;
  updatedByName: string | null;
};

export type FieldSettingChange = { fieldKey: string; context: FieldContext; required: boolean };

// ---------------------------------------------------------------- localidades

export type Country = {
  id: number;
  name: string;
  iso2: string;
  iso3: string | null;
  bacenCode: number | null;
  dialCode: string | null;
  nationality: string | null;
  active: boolean;
};

export type StateItem = {
  id: number;
  countryId: number;
  name: string;
  uf: string;
  ibgeCode: number | null;
  region: string | null;
  active: boolean;
};

export type City = {
  id: number;
  stateId: number;
  name: string;
  uf: string;
  ibgeCode: number | null;
  areaCode: string | null;
  capital: boolean;
  active: boolean;
};

export type Neighborhood = {
  id: string;
  cityId: number;
  name: string;
  source: "M" | "C" | "I";
  active: boolean;
};

export type PostalCodeResult = {
  postalCode: string;
  street: string | null;
  complement: string | null;
  neighborhoodId: string | null;
  neighborhoodName: string | null;
  cityId: number | null;
  cityName: string | null;
  uf: string | null;
  ibgeCode: number | null;
};
