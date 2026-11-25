"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { AlertTriangle } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { ROUTES } from "@/lib/constants";
import { useCompanyId, useCountries } from "../hooks/usePeople";
import { PeopleService } from "../services/people.service";
import { GENDER_OPTIONS, MARITAL_OPTIONS, formatTaxId, isValidTaxId } from "../lib/people-labels";
import type { DuplicateMember, PersonRequest } from "../types/people.types";
import { Field, NativeSelect } from "./FormBits";

/** Dados de identidade (persons). Avisa CPF/CNPJ inválido ou já cadastrado ao digitar (doc 22.1). */
export function IdentityFields({
  value,
  onChange,
  excludeId,
  requiredKeys = [],
  canSensitive = true,
}: {
  value: PersonRequest;
  onChange: (v: PersonRequest) => void;
  excludeId?: string;
  requiredKeys?: string[];
  canSensitive?: boolean;
}) {
  const companyId = useCompanyId();
  const { data: countries } = useCountries();
  const [dups, setDups] = useState<DuplicateMember[]>([]);
  const set = (patch: Partial<PersonRequest>) => onChange({ ...value, ...patch });
  const pj = value.personType === "J";
  const req = (key: string) => requiredKeys.includes(key) && !value.provisional;
  const tax = (value.taxId ?? "").replace(/[^0-9A-Za-z]/g, "").toUpperCase();
  const masked = (value.taxId ?? "").includes("*");
  const taxInvalid =
    !masked &&
    tax.length > 0 &&
    tax.length === (pj ? 14 : 11) &&
    !isValidTaxId(value.personType, tax);

  useEffect(() => {
    if (!companyId || masked || tax.length < 11 || taxInvalid) {
      setDups([]);
      return;
    }
    const t = setTimeout(() => {
      PeopleService.check(companyId, { taxId: tax, excludeId })
        .then(setDups)
        .catch(() => setDups([]));
    }, 400);
    return () => clearTimeout(t);
  }, [companyId, tax, excludeId, masked, taxInvalid]);

  return (
    <div className="space-y-4">
      <fieldset className="flex flex-wrap items-center gap-4">
        <legend className="sr-only">Tipo de pessoa</legend>
        {(["F", "J"] as const).map((t) => (
          <label key={t} className="flex items-center gap-2 text-sm">
            <input
              type="radio"
              name="personType"
              value={t}
              checked={value.personType === t}
              onChange={() => set({ personType: t })}
            />
            {t === "F" ? "Pessoa física" : "Pessoa jurídica"}
          </label>
        ))}
        <label className="ml-auto flex items-center gap-2 text-sm text-muted-foreground">
          <input
            type="checkbox"
            checked={!!value.provisional}
            onChange={(e) => set({ provisional: e.target.checked })}
          />
          Cadastro provisório (sem documento ou estrangeiro)
        </label>
      </fieldset>

      <div className="grid gap-4 sm:grid-cols-6">
        <Field
          label={pj ? "Razão social" : "Nome completo"}
          htmlFor="p-name"
          required
          className="sm:col-span-4"
        >
          <Input
            id="p-name"
            value={value.name}
            maxLength={150}
            onChange={(e) => set({ name: e.target.value })}
          />
        </Field>
        <Field
          label={pj ? "CNPJ" : "CPF"}
          htmlFor="p-tax"
          className="sm:col-span-2"
          required={req("PERSON_TAX_ID")}
          hint={masked ? "Sem permissão para ver o documento completo." : undefined}
        >
          <Input
            id="p-tax"
            value={
              masked
                ? (value.taxId ?? "")
                : tax.length >= (pj ? 14 : 11)
                  ? formatTaxId(tax)
                  : (value.taxId ?? "")
            }
            disabled={masked || !canSensitive}
            maxLength={18}
            aria-invalid={taxInvalid}
            onChange={(e) => set({ taxId: e.target.value })}
          />
          {taxInvalid && (
            <p className="text-xs text-destructive">{pj ? "CNPJ inválido." : "CPF inválido."}</p>
          )}
        </Field>
        {pj && (
          <Field
            label="Nome fantasia"
            htmlFor="p-trade"
            className="sm:col-span-3"
            required={req("PERSON_TRADE_NAME")}
          >
            <Input
              id="p-trade"
              value={value.tradeName ?? ""}
              maxLength={150}
              onChange={(e) => set({ tradeName: e.target.value })}
            />
          </Field>
        )}
        <Field
          label={pj ? "Inscrição estadual" : "RG"}
          htmlFor="p-rg"
          className="sm:col-span-2"
          required={req("PERSON_RG_IE")}
        >
          <Input
            id="p-rg"
            value={value.rgOrIe ?? ""}
            maxLength={30}
            onChange={(e) => set({ rgOrIe: e.target.value })}
          />
        </Field>
        {!pj && (
          <Field label="Órgão emissor" htmlFor="p-rgi" className="sm:col-span-1">
            <Input
              id="p-rgi"
              value={value.rgIssuer ?? ""}
              maxLength={20}
              onChange={(e) => set({ rgIssuer: e.target.value })}
            />
          </Field>
        )}
        <Field
          label={pj ? "Data de abertura" : "Nascimento"}
          htmlFor="p-birth"
          className="sm:col-span-2"
          required={req("PERSON_BIRTH_DATE")}
        >
          <Input
            id="p-birth"
            type="date"
            value={value.birthOrFoundationDate ?? ""}
            onChange={(e) => set({ birthOrFoundationDate: e.target.value || null })}
          />
        </Field>
        {!pj && (
          <>
            <Field label="Sexo" htmlFor="p-gender" className="sm:col-span-2">
              <NativeSelect
                id="p-gender"
                value={value.gender ?? ""}
                onChange={(e) => set({ gender: e.target.value || null })}
              >
                <option value="">—</option>
                {GENDER_OPTIONS.concat(
                  value.gender && !GENDER_OPTIONS.includes(value.gender) ? [value.gender] : [],
                ).map((g) => (
                  <option key={g} value={g}>
                    {g}
                  </option>
                ))}
              </NativeSelect>
            </Field>
            <Field label="Estado civil" htmlFor="p-marital" className="sm:col-span-2">
              <NativeSelect
                id="p-marital"
                value={value.maritalStatus ?? ""}
                onChange={(e) => set({ maritalStatus: e.target.value || null })}
              >
                <option value="">—</option>
                {MARITAL_OPTIONS.concat(
                  value.maritalStatus && !MARITAL_OPTIONS.includes(value.maritalStatus)
                    ? [value.maritalStatus]
                    : [],
                ).map((g) => (
                  <option key={g} value={g}>
                    {g}
                  </option>
                ))}
              </NativeSelect>
            </Field>
            <Field label="Nacionalidade" htmlFor="p-nat" className="sm:col-span-2">
              <NativeSelect
                id="p-nat"
                value={value.countryId ?? 1}
                onChange={(e) => {
                  const c = countries?.find((x) => x.id === Number(e.target.value));
                  set({ countryId: c?.id ?? 1, nationality: c?.nationality ?? null });
                }}
              >
                {(countries ?? [{ id: 1, name: "Brasil", nationality: "Brasileira" }]).map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.nationality ?? c.name}
                  </option>
                ))}
              </NativeSelect>
            </Field>
          </>
        )}
        <Field label="Observações" htmlFor="p-notes" className="sm:col-span-6">
          <Textarea
            id="p-notes"
            rows={2}
            value={value.notes ?? ""}
            onChange={(e) => set({ notes: e.target.value })}
          />
        </Field>
      </div>

      {dups.length > 0 && (
        <div
          role="alert"
          className="flex gap-3 rounded-md border border-amber-300 bg-amber-50 p-3 text-sm text-amber-900 dark:bg-amber-950 dark:text-amber-100"
        >
          <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
          <div>
            Este documento já está em:{" "}
            {dups.map((d, i) => (
              <span key={d.id}>
                {i > 0 && ", "}
                <Link className="font-semibold underline" href={`${ROUTES.PEOPLE}/${d.id}`}>
                  {d.name}
                </Link>
              </span>
            ))}
            . Abra o cadastro existente em vez de criar outro.
          </div>
        </div>
      )}
    </div>
  );
}
