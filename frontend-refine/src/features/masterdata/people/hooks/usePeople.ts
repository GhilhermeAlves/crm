import { useMutation, useQuery } from "@tanstack/react-query";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { useAuthorization } from "@/features/identity/auth/hooks/useAuthorization";
import { toHttpError } from "@/lib/http-error";
import { useMutationDefaults } from "@/lib/query/mutation-utils";
import { LocationService, PeopleService } from "../services/people.service";
import { useCompanyModules } from "@/features/identity/modules/hooks/useCompanyModules";
import type {
  FieldContext,
  FieldSettingChange,
  PersonDetail,
  PersonRequest,
} from "../types/people.types";

export function useCompanyId(): string | null {
  const { user } = useAuth();
  return user?.companyId ?? null;
}

/**
 * Tudo o que mostra dados de pessoa: a ficha nova, as listas dos menus e as telas antigas de
 * paciente/contato (mesmo id). Toda alteração invalida o conjunto (memória: atualizar tela
 * após mudar paciente).
 */
export function peopleKeys(companyId: string | null): unknown[][] {
  return [
    ["people", companyId],
    ["person", companyId],
    ["person-history", companyId],
    ["customers", companyId],
    ["suppliers", companyId],
    ["employees", companyId],
    ["patients", companyId],
    ["patient", companyId],
    ["contacts", companyId],
    ["contact", companyId],
    ["customer360", companyId],
  ];
}

export function usePeoplePermissions() {
  const { can } = useAuthorization();
  return {
    canRead: can("person:read"),
    canCreate: can("person:create"),
    canUpdate: can("person:update"),
    canDelete: can("person:delete"),
    canSensitive: can("person:sensitive"),
    canBank: can("person:bank"),
    canMerge: can("person:merge"),
    canSuppliers: can("supplier:manage"),
    canEmployees: can("employee:manage"),
    canSalary: can("employee:salary"),
    canLocations: can("location:manage"),
    canSettings: can("settings:update"),
  };
}

type ListParams = Record<string, string | number | boolean | undefined | null>;

export function usePeopleSearch(params: ListParams) {
  const companyId = useCompanyId();
  return useQuery({
    queryKey: ["people", companyId, params],
    queryFn: () => PeopleService.search(companyId as string, params),
    enabled: !!companyId,
    placeholderData: (prev) => prev,
  });
}

export function usePerson(id: string | null | undefined) {
  const companyId = useCompanyId();
  return useQuery({
    queryKey: ["person", companyId, id],
    queryFn: () => PeopleService.get(companyId as string, id as string),
    enabled: !!companyId && !!id,
    retry: (count, error) => toHttpError(error).status !== 404 && count < 2,
  });
}

export function usePersonHistory(id: string | null | undefined, enabled = true) {
  const companyId = useCompanyId();
  return useQuery({
    queryKey: ["person-history", companyId, id],
    queryFn: () => PeopleService.history(companyId as string, id as string),
    enabled: !!companyId && !!id && enabled,
  });
}

export function useBankAccounts(id: string, enabled: boolean) {
  const companyId = useCompanyId();
  return useQuery({
    queryKey: ["person", companyId, id, "bank"],
    queryFn: () => PeopleService.bankAccounts(companyId as string, id),
    enabled: !!companyId && enabled,
    retry: false,
  });
}

export function useCustomers(params: ListParams) {
  const companyId = useCompanyId();
  return useQuery({
    queryKey: ["customers", companyId, params],
    queryFn: () => PeopleService.customers(companyId as string, params),
    enabled: !!companyId,
    placeholderData: (prev) => prev,
  });
}

export function useSuppliers(params: ListParams) {
  const companyId = useCompanyId();
  return useQuery({
    queryKey: ["suppliers", companyId, params],
    queryFn: () => PeopleService.suppliers(companyId as string, params),
    enabled: !!companyId,
    placeholderData: (prev) => prev,
  });
}

export function useEmployees(params: ListParams) {
  const companyId = useCompanyId();
  return useQuery({
    queryKey: ["employees", companyId, params],
    queryFn: () => PeopleService.employees(companyId as string, params),
    enabled: !!companyId,
    placeholderData: (prev) => prev,
  });
}

