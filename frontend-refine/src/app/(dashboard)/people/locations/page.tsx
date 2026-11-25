"use client";

import { useMemo, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Loader2, Pencil, Plus } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { toHttpError } from "@/lib/http-error";
import { Field, NativeSelect, Pill } from "@/features/masterdata/people/components/FormBits";
import {
  useCities,
  useCountries,
  useNeighborhoods,
  usePeoplePermissions,
  useStates,
} from "@/features/masterdata/people/hooks/usePeople";
import { LocationService } from "@/features/masterdata/people/services/people.service";

type Editing =
  | { kind: "country"; id: number | null; values: Record<string, string> }
  | { kind: "state"; id: number | null; values: Record<string, string> }
  | { kind: "city"; id: number | null; values: Record<string, string> }
  | { kind: "neighborhood"; id: string | null; values: Record<string, string> };

const FIELDS: Record<Editing["kind"], { key: string; label: string; type?: string }[]> = {
  country: [
    { key: "name", label: "Nome" },
    { key: "iso2", label: "Sigla ISO2" },
    { key: "iso3", label: "Sigla ISO3" },
    { key: "bacenCode", label: "Código BACEN", type: "number" },
    { key: "dialCode", label: "DDI" },
    { key: "nationality", label: "Nacionalidade" },
  ],
  state: [
    { key: "name", label: "Nome" },
    { key: "uf", label: "Sigla" },
    { key: "ibgeCode", label: "Código IBGE", type: "number" },
    { key: "region", label: "Região" },
  ],
  city: [
    { key: "name", label: "Nome" },
    { key: "ibgeCode", label: "Código IBGE (7 dígitos)", type: "number" },
    { key: "areaCode", label: "DDD" },
  ],
  neighborhood: [{ key: "name", label: "Nome" }],
};

