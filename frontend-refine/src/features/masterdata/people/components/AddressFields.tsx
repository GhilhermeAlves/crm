"use client";

import { useEffect, useMemo, useState } from "react";
import { Loader2, Search } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { toHttpError } from "@/lib/http-error";
import { LocationService } from "../services/people.service";
import { useCities, useCountries, useStates } from "../hooks/usePeople";
import { ADDRESS_TYPE_LABELS, formatCep, onlyDigits } from "../lib/people-labels";
import type { AddressRequest, AddressType } from "../types/people.types";
import { Field, NativeSelect } from "./FormBits";

const BRAZIL = 1;

/**
 * Campos de endereço: CEP com busca automática (ViaCEP → cidade pelo IBGE → bairro),
 * combos País → Estado → Cidade para o Brasil e texto livre no exterior (doc 19 e 20).
 */
export function AddressFields({
  value,
  onChange,
  idPrefix,
  requiredKeys = [],
  showType = true,
}: {
  value: AddressRequest;
  onChange: (v: AddressRequest) => void;
  idPrefix: string;
  requiredKeys?: string[];
  showType?: boolean;
}) {
  const countryId = value.countryId ?? BRAZIL;
  const foreign = countryId !== BRAZIL;
  const { data: countries } = useCountries();
  const { data: states } = useStates(foreign ? null : BRAZIL);
  const [stateId, setStateId] = useState<number | null>(null);
  const { data: cities, isLoading: loadingCities } = useCities(stateId);
  const [looking, setLooking] = useState(false);

  // Estado selecionado a partir da UF gravada.
  useEffect(() => {
    if (!states || foreign) return;
    const st = states.find((s) => s.uf === value.uf);
    setStateId(st?.id ?? null);
  }, [states, value.uf, foreign]);

  const sortedCities = useMemo(() => cities ?? [], [cities]);
  const set = (patch: Partial<AddressRequest>) => onChange({ ...value, ...patch });
  const req = (key: string) => requiredKeys.includes(key);

  async function lookup() {
    const cep = onlyDigits(value.postalCode);
    if (cep.length !== 8) {
      toast.error("Informe um CEP com 8 dígitos.");
      return;
    }
    setLooking(true);
    try {
      const r = await LocationService.postalCode(cep);
      onChange({
        ...value,
        postalCode: cep,
        street: r.street ?? value.street,
        complement: value.complement || r.complement,
        neighborhoodId: r.neighborhoodId,
        neighborhoodName: r.neighborhoodName ?? value.neighborhoodName,
        cityId: r.cityId,
        cityName: r.cityName,
        uf: r.uf,
        countryId: BRAZIL,
      });
    } catch (e) {
      toast.error(toHttpError(e, "Não foi possível consultar o CEP").message);
    } finally {
      setLooking(false);
    }
  }

  return (
    <div className="grid gap-4 sm:grid-cols-6">
      {showType && (
        <Field label="Tipo" htmlFor={`${idPrefix}-type`} className="sm:col-span-2">
          <NativeSelect
            id={`${idPrefix}-type`}
            value={value.addressType ?? "RESIDENTIAL"}
            onChange={(e) => set({ addressType: e.target.value as AddressType })}
          >
            {Object.entries(ADDRESS_TYPE_LABELS).map(([k, l]) => (
              <option key={k} value={k}>
                {l}
              </option>
            ))}
          </NativeSelect>
        </Field>
      )}
      <Field
        label="País"
        htmlFor={`${idPrefix}-country`}
        className="sm:col-span-2"
        required={req("PERSON_COUNTRY")}
      >
        <NativeSelect
          id={`${idPrefix}-country`}
          value={countryId}
          onChange={(e) =>
            set({ countryId: Number(e.target.value), cityId: null, neighborhoodId: null })
          }
        >
          {(countries ?? [{ id: BRAZIL, name: "Brasil" }]).map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </NativeSelect>
      </Field>
      {!foreign && (
        <Field
          label="CEP"
          htmlFor={`${idPrefix}-cep`}
          className="sm:col-span-2"
          required={req("PERSON_POSTAL_CODE")}
        >
          <div className="flex gap-2">
            <Input
              id={`${idPrefix}-cep`}
              inputMode="numeric"
              value={formatCep(value.postalCode)}
              maxLength={9}
              onChange={(e) => set({ postalCode: onlyDigits(e.target.value).slice(0, 8) })}
              onBlur={() => onlyDigits(value.postalCode).length === 8 && !value.cityId && lookup()}
            />
            <Button
              type="button"
              variant="outline"
              size="icon"
              onClick={lookup}
              disabled={looking}
              aria-label="Buscar endereço pelo CEP"
            >
              {looking ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <Search className="h-4 w-4" />
              )}
            </Button>
          </div>
        </Field>
      )}
      <Field
        label="Logradouro"
        htmlFor={`${idPrefix}-street`}
        className="sm:col-span-4"
        required={req("PERSON_STREET")}
      >
        <Input
          id={`${idPrefix}-street`}
          value={value.street ?? ""}
          maxLength={160}
          onChange={(e) => set({ street: e.target.value })}
        />
      </Field>
      <Field
        label="Número"
        htmlFor={`${idPrefix}-number`}
        className="sm:col-span-2"
        required={req("PERSON_ADDRESS_NUMBER")}
      >
        <Input
          id={`${idPrefix}-number`}
          value={value.addressNumber ?? ""}
          maxLength={20}
          onChange={(e) => set({ addressNumber: e.target.value })}
        />
      </Field>
      <Field label="Complemento" htmlFor={`${idPrefix}-complement`} className="sm:col-span-3">
        <Input
          id={`${idPrefix}-complement`}
          value={value.complement ?? ""}
          maxLength={80}
          onChange={(e) => set({ complement: e.target.value })}
        />
      </Field>
      <Field
        label="Bairro"
        htmlFor={`${idPrefix}-nb`}
        className="sm:col-span-3"
        required={req("PERSON_NEIGHBORHOOD")}
      >
        <Input
          id={`${idPrefix}-nb`}
          value={value.neighborhoodName ?? ""}
          maxLength={80}
          onChange={(e) => set({ neighborhoodName: e.target.value, neighborhoodId: null })}
        />
      </Field>
      {foreign ? (
        <>
          <Field
            label="Cidade"
            htmlFor={`${idPrefix}-fcity`}
            className="sm:col-span-3"
            required={req("PERSON_CITY")}
          >
            <Input
              id={`${idPrefix}-fcity`}
              value={value.cityName ?? ""}
              onChange={(e) => set({ cityName: e.target.value, cityId: null, uf: null })}
            />
          </Field>
          <Field label="Região / estado" htmlFor={`${idPrefix}-region`} className="sm:col-span-3">
            <Input
              id={`${idPrefix}-region`}
              value={value.foreignRegion ?? ""}
              onChange={(e) => set({ foreignRegion: e.target.value })}
            />
          </Field>
        </>
      ) : (
        <>
          <Field
            label="Estado"
            htmlFor={`${idPrefix}-uf`}
            className="sm:col-span-2"
            required={req("PERSON_STATE")}
          >
            <NativeSelect
              id={`${idPrefix}-uf`}
              value={stateId ?? ""}
              onChange={(e) => {
                const st = states?.find((s) => s.id === Number(e.target.value));
                setStateId(st?.id ?? null);
                set({ uf: st?.uf ?? null, cityId: null, cityName: null, neighborhoodId: null });
              }}
            >
              <option value="">Selecione</option>
              {(states ?? []).map((s) => (
                <option key={s.id} value={s.id}>
                  {s.uf} — {s.name}
                </option>
              ))}
            </NativeSelect>
          </Field>
          <Field
            label="Cidade"
            htmlFor={`${idPrefix}-city`}
            className="sm:col-span-4"
            required={req("PERSON_CITY")}
          >
            <NativeSelect
              id={`${idPrefix}-city`}
              value={value.cityId ?? ""}
              disabled={!stateId || loadingCities}
              onChange={(e) => {
                const c = sortedCities.find((x) => x.id === Number(e.target.value));
                set({ cityId: c?.id ?? null, cityName: c?.name ?? null, neighborhoodId: null });
              }}
            >
              <option value="">{loadingCities ? "Carregando…" : "Selecione"}</option>
              {value.cityId == null && value.cityName && (
                <option value="" disabled>
                  {value.cityName} (texto antigo — escolha na lista)
                </option>
              )}
              {sortedCities.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </NativeSelect>
          </Field>
        </>
      )}
    </div>
  );
}