export function useJobTitles() {
  const companyId = useCompanyId();
  return useQuery({
    queryKey: ["job-titles", companyId],
    queryFn: () => PeopleService.jobTitles(companyId as string),
    enabled: !!companyId,
  });
}

export function useTeamUsers(enabled: boolean) {
  const companyId = useCompanyId();
  return useQuery({
    queryKey: ["team-users", companyId],
    queryFn: () => PeopleService.teamUsers(companyId as string),
    enabled: !!companyId && enabled,
  });
}

export function useFieldSettings() {
  const companyId = useCompanyId();
  return useQuery({
    queryKey: ["person-field-settings", companyId],
    queryFn: () => PeopleService.fieldSettings(companyId as string),
    enabled: !!companyId,
  });
}

/** Mutação genérica do cadastro: executa e invalida tudo o que mostra pessoas. */
export function usePeopleMutation<TVars, TData = unknown>(
  fn: (companyId: string, vars: TVars) => Promise<TData>,
  successMessage?: string,
  errorMessage = "Não foi possível salvar",
) {
  const companyId = useCompanyId();
  const { onSuccess, onError } = useMutationDefaults<TData, TVars>();
  return useMutation({
    mutationFn: (vars: TVars) => fn(companyId as string, vars),
    onSuccess: onSuccess({
      successMessage,
      invalidateKeys: [...peopleKeys(companyId), ["job-titles", companyId]],
    }),
    onError: onError({ errorMessage }),
  });
}

export function useCreatePerson() {
  return usePeopleMutation<PersonRequest, PersonDetail>(
    (c, data) => PeopleService.create(c, data),
    "Cadastro criado",
  );
}

export function useUpdatePerson(id: string) {
  return usePeopleMutation<PersonRequest, PersonDetail>(
    (c, data) => PeopleService.update(c, id, data),
    "Cadastro salvo",
  );
}

export function useSaveFieldSettings() {
  const companyId = useCompanyId();
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (changes: FieldSettingChange[]) =>
      PeopleService.saveFieldSettings(companyId as string, changes),
    onSuccess: onSuccess({
      successMessage: "Obrigatoriedade salva",
      invalidateKeys: [["person-field-settings", companyId]],
    }),
    onError: onError({ errorMessage: "Não foi possível salvar" }),
  });
}

// ---------------------------------------------------------------- localidades

export function useCountries(onlyActive = true) {
  return useQuery({
    queryKey: ["countries", onlyActive],
    queryFn: () => LocationService.countries(onlyActive),
    staleTime: 60 * 60 * 1000,
  });
}

export function useStates(countryId: number | null | undefined, onlyActive = true) {
  return useQuery({
    queryKey: ["states", countryId, onlyActive],
    queryFn: () => LocationService.states(countryId as number, onlyActive),
    enabled: !!countryId,
    staleTime: 60 * 60 * 1000,
  });
}

export function useCities(stateId: number | null | undefined, onlyActive = true) {
  return useQuery({
    queryKey: ["cities", stateId, onlyActive],
    queryFn: () => LocationService.cities(stateId as number, undefined, onlyActive),
    enabled: !!stateId,
    staleTime: 60 * 60 * 1000,
  });
}

export function useNeighborhoods(cityId: number | null | undefined, onlyActive = true) {
  return useQuery({
    queryKey: ["neighborhoods", cityId, onlyActive],
    queryFn: () => LocationService.neighborhoods(cityId as number, undefined, onlyActive),
    enabled: !!cityId,
  });
}

/** Empresa da área da saúde (segmento diferente de Geral): o cliente é o paciente. */
export function useHealthSegment(): boolean {
  const companyId = useCompanyId();
  const { data } = useCompanyModules(companyId);
  return !!data && data.segment !== "GENERIC";
}

/** Campos obrigatórios efetivos (seção 18) para os contextos informados. */
export function useRequiredKeys(contexts: FieldContext[]): string[] {
  const { data } = useFieldSettings();
  if (!data) return [];
  return data.fields
    .filter((f) => f.systemRequired || contexts.some((c) => f.contexts[c] === true))
    .map((f) => f.fieldKey);
}