/** Pessoas → Localidades (doc 19.8 e 22.7). Brasil vem carregado do IBGE. */
export default function LocationsPage() {
  const { canLocations, canUpdate } = usePeoplePermissions();
  const qc = useQueryClient();
  const [tab, setTab] = useState("cities");
  const [countryId, setCountryId] = useState(1);
  const [stateId, setStateId] = useState<number | null>(null);
  const [cityId, setCityId] = useState<number | null>(null);
  const [filter, setFilter] = useState("");
  const [editing, setEditing] = useState<Editing | null>(null);
  const [saving, setSaving] = useState(false);

  const { data: countries } = useCountries(false);
  const { data: states } = useStates(countryId, false);
  const { data: cities, isLoading: loadingCities } = useCities(stateId, false);
  const { data: neighborhoods, isLoading: loadingNb } = useNeighborhoods(cityId, false);

  const norm = (s: string) => s.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase();
  const visibleCities = useMemo(
    () => (cities ?? []).filter((c) => !filter || norm(c.name).includes(norm(filter))),
    [cities, filter],
  );

  async function save() {
    if (!editing) return;
    setSaving(true);
    const v = editing.values;
    const num = (k: string) => (v[k] ? Number(v[k]) : null);
    try {
      if (editing.kind === "country") {
        await LocationService.saveCountry(editing.id, {
          name: v.name,
          iso2: v.iso2,
          iso3: v.iso3 || null,
          bacenCode: num("bacenCode"),
          dialCode: v.dialCode || null,
          nationality: v.nationality || null,
          active: v.active !== "false",
        });
      } else if (editing.kind === "state") {
        await LocationService.saveState(editing.id, {
          countryId,
          name: v.name,
          uf: v.uf,
          ibgeCode: num("ibgeCode"),
          region: v.region || null,
          active: v.active !== "false",
        });
      } else if (editing.kind === "city") {
        await LocationService.saveCity(editing.id, {
          stateId: stateId ?? undefined,
          name: v.name,
          ibgeCode: num("ibgeCode"),
          areaCode: v.areaCode || null,
          active: v.active !== "false",
        });
      } else {
        await LocationService.saveNeighborhood(editing.id, {
          cityId: cityId as number,
          name: v.name,
          active: v.active !== "false",
        });
      }
      toast.success("Salvo");
      ["countries", "states", "cities", "neighborhoods"].forEach((k) =>
        qc.invalidateQueries({ queryKey: [k] }),
      );
      setEditing(null);
    } catch (e) {
      toast.error(toHttpError(e, "Não foi possível salvar").message);
    } finally {
      setSaving(false);
    }
  }

  const statusPill = (active: boolean) =>
    active ? <Pill tone="green">Ativo</Pill> : <Pill>Inativo</Pill>;
  const editBtn = (e: Editing) =>
    canLocations && (
      <Button size="icon" variant="ghost" aria-label="Editar" onClick={() => setEditing(e)}>
        <Pencil className="h-4 w-4" />
      </Button>
    );

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold tracking-tight">Localidades</h1>
      <p className="text-sm text-muted-foreground">
        Países, estados e cidades vêm da carga oficial (IBGE). Registros usados não são excluídos,
        só inativados. Bairros são criados pela busca de CEP ou à mão.
      </p>
      <div className="flex flex-wrap gap-3">
        <NativeSelect
          aria-label="País"
          className="w-48"
          value={countryId}
          onChange={(e) => {
            setCountryId(Number(e.target.value));
            setStateId(null);
            setCityId(null);
          }}
        >
          {(countries ?? []).map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </NativeSelect>
        <NativeSelect
          aria-label="Estado"
          className="w-56"
          value={stateId ?? ""}
          onChange={(e) => {
            setStateId(e.target.value ? Number(e.target.value) : null);
            setCityId(null);
          }}
        >
          <option value="">Estado…</option>
          {(states ?? []).map((s) => (
            <option key={s.id} value={s.id}>
              {s.uf} — {s.name}
            </option>
          ))}
        </NativeSelect>
        <NativeSelect
          aria-label="Cidade"
          className="w-64"
          value={cityId ?? ""}
          disabled={!stateId}
          onChange={(e) => setCityId(e.target.value ? Number(e.target.value) : null)}
        >
          <option value="">Cidade…</option>
          {(cities ?? []).map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </NativeSelect>
      </div>

      <Tabs value={tab} onValueChange={setTab}>
        <TabsList>
          <TabsTrigger value="countries">Países</TabsTrigger>
          <TabsTrigger value="states">Estados</TabsTrigger>
          <TabsTrigger value="cities">Cidades</TabsTrigger>
          <TabsTrigger value="neighborhoods">Bairros</TabsTrigger>
        </TabsList>

        <TabsContent value="countries" className="space-y-3">
          {canLocations && (
            <Button
              size="sm"
              variant="outline"
              onClick={() => setEditing({ kind: "country", id: null, values: {} })}
            >
              <Plus className="mr-1 h-4 w-4" /> País
            </Button>
          )}
          <ul className="divide-y rounded-lg border text-sm">
            {(countries ?? []).map((c) => (
              <li key={c.id} className="flex items-center gap-3 px-4 py-2">
                <span className="w-48 font-medium">{c.name}</span>
                <span className="w-16">{c.iso2}</span>
                <span className="w-24 text-muted-foreground">BACEN {c.bacenCode ?? "—"}</span>
                <span className="w-20 text-muted-foreground">DDI {c.dialCode ?? "—"}</span>
                {statusPill(c.active)}
                <span className="ml-auto">
                  {editBtn({
                    kind: "country",
                    id: c.id,
                    values: {
                      name: c.name,
                      iso2: c.iso2,
                      iso3: c.iso3 ?? "",
                      bacenCode: String(c.bacenCode ?? ""),
                      dialCode: c.dialCode ?? "",
                      nationality: c.nationality ?? "",
                      active: String(c.active),
                    },
                  })}
                </span>
              </li>
            ))}
          </ul>
        </TabsContent>

        <TabsContent value="states" className="space-y-3">
          {canLocations && (
            <Button
              size="sm"
              variant="outline"
              onClick={() => setEditing({ kind: "state", id: null, values: {} })}
            >
              <Plus className="mr-1 h-4 w-4" /> Estado
            </Button>
          )}
          <ul className="divide-y rounded-lg border text-sm">
            {(states ?? []).map((s) => (
              <li key={s.id} className="flex items-center gap-3 px-4 py-2">
                <span className="w-10 font-semibold">{s.uf}</span>
                <span className="w-56">{s.name}</span>
                <span className="w-24 text-muted-foreground">IBGE {s.ibgeCode ?? "—"}</span>
                <span className="w-28 text-muted-foreground">{s.region ?? ""}</span>
                {statusPill(s.active)}
                <span className="ml-auto">
                  {editBtn({
                    kind: "state",
                    id: s.id,
                    values: {
                      name: s.name,
                      uf: s.uf,
                      ibgeCode: String(s.ibgeCode ?? ""),
                      region: s.region ?? "",
                      active: String(s.active),
                    },
                  })}
                </span>
              </li>
            ))}
          </ul>
        </TabsContent>

        <TabsContent value="cities" className="space-y-3">
          {!stateId ? (
            <p className="text-sm text-muted-foreground">Escolha um estado.</p>
          ) : (
            <>
              <div className="flex flex-wrap gap-2">
                <Input
                  aria-label="Filtrar cidades"
                  className="max-w-xs"
                  placeholder="Filtrar por nome"
                  value={filter}
                  onChange={(e) => setFilter(e.target.value)}
                />
                {canLocations && (
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => setEditing({ kind: "city", id: null, values: {} })}
                  >
                    <Plus className="mr-1 h-4 w-4" /> Cidade
                  </Button>
                )}
              </div>
              {loadingCities ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <ul className="max-h-[60vh] divide-y overflow-y-auto rounded-lg border text-sm">
                  {visibleCities.map((c) => (
                    <li key={c.id} className="flex items-center gap-3 px-4 py-2">
                      <span className="flex-1">
                        {c.name} {c.capital && <Pill tone="blue">Capital</Pill>}
                      </span>
                      <span className="w-28 text-muted-foreground">IBGE {c.ibgeCode ?? "—"}</span>
                      {statusPill(c.active)}
                      {editBtn({
                        kind: "city",
                        id: c.id,
                        values: {
                          name: c.name,
                          ibgeCode: String(c.ibgeCode ?? ""),
                          areaCode: c.areaCode ?? "",
                          active: String(c.active),
                        },
                      })}
                    </li>
                  ))}
                </ul>
              )}
            </>
          )}
        </TabsContent>

        <TabsContent value="neighborhoods" className="space-y-3">
          {!cityId ? (
            <p className="text-sm text-muted-foreground">Escolha estado e cidade.</p>
          ) : (
            <>
              {(canLocations || canUpdate) && (
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => setEditing({ kind: "neighborhood", id: null, values: {} })}
                >
                  <Plus className="mr-1 h-4 w-4" /> Bairro
                </Button>
              )}
              {loadingNb ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (neighborhoods ?? []).length === 0 ? (
                <p className="text-sm text-muted-foreground">Nenhum bairro nesta cidade ainda.</p>
              ) : (
                <ul className="divide-y rounded-lg border text-sm">
                  {(neighborhoods ?? []).map((n) => (
                    <li key={n.id} className="flex items-center gap-3 px-4 py-2">
                      <span className="flex-1">{n.name}</span>
                      <span className="w-28 text-muted-foreground">
                        {n.source === "C" ? "Pelo CEP" : n.source === "I" ? "Importado" : "Manual"}
                      </span>
                      {statusPill(n.active)}
                      {editBtn({
                        kind: "neighborhood",
                        id: n.id,
                        values: { name: n.name, active: String(n.active) },
                      })}
                    </li>
                  ))}
                </ul>
              )}
            </>
          )}
        </TabsContent>
      </Tabs>

      <Dialog open={!!editing} onOpenChange={(o) => !o && setEditing(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{editing?.id ? "Editar" : "Novo"}</DialogTitle>
          </DialogHeader>
          {editing && (
            <div className="grid gap-4 sm:grid-cols-2">
              {FIELDS[editing.kind].map((f) => (
                <Field key={f.key} label={f.label} htmlFor={`loc-${f.key}`}>
                  <Input
                    id={`loc-${f.key}`}
                    type={f.type ?? "text"}
                    value={editing.values[f.key] ?? ""}
                    onChange={(e) =>
                      setEditing({
                        ...editing,
                        values: { ...editing.values, [f.key]: e.target.value },
                      } as Editing)
                    }
                  />
                </Field>
              ))}
              {editing.id !== null && (
                <label className="flex items-center gap-2 text-sm">
                  <input
                    type="checkbox"
                    checked={editing.values.active !== "false"}
                    onChange={(e) =>
                      setEditing({
                        ...editing,
                        values: { ...editing.values, active: String(e.target.checked) },
                      } as Editing)
                    }
                  />
                  Ativo para novas seleções
                </label>
              )}
            </div>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={() => setEditing(null)}>
              Cancelar
            </Button>
            <Button disabled={saving} onClick={save}>
              Salvar
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
