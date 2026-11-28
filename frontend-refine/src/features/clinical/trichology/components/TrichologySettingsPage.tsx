"use client";

import { useEffect, useId, useState } from "react";
import { Loader2, Pencil, Plus } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Textarea } from "@/components/ui/textarea";
import { useAuthorization } from "@/features/identity/auth/hooks/useAuthorization";
import { useSettingsMutations, useTrichologySettings } from "../hooks/useTrichology";
import type {
  CaptureProtocol,
  CaptureRegion,
  FindingCatalogItem,
  Marker,
} from "../types/trichology.types";

const HYPOTHESES: Record<string, string> = {
  ANDROGENETIC_PATTERN: "Padrão androgenético",
  TELOGEN_EFFLUVIUM: "Eflúvio telógeno",
  INFLAMMATORY_PATTERN: "Padrão inflamatório",
  PATCHY_LOSS_PATTERN: "Perda em placas",
  HAIR_SHAFT_DAMAGE: "Dano à haste",
};

/** Configurações da clínica: protocolos, catálogo de achados, marcadores e limites (doc v1.2, seção 9). */
export function TrichologySettingsPage({ companyId }: { companyId: string }) {
  const uid = useId();
  const { can } = useAuthorization();
  const editable = can("trichology:settings");
  const { data, isLoading } = useTrichologySettings(companyId);
  const m = useSettingsMutations(companyId);
  const [minConfidence, setMinConfidence] = useState("0.6");
  const [horizons, setHorizons] = useState("3,6,9,12");
  const [rules, setRules] = useState<Record<string, string>>({});
  const [protocol, setProtocol] = useState<Partial<CaptureProtocol> | null>(null);
  const [regionsText, setRegionsText] = useState("");
  const [finding, setFinding] = useState<Partial<FindingCatalogItem> | null>(null);
  const [marker, setMarker] = useState<Partial<Marker> | null>(null);

  useEffect(() => {
    if (!data) return;
    setMinConfidence(String(data.minConfidence));
    setHorizons(data.reportHorizonsMonths.join(","));
    const r: Record<string, string> = {};
    data.examRules.forEach((rule) => (r[rule.hypothesis] = rule.markerCodes.join(", ")));
    setRules(r);
  }, [data]);

  if (isLoading || !data) return <p className="text-sm text-muted-foreground">Carregando…</p>;

  const saveGeneral = () =>
    m.update.mutate({
      minConfidence: Number(minConfidence),
      reportHorizonsMonths: horizons
        .split(",")
        .map((h) => Number(h.trim()))
        .filter((h) => !Number.isNaN(h) && h > 0),
      examRules: Object.entries(rules)
        .map(([hypothesis, codes]) => ({
          hypothesis,
          markerCodes: codes
            .split(",")
            .map((c) => c.trim().toUpperCase())
            .filter(Boolean),
        }))
        .filter((r) => r.markerCodes.length > 0),
    });

  /** Regiões em texto: uma por linha "CODIGO | Nome | 20x,50x | mínimo". */
  const regionsToText = (regions: CaptureRegion[]) =>
    regions
      .map((r) => `${r.code} | ${r.label} | ${r.magnifications.join(",")} | ${r.minImages}`)
      .join("\n");
  const textToRegions = (text: string): CaptureRegion[] =>
    text
      .split("\n")
      .map((line) => line.split("|").map((p) => p.trim()))
      .filter((parts) => parts[0])
      .map(([code, label, mags, min]) => ({
        code: code.toUpperCase(),
        label: label || code,
        magnifications: (mags ?? "")
          .split(",")
          .map((x) => x.trim())
          .filter(Boolean),
        minImages: Number(min ?? 0) || 0,
      }));

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Limites e regras do copiloto</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <div className="grid gap-3 sm:grid-cols-2">
            <div className="space-y-1">
              <Label htmlFor={`${uid}-conf`}>
                Confiança mínima para mostrar achado da IA (0 a 1)
              </Label>
              <Input
                id={`${uid}-conf`}
                inputMode="decimal"
                value={minConfidence}
                disabled={!editable}
                onChange={(e) => setMinConfidence(e.target.value)}
              />
            </div>
            <div className="space-y-1">
              <Label htmlFor={`${uid}-hz`}>Retornos sugeridos (meses, separados por vírgula)</Label>
              <Input
                id={`${uid}-hz`}
                value={horizons}
                disabled={!editable}
                onChange={(e) => setHorizons(e.target.value)}
              />
            </div>
          </div>
          <p className="text-sm font-medium">
            Exames sugeridos por hipótese (códigos dos marcadores)
          </p>
          {Object.entries(HYPOTHESES).map(([code, label]) => (
            <div key={code} className="grid items-center gap-2 sm:grid-cols-[14rem_1fr]">
              <Label htmlFor={`${uid}-${code}`} className="font-normal">
                {label}
              </Label>
              <Input
                id={`${uid}-${code}`}
                value={rules[code] ?? ""}
                disabled={!editable}
                placeholder="FERRITIN, TSH"
                onChange={(e) => setRules((cur) => ({ ...cur, [code]: e.target.value }))}
              />
            </div>
          ))}
          {editable && (
            <Button type="button" onClick={saveGeneral} disabled={m.update.isPending}>
              {m.update.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />} Salvar
            </Button>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle className="text-base">Protocolos de captura</CardTitle>
          {editable && (
            <Button
              type="button"
              size="sm"
              variant="outline"
              onClick={() => {
                setProtocol({ name: "", isDefault: false, active: true, regions: [] });
                setRegionsText("FRONTAL | Frontal | 20x,50x | 2");
              }}
            >
              <Plus className="mr-2 h-4 w-4" /> Novo
            </Button>
          )}
        </CardHeader>
        <CardContent className="space-y-2">
          {data.protocols.map((p) => (
            <div
              key={p.id}
              className="flex flex-wrap items-center gap-2 rounded-md border p-2 text-sm"
            >
              <span className="font-medium">{p.name}</span>
              {p.isDefault && <Badge>Padrão</Badge>}
              {!p.active && <Badge variant="outline">Inativo</Badge>}
              <span className="text-muted-foreground">
                {p.regions.map((r) => r.label).join(", ")}
              </span>
              {editable && (
                <Button
                  type="button"
                  size="icon"
                  variant="ghost"
                  className="ml-auto"
                  aria-label={`Editar ${p.name}`}
                  onClick={() => {
                    setProtocol(p);
                    setRegionsText(regionsToText(p.regions));
                  }}
                >
                  <Pencil className="h-4 w-4" />
                </Button>
              )}
            </div>
          ))}
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle className="text-base">Catálogo de achados</CardTitle>
          {editable && (
            <Button
              type="button"
              size="sm"
              variant="outline"
              onClick={() => setFinding({ code: "", label: "", description: null, active: true })}
            >
              <Plus className="mr-2 h-4 w-4" /> Novo
            </Button>
          )}
        </CardHeader>
        <CardContent>
          <p className="mb-2 text-xs text-muted-foreground">
            A IA só pode usar os códigos ativos desta lista.
          </p>
          <div className="flex flex-wrap gap-2">
            {data.findings.map((f) => (
              <Button
                key={f.id}
                type="button"
                size="sm"
                variant={f.active ? "secondary" : "outline"}
                disabled={!editable}
                onClick={() => setFinding(f)}
                title={f.code}
              >
                {f.label}
              </Button>
            ))}
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle className="text-base">Marcadores laboratoriais (RPS)</CardTitle>
          {editable && (
            <Button
              type="button"
              size="sm"
              variant="outline"
              onClick={() =>
                setMarker({
                  code: "",
                  label: "",
                  unit: "",
                  system: "",
                  sex: null,
                  active: true,
                  targetQuartile: 3,
                })
              }
            >
              <Plus className="mr-2 h-4 w-4" /> Novo
            </Button>
          )}
        </CardHeader>
        <CardContent className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b text-left text-muted-foreground">
                <th className="py-1 pr-2">Marcador</th>
                <th className="py-1 pr-2">Sistema</th>
                <th className="py-1 pr-2">Sexo</th>
                <th className="py-1 pr-2">Referência</th>
                <th className="py-1 pr-2">Meta</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {data.markers.map((mk) => (
                <tr key={mk.id} className={`border-b ${mk.active ? "" : "opacity-60"}`}>
                  <td className="py-1 pr-2">
                    {mk.label} <span className="text-xs text-muted-foreground">{mk.code}</span>
                  </td>
                  <td className="py-1 pr-2">{mk.system}</td>
                  <td className="py-1 pr-2">{mk.sex ?? "Ambos"}</td>
                  <td className="py-1 pr-2">
                    {mk.referenceMin ?? "—"} a {mk.referenceMax ?? "—"} {mk.unit}
                  </td>
                  <td className="py-1 pr-2">{mk.targetQuartile ? `Q${mk.targetQuartile}` : "—"}</td>
                  <td className="py-1 text-right">
                    {editable && (
                      <Button
                        type="button"
                        size="icon"
                        variant="ghost"
                        aria-label={`Editar ${mk.label}`}
                        onClick={() => setMarker(mk)}
                      >
                        <Pencil className="h-4 w-4" />
                      </Button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <p className="mt-2 text-xs text-muted-foreground">
            Faixas iniciais são ponto de partida; ajuste conforme o laboratório e o critério da
            clínica.
          </p>
        </CardContent>
      </Card>

      <Dialog open={protocol !== null} onOpenChange={(o) => !o && setProtocol(null)}>
        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>{protocol?.id ? "Editar protocolo" : "Novo protocolo"}</DialogTitle>
          </DialogHeader>
          {protocol && (
            <div className="space-y-3">
              <div className="space-y-1">
                <Label htmlFor={`${uid}-pname`}>Nome</Label>
                <Input
                  id={`${uid}-pname`}
                  value={protocol.name ?? ""}
                  onChange={(e) => setProtocol({ ...protocol, name: e.target.value })}
                />
              </div>
              <div className="space-y-1">
                <Label htmlFor={`${uid}-regions`}>
                  Regiões (uma por linha: CÓDIGO | Nome | magnificações | mínimo)
                </Label>
                <Textarea
                  id={`${uid}-regions`}
                  rows={8}
                  className="font-mono text-xs"
                  value={regionsText}
                  onChange={(e) => setRegionsText(e.target.value)}
                />
              </div>
              <div className="flex flex-wrap gap-6">
                <div className="flex items-center gap-2">
                  <Switch
                    id={`${uid}-pdef`}
                    checked={Boolean(protocol.isDefault)}
                    onCheckedChange={(v) => setProtocol({ ...protocol, isDefault: v })}
                  />
                  <Label htmlFor={`${uid}-pdef`}>Padrão</Label>
                </div>
                <div className="flex items-center gap-2">
                  <Switch
                    id={`${uid}-pact`}
                    checked={protocol.active !== false}
                    onCheckedChange={(v) => setProtocol({ ...protocol, active: v })}
                  />
                  <Label htmlFor={`${uid}-pact`}>Ativo</Label>
                </div>
              </div>
            </div>
          )}
          <DialogFooter>
            <Button
              type="button"
              disabled={m.protocol.isPending}
              onClick={() =>
                protocol &&
                m.protocol.mutate(
                  {
                    id: protocol.id ?? null,
                    name: protocol.name ?? "",
                    regions: textToRegions(regionsText),
                    isDefault: Boolean(protocol.isDefault),
                    active: protocol.active !== false,
                  },
                  { onSuccess: () => setProtocol(null) },
                )
              }
            >
              Salvar
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={finding !== null} onOpenChange={(o) => !o && setFinding(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{finding?.id ? "Editar achado" : "Novo achado"}</DialogTitle>
          </DialogHeader>
          {finding && (
            <div className="space-y-3">
              <div className="space-y-1">
                <Label htmlFor={`${uid}-fcode`}>Código</Label>
                <Input
                  id={`${uid}-fcode`}
                  value={finding.code ?? ""}
                  disabled={Boolean(finding.id)}
                  placeholder="EX: PERIFOLLICULAR_SCALING"
                  onChange={(e) => setFinding({ ...finding, code: e.target.value.toUpperCase() })}
                />
              </div>
              <div className="space-y-1">
                <Label htmlFor={`${uid}-flabel`}>Nome</Label>
                <Input
                  id={`${uid}-flabel`}
                  value={finding.label ?? ""}
                  onChange={(e) => setFinding({ ...finding, label: e.target.value })}
                />
              </div>
              <div className="space-y-1">
                <Label htmlFor={`${uid}-fdesc`}>Descrição para a IA (opcional)</Label>
                <Textarea
                  id={`${uid}-fdesc`}
                  value={finding.description ?? ""}
                  onChange={(e) => setFinding({ ...finding, description: e.target.value || null })}
                />
              </div>
              <div className="flex items-center gap-2">
                <Switch
                  id={`${uid}-fact`}
                  checked={finding.active !== false}
                  onCheckedChange={(v) => setFinding({ ...finding, active: v })}
                />
                <Label htmlFor={`${uid}-fact`}>Ativo</Label>
              </div>
            </div>
          )}
          <DialogFooter>
            <Button
              type="button"
              disabled={m.finding.isPending}
              onClick={() =>
                finding &&
                m.finding.mutate(
                  {
                    id: finding.id ?? null,
                    code: finding.code ?? "",
                    label: finding.label ?? "",
                    description: finding.description ?? null,
                    active: finding.active !== false,
                  },
                  { onSuccess: () => setFinding(null) },
                )
              }
            >
              Salvar
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={marker !== null} onOpenChange={(o) => !o && setMarker(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{marker?.id ? "Editar marcador" : "Novo marcador"}</DialogTitle>
          </DialogHeader>
          {marker && (
            <div className="grid gap-3 sm:grid-cols-2">
              {(
                [
                  ["code", "Código"],
                  ["label", "Nome"],
                  ["unit", "Unidade"],
                  ["system", "Sistema (ex.: FERRO)"],
                  ["referenceMin", "Referência mínima"],
                  ["referenceMax", "Referência máxima"],
                  ["targetQuartile", "Quartil-alvo (1 a 4)"],
                ] as const
              ).map(([key, label]) => (
                <div key={key} className="space-y-1">
                  <Label htmlFor={`${uid}-m-${key}`}>{label}</Label>
                  <Input
                    id={`${uid}-m-${key}`}
                    value={
                      marker[key] === null || marker[key] === undefined ? "" : String(marker[key])
                    }
                    disabled={key === "code" && Boolean(marker.id)}
                    onChange={(e) => setMarker({ ...marker, [key]: e.target.value })}
                  />
                </div>
              ))}
              <div className="space-y-1">
                <Label htmlFor={`${uid}-m-sex`}>Sexo (F, M ou vazio)</Label>
                <Input
                  id={`${uid}-m-sex`}
                  value={marker.sex ?? ""}
                  maxLength={1}
                  onChange={(e) =>
                    setMarker({
                      ...marker,
                      sex: (e.target.value.toUpperCase() || null) as Marker["sex"],
                    })
                  }
                />
              </div>
              <div className="flex items-center gap-2">
                <Switch
                  id={`${uid}-m-act`}
                  checked={marker.active !== false}
                  onCheckedChange={(v) => setMarker({ ...marker, active: v })}
                />
                <Label htmlFor={`${uid}-m-act`}>Ativo</Label>
              </div>
            </div>
          )}
          <DialogFooter>
            <Button
              type="button"
              disabled={m.marker.isPending}
              onClick={() => {
                if (!marker) return;
                const num = (v: unknown) =>
                  v === null || v === undefined || String(v).trim() === "" ? null : Number(v);
                m.marker.mutate(
                  {
                    id: marker.id ?? null,
                    code: String(marker.code ?? "").toUpperCase(),
                    label: marker.label ?? "",
                    unit: marker.unit || null,
                    system: String(marker.system ?? "").toUpperCase(),
                    sex: marker.sex ?? null,
                    referenceMin: num(marker.referenceMin),
                    referenceMax: num(marker.referenceMax),
                    targetQuartile: num(marker.targetQuartile),
                    active: marker.active !== false,
                  },
                  { onSuccess: () => setMarker(null) },
                );
              }}
            >
              Salvar
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
